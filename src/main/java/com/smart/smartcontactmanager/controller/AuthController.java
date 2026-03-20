package com.smart.smartcontactmanager.controller;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.smartcontactmanager.entities.user;
import com.smart.smartcontactmanager.helper.Message;
import com.smart.smartcontactmanager.helper.MessageType;
import com.smart.smartcontactmanager.service.UserServiceThread;
import com.smart.smartcontactmanager.dao.userRepo;

@Controller
@RequestMapping("/auth")
public class AuthController {
	
	@Autowired
	private userRepo userRepo;
	
	@Autowired
	@Qualifier("io")
	private ExecutorService ioExecutor;  // for I/O-bound tasks
	
	@GetMapping("/verify-email")
	public String verifyEmail(@RequestParam("token") String token, Model model, HttpSession session, 
            HttpServletRequest request) throws InterruptedException, ExecutionException {
		System.out.println("Verifying email with token: " + token);
	    user verifiedUser = userRepo.findByEmailToken(token);
	    if (verifiedUser != null) {
	    	if (verifiedUser.getEmailtokenExpiry().isAfter(LocalDateTime.now())) {


	        verifiedUser.setEnabled(true);		//false by default
	        System.out.println("User enabled: " + verifiedUser.isEnabled());
	        verifiedUser.setEmailToken(null); // token expire kar do
	        verifiedUser.setEmailtokenExpiry(null);
	     // ✅ DB save in virtual thread (I/O heavy)
            ioExecutor.submit(() -> {
                userRepo.save(verifiedUser);
                return null;
            }).get();
        
            
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
