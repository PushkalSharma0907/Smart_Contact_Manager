package com.smart.smartcontactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

import com.smart.smartcontactmanager.dao.ContactRepo;
import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.smart.smartcontactmanager.entities.user;
import com.smart.smartcontactmanager.helper.message;

@Controller
@RequestMapping("/user")
public class userController {
	
	@Autowired
	private userRepo userRepo;
	
	@Autowired
	private ContactRepo contactRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	// method to add common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);

		// get the user using username(Email)
		user user = userRepo.getUserByUserName(userName);
		System.out.println("USER " + user);

		model.addAttribute("user", user);
	}
	
	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model , Principal principal) {
		model.addAttribute("title", "User Dashboard");
		
		user user = this.userRepo.getUserByUserName(principal.getName());
		List<contact> contacts = this.contactRepo.findContactsByUserId(user.getId(), null).getContent();
		
		model.addAttribute("noOfContact", contacts.size());
		
		
		return "normal/user_dashboard";
	}
	
	// open add form handler
	@GetMapping("/add-contact")	
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new contact());
		return "normal/add_contact_form";
	}
	
	// process add contact form
	@PostMapping("/process-contact")
	public String processAddContactForm(@ModelAttribute contact contact,
	                                    @RequestParam("profileImage") MultipartFile multi,
	                                    Principal principal,
	                                    Model model) {
	    try {
	        String name = principal.getName();
	        user user = this.userRepo.getUserByUserName(name);

	        // processing and uploading file
	        if (multi.isEmpty()) {
	            System.out.println("File is empty");
	            contact.setImage("default.png");
	           
	        } else {
	        	
	        	String fname = multi.getOriginalFilename();
	            contact.setImage(fname);

	            // file upload code
	            File file = new ClassPathResource("static/images").getFile();
	            Path path = Paths.get(file.getAbsolutePath() + File.separator + multi.getOriginalFilename());
	            Files.copy(multi.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	            System.out.println("Image uploaded successfully");
	        }

	        // ✅ ye hamesha chalega, chahe file empty ho ya na ho
	        contact.setUser(user);
	        user.getContacts().add(contact);

	        this.userRepo.save(user);

	        System.out.println("Added to database");
	        model.addAttribute("contact", new contact());
	        model.addAttribute("msg1", "Your contact is added !! Add more..");

	    } catch (Exception e) {
	        System.out.println("ERROR " + e.getMessage());
	        model.addAttribute("contact", contact);
	        model.addAttribute("msg2", "Something went wrong !! Try again..");
	    }

	    return "normal/add_contact_form";
	}
	
	@GetMapping("/show-contacts/{pageNo}")
	public String showContacts(@PathVariable("pageNo") Integer pageNo , Model model, Principal principal) {
		model.addAttribute("title", "Show User Contacts");
		String userName = principal.getName();
		user user = this.userRepo.getUserByUserName(userName);
		
		
		//current page - pageNo
		//contact per page - 3
		Pageable Pageable = PageRequest.of(pageNo, 3);
		
		Page<contact> contacts = contactRepo.findContactsByUserId(user.getId(), Pageable );
		
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", pageNo);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}
	
	@GetMapping("/{cid}/contact")
	public String showContactDetail(@PathVariable("cid") Integer cid,@RequestParam("page") int page , Model model, Principal principal) {
		
		System.out.println("CID " + cid);
		
		
		contact contact = this.contactRepo.findById(cid).get();
		
		

		String userName = principal.getName();
		user user = this.userRepo.getUserByUserName(userName);
		
		if (contact == null) {
			model.addAttribute("title", "Contact Not Found");
			model.addAttribute("message","Contact not found");
		}
		
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
			model.addAttribute("currentPage", page);
		}
		
		return "normal/contact_detail";
	
	}
	
	@GetMapping("/{cid}/contact-show")
	public String showContactDetail(@PathVariable("cid") Integer cid, Model model, Principal principal) {
		
		System.out.println("CID " + cid);
		
		
		contact contact = this.contactRepo.findById(cid).get();
		
		
		
		String userName = principal.getName();
		user user = this.userRepo.getUserByUserName(userName);
		
		if (contact == null) {
			model.addAttribute("title", "Contact Not Found");
			model.addAttribute("message","Contact not found");
		}
		
		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
			model.addAttribute("currentPage", 0);
		}
		
		return "normal/contact_detail";
		
	}
	
	@GetMapping("/{cid}/delete")
	public String deleteContact(@PathVariable("cid") Integer cid, Model model, Principal principal,
			@RequestParam("page") int page) {

		contact contact = this.contactRepo.findById(cid).get();

		String userName = principal.getName();
		user user = this.userRepo.getUserByUserName(userName);

		// check whether the contact belongs to the user or not
		if (user.getId() == contact.getUser().getId()) {
			
			try {
				
			if (!contact.getImage().equals("default.png")) {	
			File deleteFile = new ClassPathResource("static/images").getFile();
			File file1 = new File(deleteFile, contact.getImage());
			file1.delete();}
		} catch (Exception e) {
			System.out.println("No image found");
		}
			
			contact.setUser(null);
			this.contactRepo.delete(contact);
			System.out.println("Contact deleted successfully");
		}

		return "redirect:/user/show-contacts/" + page;
	}
	
	@GetMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model model) {
		model.addAttribute("title", "Update Contact");
		contact contact = this.contactRepo.findById(cid).get();
		contact.setCid(cid);
		model.addAttribute("contact", contact); 
		
		return "normal/update_form";
	}
	
	@PostMapping("/process-update")
	public String updateHandler(@ModelAttribute contact contact, @RequestParam("profileImage") MultipartFile multi,
			Model model, Principal principal) {
		try {
			// old contact details
			contact oldContact = this.contactRepo.findById(contact.getCid()).get();

			// image..
			if (!multi.isEmpty()) {
				if(!oldContact.getImage().equals("default.png")) {
				// delete old photo
				File deleteFile = new ClassPathResource("static/images").getFile();
				File file1 = new File(deleteFile, oldContact.getImage());
				file1.delete();
				}
				// update new photo
				File file = new ClassPathResource("static/images").getFile();
				Path path = Paths.get(file.getAbsolutePath() + File.separator + multi.getOriginalFilename());
				Files.copy(multi.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(multi.getOriginalFilename());
			} else {
				contact.setImage(oldContact.getImage());
			}

			String userName = principal.getName();
			user user = this.userRepo.getUserByUserName(userName);
			contact.setUser(user);

			this.contactRepo.save(contact);
			System.out.println("Contact updated successfully");
			model.addAttribute("msg1", "Your contact is updated !!");

		} catch (Exception e) {
			System.out.println("ERROR " + e.getMessage());
			model.addAttribute("msg2", "Something went wrong !! Try again..");
		}

		return "normal/update_form";
	}
	
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
	
	@GetMapping("/update-profile")
	public String updateProfile(Model model , Principal principal) {
		model.addAttribute("title", "Update Profile");
		
		user user = this.userRepo.getUserByUserName(principal.getName());
		model.addAttribute("user", user);
		
		return "normal/update_profile";
	}
	
	@PostMapping("/process-update-profile")
	public String updateProfileHandler(@ModelAttribute user user,@RequestParam("profileImage") MultipartFile multi, Model model, Principal principal) {
		try {
			
		
			user oldUser = this.userRepo.getUserById(user.getId());
			
			if(!multi.isEmpty()) {
				if(!oldUser.getImageUrl().equals("default.png")) {
					File file = new ClassPathResource("/static/images").getFile();
					File delFile = new File(file , oldUser.getImageUrl());
					delFile.delete();
				}
				// update new photo
				File file = new ClassPathResource("static/images").getFile();
				String fname = oldUser.getId()+"_" +multi.getOriginalFilename();
				File f = new File(file , fname);
				  multi.transferTo(f);

				
				oldUser.setImageUrl(fname);
				
			}
			else {
				oldUser.setImageUrl(oldUser.getImageUrl());
			}
			
			user username = this.userRepo.getUserByUserName(principal.getName());
			oldUser.setName(user.getName());
			oldUser.setAbout(user.getAbout());
			this.userRepo.save(oldUser);

			model.addAttribute("msg1", "Your profile is updated");

		} catch (Exception e) {
			System.out.println("ERROR " + e.getMessage());
			model.addAttribute("msg2", "Something went wrong !! Try again..");
		}

		return "normal/update_profile";
	}
	
	@GetMapping("/settings")
	public String openSettings(Model model) {
		model.addAttribute("title", "Settings");
		return "normal/settings";
	}
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Model model, Principal principal) {

		System.out.println("OLD PASSWORD " + oldPassword);
		System.out.println("NEW PASSWORD " + newPassword);

		String userName = principal.getName();
		user user = this.userRepo.getUserByUserName(userName);

		if (this.passwordEncoder.matches(oldPassword, user.getPassword())) {
			// change the password
			user.setPassword(this.passwordEncoder.encode(newPassword));
			this.userRepo.save(user);
			model.addAttribute("msg1", "Your password is changed successfully");
		} else {
			// error
			model.addAttribute("msg2", "Please enter correct old password");
			return "normal/settings";
		}

		return "redirect:/user/index";
	}
}
