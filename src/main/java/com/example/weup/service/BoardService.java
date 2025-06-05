package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.BoardCreateRequestDTO;
import com.example.weup.dto.request.BoardListRequestDTO;
import com.example.weup.dto.request.EditBoardRequestDTO;
import com.example.weup.dto.response.BoardDetailResponseDTO;
import com.example.weup.dto.response.FileResponseDTO;
import com.example.weup.dto.response.FileFullResponseDTO;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final MemberService memberService;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final TagRepository tagRepository;
    private final FileRepository fileRepository;
    private final S3Service s3Service;

    @Transactional
    public void createBoard(Long userId, Long projectId, BoardCreateRequestDTO boardCreateRequestDTO) {

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));
        Tag tag = tagRepository.findByTagName(boardCreateRequestDTO.getTag())
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));

        Board board = Board.builder()
                .member(member)
                .project(project)
                .title(boardCreateRequestDTO.getTitle())
                .contents(boardCreateRequestDTO.getContents())
                .tag(tag)
                .build();
        boardRepository.save(board);

        List<FileFullResponseDTO> fileFullResponseDTOS = s3Service.uploadFiles(boardCreateRequestDTO.getFile());

        if (!fileFullResponseDTOS.isEmpty()) {
            List<File> fileEntities = fileFullResponseDTOS.stream()
                    .map(dto -> File.builder()
                            .board(board)
                            .fileName(dto.getOriginalFileName())
                            .storedName(dto.getStoredFileName())
                            .fileSize(dto.getFileSize())
                            .fileType(dto.getFileType())
                            .build())
                    .collect(Collectors.toList());

            fileRepository.saveAll(fileEntities);
        }
    }

    public Page<BoardListResponseDTO> getBoardList(Long userId, Long projectId, BoardListRequestDTO boardListRequestDTO) {

        String tag = boardListRequestDTO.getTag();
        String search = boardListRequestDTO.getSearch();

        Pageable pageable = PageRequest.of(
                boardListRequestDTO.getPage(),
                boardListRequestDTO.getSize(),
                Sort.by(Sort.Direction.DESC, "boardCreateTime")
        );

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, projectId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        Page<Board> boards = boardRepository.findByProjectIdAndFilters(projectId, tag, search, pageable);

        return boards.map(board -> {
            User user = board.getMember().getUser();
            boolean hasFile = fileRepository.existsByBoard(board);

            return BoardListResponseDTO.builder()
                    .boardId(board.getBoardId())
                    .nickname(user.getName())
                    .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
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

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, board.getProject().getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        List<File> files = fileRepository.findAllByBoard(board);

        List<FileResponseDTO> fileDTOs = files.stream()
                .map(file -> FileResponseDTO.builder()
                        .fileName(file.getFileName())
                        .fileSize(file.getFileSize())
                        .downloadUrl(s3Service.getPresignedUrl(file.getStoredName()))
                        .build())
                .collect(Collectors.toList());

        return BoardDetailResponseDTO.builder()
                .name(member.getUser().getName())
                .profileImage(member.getUser().getProfileImage())
                .title(board.getTitle())
                .contents(board.getContents())
                .boardCreatedTime(board.getBoardCreateTime())
                .tag(board.getTag().getTagName())
                .files(fileDTOs)
                .build();
    }

    @Transactional
    public void editBoard(Long userId, EditBoardRequestDTO editBoardRequestDTO) throws IOException {

        Long boardId = editBoardRequestDTO.getBoardId();
        String title = editBoardRequestDTO.getTitle();
        String contents = editBoardRequestDTO.getContents();
        String tagName = editBoardRequestDTO.getTag();
        MultipartFile file = editBoardRequestDTO.getFile();

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, board.getProject().getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        if (!board.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        if (title != null) {
            board.setTitle(title.trim());
        } else if (contents != null) {
            board.setContents(contents.trim());
        } else {
            throw new GeneralException(ErrorInfo.INTERNAL_ERROR);
        }

        Tag tag = tagRepository.findByTagName(tagName)
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));
        board.setTag(tag);

        if (file != null && !file.isEmpty()) {
            List<File> existingFiles = fileRepository.findAllByBoard(board);
            for (File existing : existingFiles) {
                s3Service.deleteFile(existing.getStoredName());
            }
            fileRepository.deleteAll(existingFiles);

            FileFullResponseDTO uploaded = s3Service.uploadSingleFile(file);

            File newFile = File.builder()
                    .board(board)
                    .fileName(uploaded.getOriginalFileName())
                    .storedName(uploaded.getStoredFileName())
                    .fileType(uploaded.getFileType())
                    .fileSize(uploaded.getFileSize())
                    .build();

            fileRepository.save(newFile);
        }
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, board.getProject().getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.FORBIDDEN));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        if (!board.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.FORBIDDEN);
        }

        List<File> files = fileRepository.findAllByBoard(board);
        for (File file : files) {
            s3Service.deleteFile(file.getStoredName());
        }
        fileRepository.deleteAll(files);

        boardRepository.delete(board);
    }
}
