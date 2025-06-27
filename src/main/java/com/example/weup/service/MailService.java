package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.repository.AccountSocialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MailService {

    private final AccountSocialRepository accountSocialRepository;
    private final AsyncMailService asyncMailService;

    private final Map<String, Integer> emailVerificationMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> emailVerifiedMap = new ConcurrentHashMap<>();

    public void sendMail(String mail) {
        int number = createNumber();

        emailVerificationMap.put(mail, number);
        emailVerifiedMap.put(mail, false);

        asyncMailService.sendVerificationEmail(mail, number);
    }

    public boolean checkVerificationNumber(String mail, int userNumber) {
        int storedNumber = emailVerificationMap.getOrDefault(mail, -1);
        boolean isMatch = storedNumber == userNumber;

        if (isMatch) {
            emailVerifiedMap.put(mail, true);
        }

        return isMatch;
    }

    public boolean isEmailVerified(String mail) {
        return emailVerifiedMap.getOrDefault(mail, false);
    }

    public void validateEmailNotRegistered(String email) {
        if (accountSocialRepository.existsByEmail(email)) {
            throw new GeneralException(ErrorInfo.EMAIL_ALREADY_EXIST);
        }
    }

    private int createNumber() {
        return (int) (Math.random() * 90000) + 100000;
    }
}
