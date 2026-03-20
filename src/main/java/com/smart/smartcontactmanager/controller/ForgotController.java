package com.smart.smartcontactmanager.controller;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.smartcontactmanager.service.EmailService;
import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.user;

@Controller
public class ForgotController {
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private userRepo userRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	@Qualifier("cpu")
	private ExecutorService cpuExecutor; // for CPU-bound tasks
	
	@GetMapping("/forgot")
	public String openEmailForm(Model model) {
		model.addAttribute("title", "Forgot Password");
		
		System.out.println("ForgotController: openEmailForm called");
		
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, Model model, HttpSession session) throws InterruptedException, ExecutionException {
		
		System.out.println("Email: " + email);
		
		//generating random 4 digit number
		Random random = new Random();  // no fixed seed
		int otp = random.nextInt(9000) + 1000; // 4 digit OTP (1000–9999)
		System.out.println("OTP: " + otp);
		
		//write code for send otp to email
		String subject = "OTP from Smart Contact Manager";
		String message = "<h1>Your OTP is </h1>" + otp;
		String toEmail = email;
		
		Future<Boolean> result = this.emailService.sendSimpleEmail(toEmail, subject, message);
		
		System.out.println("OTP: " + otp);
		
		if(result.get()) {		// wait for result if needed

            //otp is sent successfully
			session.setAttribute("email", email);
			session.setAttribute("myotp", otp);
			 session.setAttribute("otpTime", System.currentTimeMillis()); // store timestamp
			 
			 model.addAttribute("message2", "OTP sent to your email id !!");

			return "verify_otp";
		}
		else {
			// otp is not sent
			model.addAttribute("message", "Check your email id !!");
			
			return "forgot_email_form";
		}
		
	}
	
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp") int otp, HttpSession session, Model model) {

		int myotp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");
		
		 Long otpTime = (Long) session.getAttribute("otpTime");

		    // check if OTP expired
		    long currentTime = System.currentTimeMillis();
		    if (otpTime == null || (currentTime - otpTime) > 600000) { // 10 minutes
		    	model.addAttribute("message", "OTP has expired. Please request a new one.");
		        return "varify_otp";
		    }


		if (myotp == otp) {
			
		 user user = this.userRepo.getUserByUserName(email);
			if (user == null) {
				// user not found
				model.addAttribute("message", "User does not exist with this email !!");
				return "forgot_email_form";
			}else {
				// otp is correct
				
				model.addAttribute("message2", "OTP verified successfully !! You can change your password now.");
				
				System.out.println("OTP is correct");
				return "password_change_form";
				
			}
			
		} else {
			// otp is incorrect
			System.out.println("OTP is incorrect");
			model.addAttribute("message", "You have entered wrong OTP !!");
			return "verify_otp";
		}

	}
		
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword1") String newpassword1,@RequestParam("newPassword2") String newpassword2 , HttpSession session , Model model) throws InterruptedException, ExecutionException {

		String email = (String) session.getAttribute("email");
		user user = this.userRepo.getUserByUserName(email);
		
		if (!newpassword1.equals(newpassword2)) {
			model.addAttribute("message", "Both passwords do not match !!");
			return "password_change_form";
		}
		
		 // ✅ CPU-bound password hashing in cpuExecutor
        Future<String> encodedFuture = cpuExecutor.submit(() -> this.passwordEncoder.encode(newpassword1));
        String encodedPassword = encodedFuture.get(); // wait for result
        user.setPassword(encodedPassword);
		

		this.userRepo.save(user);

		return "redirect:/signin?change=password changed successfully...";
	}
}
