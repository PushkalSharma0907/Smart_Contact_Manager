package com.smart.smartcontactmanager.controller;

import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.smartcontactmanager.entities.user;
import com.smart.smartcontactmanager.helper.Message;
import com.smart.smartcontactmanager.helper.MessageType;
import com.smart.smartcontactmanager.dao.userRepo;

@Controller
@RequestMapping("/auth")
public class AuthController {
	
	@Autowired
	private userRepo userRepo;
	
	@GetMapping("/verify-email")
	public String verifyEmail(@RequestParam("token") String token, Model model, HttpSession session, 
            HttpServletRequest request) {
		System.out.println("Verifying email with token: " + token);
	    user verifiedUser = userRepo.findByEmailToken(token);
	    if (verifiedUser != null) {
	    	if (verifiedUser.getEmailtokenExpiry().isAfter(LocalDateTime.now())) {


	        verifiedUser.setEnabled(true);		//false by default
	        System.out.println("User enabled: " + verifiedUser.isEnabled());
	        verifiedUser.setEmailToken(null); // token expire kar do
	        verifiedUser.setEmailtokenExpiry(null);

	        userRepo.save(verifiedUser);		// update existing user with enabled true and token null
        
            
            model.addAttribute("message", "Email verified successfully!");	       
            return "login";}
	    	else {
	            model.addAttribute("message", "Verification link expired. Please register again.");
	            return "error";
	        }

	    } else {
	            // user is disabled
	           
	            model.addAttribute("message", "Invalid or expired verification link.");	        
	            System.out.println("Email verification failed: Invalid token");
	        return "error";
	    }
	}

}
