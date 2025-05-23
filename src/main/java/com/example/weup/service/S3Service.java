package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.config.S3Properties;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.FileFullResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final S3Properties s3Properties;

    public String getPresignedUrl(String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(fileName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    public List<FileFullResponseDTO> uploadFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return Collections.emptyList();

        List<FileFullResponseDTO> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                uploadedFiles.add(uploadSingleFile(file));
            } catch (IOException e) {
                throw new GeneralException(ErrorInfo.FILE_UPLOAD_FAILED);
            }
        }

        return uploadedFiles;
    }

    public FileFullResponseDTO uploadSingleFile(MultipartFile file) throws IOException {
        String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedFileName = UUID.randomUUID() + "-" + originalFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(storedFileName)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return FileFullResponseDTO.builder()
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .build();
    }

    public void deleteFile(String fileName) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(fileName)
                .build());
    }
}
