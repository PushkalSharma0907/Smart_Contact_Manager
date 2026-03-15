package com.smart.smartcontactmanager.service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
	
	@Autowired
	private JavaMailSender mailSender;
	
	@Value("${spring.mail.properties.domain.name}")
	private String domainName;
	
	public boolean sendSimpleEmail(String toEmail, String subject, String body) {
        MimeMessage message =  mailSender.createMimeMessage();
        
        MimeMessageHelper helper;
		try {
			helper = new MimeMessageHelper(message , true);
			
			helper.setFrom(domainName);
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
