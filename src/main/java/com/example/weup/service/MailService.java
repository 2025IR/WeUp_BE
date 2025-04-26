package com.example.weup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    private final TemplateEngine templateEngine;

    public void sendMail(String mail) {
        int number = createNumber();

        emailVerificationMap.put(mail, number);
        emailVerifiedMap.put(mail, false);

        sendEmailAsync(mail, number);
    }
    
    /**
     * 실제 이메일 발송을 비동기적으로 처리?? 얘는 안 됨
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

    //얘는 비동기처리 됨
    @Async
    public CompletableFuture<Void> sendProjectInviteEmail(String recipientEmail, String recipientName, 
                                                         String inviterName, Long projectId, String projectName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("[we:up] | " + projectName + " 프로젝트에 초대되었습니다!");
            
            // Thymeleaf 컨텍스트 생성 및 변수 설정
            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("inviterName", inviterName);
            context.setVariable("projectName", projectName);
            
            //todo. 연결하고 나서 초대 URL 수정필요할듯 (localhost)
            String inviteUrl = "http://localhost:8080/projects/join/" + projectId;
            context.setVariable("inviteUrl", inviteUrl);
            
            // 템플릿 처리 및 이메일 내용 설정
            String emailContent = templateEngine.process("InviteEmail", context);
            helper.setText(emailContent, true);
            
            // 이메일 전송
            javaMailSender.send(message);
            
            System.out.println("초대 이메일 전송 완료: " + recipientEmail);
        } catch (MessagingException e) {
            System.err.println("초대 이메일 전송 실패: " + e.getMessage());
            e.printStackTrace();
        }
        
        return CompletableFuture.completedFuture(null);
    }
}