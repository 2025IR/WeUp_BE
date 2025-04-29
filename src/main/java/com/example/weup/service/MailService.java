package com.example.weup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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

    public int getVerificationNumber(String mail) {
        return emailVerificationMap.getOrDefault(mail, -1);
    }

    private int createNumber() {
        return (int)(Math.random() * (90000)) + 100000;
    }

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

    public boolean isEmailVerified(String mail) {
        return emailVerifiedMap.getOrDefault(mail, false);
    }

    @Async
    public CompletableFuture<Void> sendProjectInviteEmail(
            String recipientEmail,
            String recipientName,
            String inviterName,
            String projectName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("[we:up] | " + projectName + " 프로젝트에 초대되었습니다!");

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("inviterName", inviterName);
            context.setVariable("projectName", projectName);

            String emailContent = templateEngine.process("InviteEmail", context);
            helper.setText(emailContent, true);

            javaMailSender.send(message);

            log.info("초대 이메일 전송 완료: {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("초대 이메일 전송 실패: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }
}