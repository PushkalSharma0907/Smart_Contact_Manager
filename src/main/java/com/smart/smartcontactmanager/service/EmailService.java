package com.smart.smartcontactmanager.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
	
	@Autowired
	private JavaMailSender mailSender;
	
	public boolean sendSimpleEmail(String toEmail, String subject, String body) {
        MimeMessage message =  mailSender.createMimeMessage();
        
        MimeMessageHelper helper;
		try {
			helper = new MimeMessageHelper(message , true);
			
			helper.setFrom("theironfist0907@gmail.com");
        	helper.setTo(toEmail);
        	helper.setSubject(subject);
        	helper.setText(body, true);
        	
        	mailSender.send(message);
        	
        	
		} catch (MessagingException e) {
			
			e.printStackTrace();
		}
		
		        return true;
	}

}
