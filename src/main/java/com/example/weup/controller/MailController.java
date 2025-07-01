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

    //TODO. try-catch 문은 service에서 터지는 에러를 잡는거니까 service에 있는게 좋지 않을까...
    @PostMapping("/email")
    public ResponseEntity<DataResponseDTO<HashMap<String, Object>>> sendMail(@RequestBody MailRequestDTO mailRequestDTO) {
        try {
            String mail = mailRequestDTO.getEmail();

            mailService.validateEmailNotRegistered(mail);
            
            mailService.sendMail(mail);

            return ResponseEntity.ok(DataResponseDTO.of("이메일 전송이 완료되었습니다."));
        } catch (Exception e) {
            throw new GeneralException(ErrorInfo.EMAIL_SEND_FAILED);
        }
    }

    @PostMapping("/email/check")
    public ResponseEntity<DataResponseDTO<String>> mailCheck(@RequestBody MailCheckRequestDTO mailCheckRequestDTO) {

        String mail = mailCheckRequestDTO.getEmail();
        int checkCode;

        //TODO. 이런 예외처리도... service가 좋지 않을까...
        try {
            checkCode = Integer.parseInt(mailCheckRequestDTO.getCheckCode());
        } catch (NumberFormatException e) {
            throw new GeneralException(ErrorInfo.BAD_REQUEST);  //TODO. bad request ?
        }

        boolean isMatch = mailService.checkVerificationNumber(mail, checkCode);

        //TODO. 얘는 무조건 service. 이런 평가는 service 에서 진행하기
        if (isMatch) {
            return ResponseEntity.ok(DataResponseDTO.of("이메일 인증이 완료되었습니다. 회원가입을 진행해주세요."));
        } else {
            throw new GeneralException(ErrorInfo.EMAIL_VERIFICATION_FAILED);
        }
    }
}