package com.smart.smartcontactmanager.controller;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.smart.smartcontactmanager.dao.ContactRepo;
import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.contact;
import com.smart.smartcontactmanager.entities.user;
import com.smart.smartcontactmanager.service.ContactServiceThread;

@RestController
public class searchController {
	
	@Autowired
	private userRepo userRepo;
	
	@Autowired
	private ContactRepo contactRepo;
	
	@Autowired
	private ContactServiceThread contactServiceThread;
	
	@GetMapping("/search/{query}")
	public ResponseEntity<?> searchContacts(@PathVariable("query") String query, Principal principal) throws InterruptedException, ExecutionException {
		String userName = principal.getName();
		user user = userRepo.getUserByUserName(userName);

		 List<contact> contacts = contactServiceThread.findContactsByNameContainingAndUser(query, user).get();

		return ResponseEntity.ok(contacts);
	}

}
