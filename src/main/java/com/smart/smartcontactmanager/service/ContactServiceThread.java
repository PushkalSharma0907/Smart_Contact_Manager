package com.smart.smartcontactmanager.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.smart.smartcontactmanager.entities.user;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;


import com.smart.smartcontactmanager.dao.ContactRepo;
import com.smart.smartcontactmanager.entities.contact;

@Service
public class ContactServiceThread {
	
	 @Autowired
	    private ContactRepo contactRepo;

	    @Autowired
	    @Qualifier("io")   // ye tumhare @Bean(name="io") wala virtual thread pool hai
	    private ExecutorService ioExecutor;

	    public Future<Page<contact>> findContactsByUserId(int userId, Pageable pageable) {
	        return ioExecutor.submit(() -> contactRepo.findContactsByUserId(userId, pageable));
	    }

	    public Future<List<contact>> findContactsByUserId(int id) {
	        return ioExecutor.submit(() -> contactRepo.findContactsByUserId(id));
	    }

	    public Future<List<contact>> findContactsByNameContainingAndUser(String keyword, user user) {
	        return ioExecutor.submit(() -> contactRepo.findContactsByNameContainingAndUser(keyword, user));
	    }
	    
		public Future<contact> findContactById(int id) {
			return ioExecutor.submit(() -> contactRepo.findById(id).orElse(null));
		}


}
