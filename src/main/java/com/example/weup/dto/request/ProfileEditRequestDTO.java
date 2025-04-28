package com.example.weup.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileEditRequestDTO {
    private String name;
    private String profileImage;
    private String phoneNumber;
}

