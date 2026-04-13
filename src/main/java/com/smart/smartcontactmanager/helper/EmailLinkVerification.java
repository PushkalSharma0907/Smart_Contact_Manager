package com.smart.smartcontactmanager.helper;



public class EmailLinkVerification {
	
	
	public static String generateVerificationLink(String emailToken) {
		
			
		String baseUrl = System.getenv("APP_URL") != null 
                ? System.getenv("APP_URL") 
                : "http://localhost:8080";
		String verificationLink = baseUrl + "/auth/verify-email?token=" + emailToken;
		return verificationLink;
	}

}
