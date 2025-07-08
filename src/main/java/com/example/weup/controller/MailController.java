package com.example.weup.controller;

import com.example.weup.GeneralException;
import com.example.weup.dto.request.MailCheckRequestDTO;
import com.example.weup.dto.request.MailRequestDTO;
import com.example.weup.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.constant.ErrorInfo;

import java.util.HashMap;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @PostMapping("/email")
    public ResponseEntity<DataResponseDTO<HashMap<String, Object>>> sendMail(@RequestBody MailRequestDTO mailRequestDTO) {
        String mail = mailRequestDTO.getEmail();

        mailService.validateEmailNotRegistered(mail);
        mailService.sendMail(mail);

        return ResponseEntity.ok(DataResponseDTO.of("이메일 전송이 완료되었습니다."));
    }

    @PostMapping("/email/check")
    public ResponseEntity<DataResponseDTO<String>> mailCheck(@RequestBody MailCheckRequestDTO dto) {
        mailService.verifyEmailCode(dto);

        return ResponseEntity.ok(DataResponseDTO.of("이메일 인증이 완료되었습니다. 회원가입을 진행해주세요."));
    }

}