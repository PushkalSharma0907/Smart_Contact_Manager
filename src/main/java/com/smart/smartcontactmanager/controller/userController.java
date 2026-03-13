package com.smart.smartcontactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.smart.smartcontactmanager.config.CloudinaryConfig;
import com.smart.smartcontactmanager.dao.ContactRepo;
import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.contact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
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


@Controller
@RequestMapping("/user")
public class userController {
	
	@Autowired
	private userRepo userRepo;
	
	@Autowired
	private ContactRepo contactRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private  Cloudinary cloudinary;
	
	private String resolveEmail(Principal principal) {
	    if (principal instanceof Authentication) {
	        Object principalObj = ((Authentication) principal).getPrincipal();
	        
	    	//oauth se login ke liye bcoz principal.getName goolge ki id dega , jo hme nhi chahiye, hme email chahiye , isliye authentication se email nikalna padega

	        if (principalObj instanceof DefaultOAuth2User) {
	            // OAuth2 login case
	            DefaultOAuth2User oauthUser = (DefaultOAuth2User) principalObj;
	            return oauthUser.getAttribute("email");
	        } else if (principalObj instanceof org.springframework.security.core.userdetails.User) {
	            // Direct form login case
	            return ((org.springframework.security.core.userdetails.User) principalObj).getUsername();
	        }
	    }
	    return principal.getName(); // fallback
	}

	
	// method to add common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String email = resolveEmail(principal);   // ✅ works for both direct + OAuth2
	    user user = userRepo.getUserByEmail(email);
	    model.addAttribute("user", user);

	}
	
	

	
	// dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model , Principal principal) {
		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);

		    List<contact> contacts = contactRepo.findContactsByUserId(user.getId(), null).getContent();
		    model.addAttribute("noOfContact", contacts.size());
		    model.addAttribute("title", "User Dashboard");

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
	    	 String email = resolveEmail(principal);
			    user user = userRepo.getUserByEmail(email);

	        // processing and uploading file
	        if (multi.isEmpty()) {
	            System.out.println("File is empty");
	            contact.setImage("default.png");
	           
	        } else {
	        	
	        	String fname = multi.getOriginalFilename();
	            contact.setImage(fname);

	            // file upload code on server
//	            File file = new ClassPathResource("static/images").getFile();
//	            Path path = Paths.get(file.getAbsolutePath() + File.separator + multi.getOriginalFilename());
//	            Files.copy(multi.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
//	            System.out.println("Image uploaded successfully");
	            
	            //image upload code on Cloudinary
	            Map uploadResult = cloudinary.uploader().upload(multi.getBytes(),
	                    ObjectUtils.asMap("folder", "contacts"));

	            String imageUrl = uploadResult.get("secure_url").toString();
	            contact.setImage(imageUrl);
	            contact.setPublicId(uploadResult.get("public_id").toString());

	            System.out.println("Image uploaded successfully to Cloudinary: " + imageUrl);

	            
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
		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);
		
		
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
		
		

		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);
		
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
		
		
		
		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);
		
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

		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);

		// check whether the contact belongs to the user or not
		if (user.getId() == contact.getUser().getId()) {
			
			try {
				
			if (!contact.getImage().equals("default.png")) {	
//			File deleteFile = new ClassPathResource("static/images").getFile();
//			File file1 = new File(deleteFile, contact.getImage());
//			file1.delete();
			
				 // ✅ Cloudinary delete
                String publicId = contact.getPublicId(); // store this in DB at upload time
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("Image deleted from Cloudinary: " + publicId);
	
			}
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
//				File deleteFile = new ClassPathResource("static/images").getFile();
//				File file1 = new File(deleteFile, oldContact.getImage());
//				file1.delete();
					
					 // ✅ Cloudinary delete
	                String publicId = oldContact.getPublicId(); 
	                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
	                System.out.println("Image deleted from Cloudinary: " + publicId);
	
				}
				// update new photo
//				File file = new ClassPathResource("static/images").getFile();
//				Path path = Paths.get(file.getAbsolutePath() + File.separator + multi.getOriginalFilename());
//				Files.copy(multi.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
//
//				contact.setImage(multi.getOriginalFilename());
				
				 // upload new photo to Cloudinary
				Map<String, Object> uploadResult = cloudinary.uploader().upload(
				        multi.getBytes(),
				        ObjectUtils.asMap("folder", "contacts")
				);

				String imageUrl = uploadResult.get("secure_url").toString();
				String publicId = uploadResult.get("public_id").toString();

				contact.setImage(imageUrl);     // frontend ke liye
				contact.setPublicId(publicId);  // delete/update ke liye

			} else {
				contact.setImage(oldContact.getImage());       // purani image hi rakho
			    contact.setPublicId(oldContact.getPublicId()); // purana publicId hi rakho

			}

			 String email = resolveEmail(principal);
			    user user = userRepo.getUserByEmail(email);
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
		
		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);
		model.addAttribute("user", user);
		
		return "normal/update_profile";
	}
	
	@PostMapping("/process-update-profile")
	public String updateProfileHandler(@ModelAttribute user user,@RequestParam("profileImage") MultipartFile multi, Model model, Principal principal) {
		try {
			
		
			user oldUser = this.userRepo.getUserById(user.getId());
			
			if(!multi.isEmpty()) {
				if(!oldUser.getImageUrl().equals("default.png")) {
//					File file = new ClassPathResource("/static/images").getFile();
//					File delFile = new File(file , oldUser.getImageUrl());
//					delFile.delete();
					
					// ✅ Cloudinary delete
					String publicId = oldUser.getPublicId();
					cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
					System.out.println("Image deleted from Cloudinary: " + publicId);
				}
				// update new photo
//				File file = new ClassPathResource("static/images").getFile();
//				String fname = oldUser.getId()+"_" +multi.getOriginalFilename();
//				File f = new File(file , fname);
//				  multi.transferTo(f);
//				  oldUser.setImageUrl(fname);
				// upload new photo to Cloudinary
	            Map<String, Object> uploadResult = cloudinary.uploader().upload(
	                    multi.getBytes(),
	                    ObjectUtils.asMap("folder", "profiles")
	            );

	            String imageUrl = uploadResult.get("secure_url").toString();
	            String publicId = uploadResult.get("public_id").toString();

	            oldUser.setImageUrl(imageUrl);   // frontend ke liye
	            oldUser.setPublicId(publicId);   // delete/update ke liye

				
				
			}
			else {
				 oldUser.setImageUrl(oldUser.getImageUrl());
		            oldUser.setPublicId(oldUser.getPublicId());

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

		 String email = resolveEmail(principal);
		    user user = userRepo.getUserByEmail(email);

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
