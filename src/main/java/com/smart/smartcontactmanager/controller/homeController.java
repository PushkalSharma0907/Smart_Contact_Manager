package com.smart.smartcontactmanager.controller;

import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.user;
import com.smart.smartcontactmanager.helper.EmailLinkVerification;
import com.smart.smartcontactmanager.service.EmailService;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
public class homeController {
	
	@Autowired
	private PasswordEncoder BPE;
	
	@Autowired
	private userRepo userRepo;
	
	@Autowired
	private EmailService emailService;
	
	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}
	
	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title", "About - Smart Contact Manager");
		return "about";
	}
	
	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title", "Signup - Smart Contact Manager");
		model.addAttribute("user", new com.smart.smartcontactmanager.entities.user());
		return "signup";
	}
	
	@PostMapping("/do_register")
	public String registerUser (@javax.validation.Valid @ModelAttribute("user") com.smart.smartcontactmanager.entities.user user , BindingResult result ,@RequestParam(value="agreement",defaultValue="false") boolean agreement ,  Model model ) {
		
		
			if (!agreement) {
				System.out.println("You have not agreed the terms and conditions");
				 result.rejectValue("enabled", "error.user", "You must accept terms and conditions");
			}
			
			if (result.hasErrors()) {
			    result.getAllErrors().forEach(err -> 
			        System.out.println("Validation error: " + err.getDefaultMessage())
			    );
			    return "signup";
			}

			user.setPassword(BPE.encode(user.getPassword()));
			user.setRole("ROLE_USER");
			user.setEnabled(false); // default false until email verification
			user.setImageUrl("default.png");
			String emailToken = UUID.randomUUID().toString();
	        user.setEmailToken(emailToken);
	        user.setEmailtokenExpiry(LocalDateTime.now().plusHours(24)); // 24 ghante valid

			user savedUser = userRepo.save(user);
			
			String emailLink = EmailLinkVerification.generateVerificationLink(emailToken);
			emailService.sendSimpleEmail(savedUser.getEmail(), "Verify Account : Smart  Contact Manager", emailLink);
			System.out.println("User data: " + user);

			model.addAttribute("user", new com.smart.smartcontactmanager.entities.user());
			model.addAttribute("message", "Successfully Registered!!");
			return "signup";

	}
	
	@RequestMapping("/signin")
	public String customLogin(Model model , HttpSession session) {
		model.addAttribute("title", "Login - Smart Contact Manager");
	    
	
		return "login";
	}

}
