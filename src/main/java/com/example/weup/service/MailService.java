package com.example.weup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private static final String senderEmail= "badul312836@gmail.com";

    // 이메일별 인증 번호
    private final Map<String, Integer> emailVerificationMap = new ConcurrentHashMap<>();
    
    // 이메일별 인증 완료 상태
    private final Map<String, Boolean> emailVerifiedMap = new ConcurrentHashMap<>();

    public void sendMail(String mail) {
        int number = createNumber();

        emailVerificationMap.put(mail, number);
        emailVerifiedMap.put(mail, false);

        sendEmailAsync(mail, number);
    }
    
    /**
     * 실제 이메일 발송을 비동기적으로 처리?? 몰라 이거 안됨 찾아봐야함
     */
    @Async
    public CompletableFuture<Void> sendEmailAsync(String mail, int number) {
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, mail);
            message.setSubject("we:up 이메일 인증 요청");

            String body = "";
            body += "<h3>" + "we:up에 오신 걸 환영합니다!" + "</h3>";
            body += "<p>" + "we:up 이메일 인증을 위한 인증번호가 발급되었습니다." + "</p>";
            body += "<p>" + "아래의 인증번호를 통하여 이메일 인증을 진행해주세요." + "</p>";
            body += "<hr>";
            body += "<h1>" + number + "</h1>";

            message.setText(body,"UTF-8", "html");

            javaMailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 해당 메일의 인증 번호 반환
     */
    public int getVerificationNumber(String mail) {
        return emailVerificationMap.getOrDefault(mail, -1);
    }

    /**
     * 인증 번호 생성
     */
    private int createNumber() {
        return (int)(Math.random() * (90000)) + 100000;
    }

    /**
     * 인증 번호 일치 여부 확인
     * 일치할 경우 해당 이메일을 인증 완료 상태로 표시
     */
    public boolean checkVerificationNumber(String mail, int userNumber) {
        System.out.println("검증 요청: 이메일=" + mail + ", 인증번호=" + userNumber);
        int storedNumber = getVerificationNumber(mail);
        System.out.println("저장된 번호: " + storedNumber);
        
        boolean isMatch = storedNumber == userNumber;

        if (isMatch) {
            emailVerifiedMap.put(mail, true);
        }
        
        return isMatch;
    }
    
    /**
     * 이메일이 인증 완료 상태인지 확인
     */
    public boolean isEmailVerified(String mail) {
        return emailVerifiedMap.getOrDefault(mail, false);
    }
}