package com.smart.smartcontactmanager.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Resource;
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
	
	 @Resource(name = "io")
	 private ExecutorService ioExecutor;

	
	@Value("${spring.mail.properties.domain.name}")
	private String domainName;
	
	public Future<Boolean> sendSimpleEmail(String toEmail, String subject, String body) {
       return ioExecutor.submit(() -> {
    		   MimeMessage message =  mailSender.createMimeMessage();
        
        MimeMessageHelper helper;
		try {
			helper = new MimeMessageHelper(message , true);
			
			helper.setFrom(domainName);
        	helper.setTo(toEmail);
        	helper.setSubject(subject);
        	helper.setText(body, true);
        	
        	mailSender.send(message);
        	
        	System.out.println("Email sent successfully to " + toEmail+"via virtual thread");
        	
        	
		} catch (MessagingException e) {
			
			e.printStackTrace();
		}
		
		        return true;
	});
	}
}
