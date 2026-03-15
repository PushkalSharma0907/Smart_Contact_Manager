package com.smart.smartcontactmanager.helper;


public class EmailLinkVerification {
	
	
	public static String generateVerificationLink(String emailToken) {
		
		String verificationLink = "http://localhost:8080/auth/verify-email?token=" + emailToken;

		return verificationLink;
	}

}
