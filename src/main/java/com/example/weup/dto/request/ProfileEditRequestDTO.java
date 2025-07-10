package com.example.weup.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ProfileEditRequestDTO {
    @NotBlank(message = "이름은 필수로 입력되어야 합니다.")
    private String name;
    private MultipartFile profileImage;
    @NotNull
    private String phoneNumber;
}

