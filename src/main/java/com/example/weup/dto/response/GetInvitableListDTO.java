package com.example.weup.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetInvitableListDTO {

    private Long memberId;

    private String memberName;

    private String profileImage;
}
