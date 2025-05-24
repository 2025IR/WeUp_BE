package com.example.weup.dto.request;

import lombok.Data;

@Data
public class MailCheckRequestDTO {
    private String email;
    private String checkCode;
}
