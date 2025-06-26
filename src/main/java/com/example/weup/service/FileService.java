package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.FileFullResponseDTO;
import com.example.weup.dto.response.FileResponseDTO;
import com.example.weup.entity.Board;
import com.example.weup.entity.File;
import com.example.weup.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private final S3Service s3Service;
    private final FileRepository fileRepository;

    public void saveFilesForBoard(Board board, List<MultipartFile> files) {
        List<FileFullResponseDTO> fileFullResponseDTOS = s3Service.uploadFiles(files);

        if (fileFullResponseDTOS.isEmpty()) return;

        List<File> fileEntities = fileFullResponseDTOS.stream()
                .map(dto -> File.builder()
                        .board(board)
                        .fileName(dto.getOriginalFileName())
                        .storedName(dto.getStoredFileName())
                        .fileSize(dto.getFileSize())
                        .fileType(dto.getFileType())
                        .build())
                .toList();

        fileRepository.saveAll(fileEntities);
    }

    public List<FileResponseDTO> getFileResponses(Board board) {
        return fileRepository.findAllByBoard(board).stream()
                .map(file -> FileResponseDTO.builder()
                        .fileName(file.getFileName())
                        .fileSize(file.getFileSize())
                        .downloadUrl(s3Service.getPresignedUrl(file.getStoredName()))
                        .fileId(file.getFileId())
                        .build())
                .toList();
    }

    public void removeFiles(Board board, List<Long> removeFileIds) {
        if (removeFileIds == null || removeFileIds.isEmpty()) return;

        List<File> removeFiles = fileRepository.findAllById(removeFileIds);

        for (File file : removeFiles) {
            if (!file.getBoard().equals(board)) {
                throw new GeneralException(ErrorInfo.FORBIDDEN);
            }
            s3Service.deleteFile(file.getStoredName());
        }

        fileRepository.deleteAll(removeFiles);
        fileRepository.findAllByBoard(board);
    }

    public void addFiles(Board board, List<MultipartFile> files) {
        List<FileFullResponseDTO> uploadedFiles = s3Service.uploadFiles(files);
        List<File> fileEntities = uploadedFiles.stream()
                .map(dto -> File.builder()
                        .board(board)
                        .fileName(dto.getOriginalFileName())
                        .storedName(dto.getStoredFileName())
                        .fileSize(dto.getFileSize())
                        .fileType(dto.getFileType())
                        .build())
                .toList();

        fileRepository.saveAll(fileEntities);
    }
}
