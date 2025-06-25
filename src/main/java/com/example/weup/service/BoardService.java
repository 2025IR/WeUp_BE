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
    public void createBoard(Long userId, BoardCreateRequestDTO boardCreateRequestDTO) {

//        if (boardCreateRequestDTO.getTitle() == null || boardCreateRequestDTO.getTitle().isEmpty()
//                || boardCreateRequestDTO.getTag() == null || boardCreateRequestDTO.getTag().isEmpty()) {
//            throw new GeneralException(ErrorInfo.EMPTY_INPUT_VALUE);
//        }

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, boardCreateRequestDTO.getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

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
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        Page<Board> boards = boardRepository.findByProjectIdAndFilters(projectId, tag, search, pageable);

        return boards.map(board -> {
            User user = board.getMember().getUser();
            boolean hasFile = fileRepository.existsByBoard(board);

            return BoardListResponseDTO.builder()
                    .boardId(board.getBoardId())
                    .name(user.getName())
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
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        List<File> files = fileRepository.findAllByBoard(board);

        List<FileResponseDTO> fileDTOs = files.stream()
                .map(file -> FileResponseDTO.builder()
                        .fileName(file.getFileName())
                        .fileSize(file.getFileSize())
                        .downloadUrl(s3Service.getPresignedUrl(file.getStoredName()))
                        .fileId(file.getFileId())
                        .build())
                .collect(Collectors.toList());

        return BoardDetailResponseDTO.builder()
                .name(member.getUser().getName())
                .profileImage(s3Service.getPresignedUrl(member.getUser().getProfileImage()))
                .title(board.getTitle())
                .contents(board.getContents())
                .boardCreatedTime(board.getBoardCreateTime())
                .tag(board.getTag().getTagName())
                .files(fileDTOs)
                .build();
    }

    @Transactional
    public void editBoard(Long userId, Long boardId, EditBoardRequestDTO editBoardRequestDTO) throws IOException {

//        if (editBoardRequestDTO.getTitle() == null || editBoardRequestDTO.getTitle().isEmpty()
//                || editBoardRequestDTO.getTag() == null || editBoardRequestDTO.getTag().isEmpty()) {
//            throw new GeneralException(ErrorInfo.EMPTY_INPUT_VALUE);
//        }

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, board.getProject().getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        if (!board.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.NOT_WRITER);
        }

        board.setTitle(editBoardRequestDTO.getTitle().trim());
        board.setContents(editBoardRequestDTO.getContents().trim());

        Tag tag = tagRepository.findByTagName(editBoardRequestDTO.getTag())
                .orElseThrow(() -> new GeneralException(ErrorInfo.TAG_NOT_FOUND));
        board.setTag(tag);

        List<Long> removeFileIds = editBoardRequestDTO.getRemoveFileIds();
        if (removeFileIds != null && !removeFileIds.isEmpty()) {
            List<File> removeFiles = fileRepository.findAllById(removeFileIds);

            for (File file : removeFiles) {
                if (!file.getBoard().getBoardId().equals(board.getBoardId())) {
                    throw new GeneralException(ErrorInfo.FORBIDDEN);
                }
                s3Service.deleteFile(file.getStoredName());
            }

            fileRepository.deleteAll(removeFiles);
            fileRepository.findAllByBoard(board);
        }

        List<MultipartFile> newFiles = editBoardRequestDTO.getFile();

        List<FileFullResponseDTO> uploadedFiles = s3Service.uploadFiles(newFiles);

        List<File> newFileEntities = uploadedFiles.stream()
                .map(dto -> File.builder()
                        .board(board)
                        .fileName(dto.getOriginalFileName())
                        .storedName(dto.getStoredFileName())
                        .fileSize(dto.getFileSize())
                        .fileType(dto.getFileType())
                        .build())
                .collect(Collectors.toList());

        fileRepository.saveAll(newFileEntities);
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {

        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.BOARD_NOT_FOUND));

        Member member = memberRepository.findByUser_UserIdAndProject_ProjectId(userId, board.getProject().getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.NOT_IN_PROJECT));

        if (memberService.isDeletedMember(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.DELETED_MEMBER);
        }

        if (!board.getMember().getMemberId().equals(member.getMemberId())) {
            throw new GeneralException(ErrorInfo.NOT_WRITER);
        }

        List<File> files = fileRepository.findAllByBoard(board);
        for (File file : files) {
            s3Service.deleteFile(file.getStoredName());
        }
        fileRepository.deleteAll(files);

        boardRepository.delete(board);
    }
}
