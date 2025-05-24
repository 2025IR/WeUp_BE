package com.example.weup.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ProfileEditRequestDTO {
    private String name;
    private MultipartFile profileImage;
    private String phoneNumber;
}

