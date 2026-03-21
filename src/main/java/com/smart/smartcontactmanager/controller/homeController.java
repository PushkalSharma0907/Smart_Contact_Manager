package com.smart.smartcontactmanager.controller;

import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.user;
import com.smart.smartcontactmanager.helper.EmailLinkVerification;
import com.smart.smartcontactmanager.service.EmailService;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
	
	@Autowired
	@Qualifier("cpu")
	private ExecutorService cpuExecutor; // for CPU-bound tasks
	
	@Autowired
	@Qualifier("io")
	private ExecutorService ioExecutor;  // for I/O-bound tasks
	
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
	public String registerUser(@Valid @ModelAttribute("user") user user,
	                           BindingResult result,
	                           @RequestParam(value="agreement", defaultValue="false") boolean agreement,
	                           Model model) {
	    try {
	        if (!agreement) {
	            result.rejectValue("enabled", "error.user", "You must accept terms and conditions");
	        }

	        if (result.hasErrors()) {
	            return "signup";
	        }
	        
	     // ✅ Email already registered check
	        Future<user> existingUserFuture = ioExecutor.submit(() -> userRepo.getUserByEmail(user.getEmail()));
	        user existingUser = existingUserFuture.get();

	        if (existingUser != null) {
	            model.addAttribute("message", "This email is already registered! Please login.");
	            model.addAttribute("user", user); // form data wapas bhejo
	            return "signup";
	        }

	        // ✅ CPU-bound password hashing in cpuExecutor
	        Future<String> encodedFuture = cpuExecutor.submit(() -> BPE.encode(user.getPassword()));
	        String encodedPassword = encodedFuture.get(); // wait for result
	        user.setPassword(encodedPassword);

	        user.setRole("ROLE_USER");
	        user.setEnabled(false);
	        user.setImageUrl("default.png");

	        String emailToken = UUID.randomUUID().toString();
	        user.setEmailToken(emailToken);
	        user.setEmailtokenExpiry(LocalDateTime.now().plusHours(24));

	        // ✅ DB save in ioExecutor
	        Future<user> savedFuture = ioExecutor.submit(() -> userRepo.save(user));
	        user savedUser = savedFuture.get(); // wait for DB result

	        // ✅ Email send async (non-blocking)
	        ioExecutor.submit(() -> {
	            String emailLink = EmailLinkVerification.generateVerificationLink(emailToken);
	            emailService.sendSimpleEmail(savedUser.getEmail(),
	                    "Verify Account : Smart Contact Manager",
	                    emailLink);
	            System.out.println("Verification email sent to: " + savedUser.getEmail()+ "using virtual thread");
	        });

	        model.addAttribute("user", new user());
	        model.addAttribute("message", "Successfully Registered!! Please check your email to verify.");
	        return "signup";

	    } catch (Exception e) {
	        model.addAttribute("message", "Something went wrong during registration.");
	        return "signup";
	    }
	}
	
	@RequestMapping("/signin")
	public String customLogin(Model model , HttpSession session) {
		model.addAttribute("title", "Login - Smart Contact Manager");
	    
	
		return "login";
	}
	
	
	@RequestMapping("/error/403")
	public String accessDenied(Model model) {
	    model.addAttribute("title", "Access Denied");
	    model.addAttribute("errorCode", "403");  // ← yeh zaroori hai
	    return "error";
	}
}
