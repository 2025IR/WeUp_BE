package com.example.weup.dto.response;

import com.example.weup.entity.User;
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
    private String password;
    private String profileImage;

//    public GetProfileResponseDTO(User user) {
//        this.name = user.getName();
//        this.email = user.getEmail();
//        this.password = user.getPassword();
//        this.profileImage = user.getProfileImage();
//    }

} 