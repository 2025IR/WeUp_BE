package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProfileResponseDTO {
    private String name;
    private String email;
    private String profileImage;
    private String phoneNumber;
} 