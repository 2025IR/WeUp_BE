package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.BoardCreateRequestDTO;
import com.example.weup.dto.request.BoardListRequestDTO;
import com.example.weup.dto.request.EditBoardRequestDTO;
import com.example.weup.dto.response.BoardDetailResponseDTO;
import com.example.weup.dto.response.FileResponseDTO;
import com.example.weup.validate.MemberValidator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.weup.dto.response.BoardListResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final FileService fileService;
    private final ProjectRepository projectRepository;
    private final BoardRepository boardRepository;
    private final TagRepository tagRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;
    private final MemberValidator memberValidator;

    @Transactional
    public void createBoard(Long userId, BoardCreateRequestDTO boardCreateRequestDTO) {

        Member member = memberValidator.validateActiveMemberInProject(userId, boardCreateRequestDTO.getProjectId());

        Project project = projectRepository.findById(boardCreateRequestDTO.getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        Tag tag = tagRepository.findByTagName(boardCreateRequestDTO.getTag())
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));

        Board board = Board.builder()
                .member(member)
                .project(project)
                .title(boardCreateRequestDTO.getTitle().trim())
                .contents(boardCreateRequestDTO.getContents().trim())
                .tag(tag)
                .build();
        boardRepository.save(board);

        if (boardCreateRequestDTO.getFile() != null) {
            fileService.saveFilesForBoard(board, boardCreateRequestDTO.getFile());
        }
    }

    public Page<BoardListResponseDTO> getBoardList(Long userId, Long projectId, BoardListRequestDTO boardListRequestDTO) {

        Pageable pageable = PageRequest.of(
                boardListRequestDTO.getPage(),
                boardListRequestDTO.getSize(),
                Sort.by(Sort.Direction.DESC, "boardCreateTime")
        );

        memberValidator.validateActiveMemberInProject(userId, projectId);

        Page<Board> boards = boardRepository.findByProjectIdAndFilters(projectId, boardListRequestDTO.getTag(), boardListRequestDTO.getSearch(), pageable);

        return boards.map(board -> {
            boolean hasFile = fileRepository.existsByBoard(board);

            return BoardListResponseDTO.builder()
                    .boardId(board.getBoardId())
                    .name(getWriterName(board))
                    .profileImage(getWriterProfileImage(board))
                    .title(board.getTitle())
                    .boardCreatedTime(board.getBoardCreateTime())
                    .tag(board.getTag().getTagName())
                    .hasFile(hasFile)
                    .build();
        });
    }

    public BoardDetailResponseDTO getBoardDetail(Long userId, Long boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        memberValidator.validateActiveMemberInProject(userId, board.getProject().getProjectId());

        List<FileResponseDTO> fileDTOs = fileService.getFileResponses(board);

        return BoardDetailResponseDTO.builder()
                .name(getWriterName(board))
                .profileImage(getWriterProfileImage(board))
                .memberId(board.getMember() != null ? board.getMember().getMemberId() : null)
                .title(board.getTitle())
                .contents(board.getContents())
                .boardCreatedTime(board.getBoardCreateTime())
                .tag(board.getTag().getTagName())
                .files(fileDTOs)
                .build();
    }

    @Transactional
    public void editBoard(Long userId, Long boardId, EditBoardRequestDTO editBoardRequestDTO) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        Member member = memberValidator.validateActiveMemberInProject(userId, board.getProject().getProjectId());
        if (board.getMember() != null) {
            memberValidator.validateBoardWriter(board, member);
        }

        Tag tag = tagRepository.findByTagName(editBoardRequestDTO.getTag())
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));

        board.editBoard(editBoardRequestDTO.getTitle().trim(), editBoardRequestDTO.getContents().trim(), tag);

        if (editBoardRequestDTO.getFile() != null) {
            fileService.addFiles(board, editBoardRequestDTO.getFile());
        }

        if (editBoardRequestDTO.getRemoveFileIds() != null && !editBoardRequestDTO.getRemoveFileIds().isEmpty()) {
            fileService.removeFiles(board, editBoardRequestDTO.getRemoveFileIds());
        }

        boolean hasContents = board.getContents() != null && !board.getContents().trim().isEmpty();
        boolean hasFiles = fileRepository.existsByBoard(board);
        if (!hasContents && !hasFiles) {
            throw new GeneralException(ErrorInfo.BOARD_CONTENT_OR_FILE_REQUIRED);
        }
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        Member member = memberValidator.validateActiveMemberInProject(userId, board.getProject().getProjectId());
        if (board.getMember() != null) {
            memberValidator.validateBoardWriter(board, member);
        }

        List<File> files = fileRepository.findAllByBoard(board);
        for (File file : files) {
            s3Service.deleteFile(file.getStoredName());
        }
        fileRepository.deleteAll(files);

        boardRepository.delete(board);
    }

    private String getWriterName(Board board) {
        if (board.getMember() != null) {
            return board.getMember().getUser().getName();
        }
        return board.getSenderType().getName();
    }

    private String getWriterProfileImage(Board board) {
        if (board.getMember() != null) {
            return s3Service.getPresignedUrl(board.getMember().getUser().getProfileImage());
        }
        return s3Service.getPresignedUrl(board.getSenderType().getProfileImage());
    }

}
