package com.example.weup.controller;

import com.example.weup.dto.request.FileDownloadRequestDTO;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.dto.response.FileFullResponseDTO;
import com.example.weup.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/s3")
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<DataResponseDTO<List<FileFullResponseDTO>>> uploadFiles(@RequestParam List<MultipartFile> files) {
        List<FileFullResponseDTO> uploadedFiles = s3Service.uploadFiles(files);
        return ResponseEntity.ok(DataResponseDTO.of(uploadedFiles, "파일 업로드 성공"));
    }

    @PostMapping("/download")
    public ResponseEntity<DataResponseDTO<String>> getPresignedUrl(@RequestBody FileDownloadRequestDTO fileDownloadRequestDTO) {
        String url = s3Service.getPresignedUrl(fileDownloadRequestDTO.getFileName());
        return ResponseEntity.ok(DataResponseDTO.of(url, "Presigned URL 발급 완료"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<DataResponseDTO<String>> delete(@RequestBody FileDownloadRequestDTO fileDownloadRequestDTO) {
        s3Service.deleteFile(fileDownloadRequestDTO.getFileName());
        return ResponseEntity.ok(DataResponseDTO.of("파일 삭제 완료"));
    }
}
