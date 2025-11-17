package com.dialog.email.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

	
    private final JavaMailSender mailSender;
    
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("비밀번호 재설정 안내");
        message.setText("비밀번호 재설정을 위해 아래 링크를 클릭하세요:\n" + resetUrl + "\n\n유효시간은 1시간입니다.");
        mailSender.send(message);
    }
}