package com.example.weup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.sender}")
    private String senderEmail;


    @Async
    public CompletableFuture<Void> sendVerificationEmail(String mail, int number) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom(senderEmail);
            message.setRecipients(MimeMessage.RecipientType.TO, mail);
            message.setSubject("we:up 이메일 인증 요청");

            String body = """
                    <h3>we:up에 오신 걸 환영합니다!</h3>
                    <p>we:up 이메일 인증을 위한 인증번호가 발급되었습니다.</p>
                    <p>아래의 인증번호를 통하여 이메일 인증을 진행해주세요.</p>
                    <hr>
                    <h1>%d</h1>
                    """.formatted(number);

            message.setText(body, "UTF-8", "html");
            javaMailSender.send(message);
        } catch (MessagingException e) {
            log.error("이메일 인증 메일 전송 실패: {}", e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
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
