package com.connecttrack.pro.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("nramanjaneyulu2002@gmail.com"); // Or your company's email
        message.setTo(toEmail);
        message.setSubject("Your ConnectTrack Pro Password Has Been Reset");
        message.setText("Hello,\n\n"
                + "Your password has been reset. Please use the following temporary password to log in and set a new one:\n\n"
                + "Temporary Password: " + temporaryPassword + "\n\n"
                + "Thank you,\nThe ConnectTrack Pro Team");
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // In a real app, you would log this error more robustly
            System.err.println("Error sending password reset email: " + e.getMessage());
        }
    }
}