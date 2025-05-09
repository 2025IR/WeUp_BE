package com.example.weup.controller;

import com.example.weup.GeneralException;
import com.example.weup.dto.request.MailCheckRequestDTO;
import com.example.weup.dto.request.MailRequestDTO;
import com.example.weup.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

import com.example.weup.dto.response.DataResponseDTO;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.response.ErrorResponseDTO;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    @PostMapping("/email")
    public ResponseEntity<DataResponseDTO<HashMap<String, Object>>> sendMail(@RequestBody MailRequestDTO mailRequestDTO) {
        try {
            String mail = mailRequestDTO.getMail();
            System.out.println(mail);
            
            mailService.sendMail(mail);
            int verificationNumber = mailService.getVerificationNumber(mail);
            String num = String.valueOf(verificationNumber);
            
            HashMap<String, Object> map = new HashMap<>();
            map.put("number", num);
            
            return ResponseEntity.ok(DataResponseDTO.of(map, "이메일 전송이 완료되었습니다."));
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.EMAIL_SEND_FAILED);
        }
    }

    @PostMapping("/email/check")
    public ResponseEntity<DataResponseDTO<String>> mailCheck(@RequestBody MailCheckRequestDTO mailCheckRequestDTO) {
        String mail = mailCheckRequestDTO.getMail();
        int userNumber = mailCheckRequestDTO.getUserNumber();
        boolean isMatch = mailService.checkVerificationNumber(mail, userNumber);
        
        if (isMatch) {
            return ResponseEntity.ok(DataResponseDTO.of("이메일 인증이 완료되었습니다. 회원가입을 진행해주세요."));
        } else {
            throw new GeneralException(ErrorInfo.EMAIL_VERIFICATION_FAILED);
        }
    }
}
