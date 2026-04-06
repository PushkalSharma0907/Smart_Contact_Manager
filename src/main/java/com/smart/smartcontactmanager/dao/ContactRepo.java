package com.smart.smartcontactmanager.dao;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smart.smartcontactmanager.entities.contact;
import com.smart.smartcontactmanager.entities.user;

public interface ContactRepo extends JpaRepository<contact, Integer> {
	
	
	@Query("from contact as c where c.user.id =?1 ")
	public Page<contact> findContactsByUserId(int userId , Pageable pageable);
	
	
	public List<contact> findContactsByUserId(int id );
	
	
	public List<contact> findContactsByNameContainingAndUser(String keyword , user user);
	
	
}
