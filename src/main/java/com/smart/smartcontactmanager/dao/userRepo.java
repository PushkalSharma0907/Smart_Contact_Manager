package com.smart.smartcontactmanager.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.smart.smartcontactmanager.entities.user;

public interface userRepo extends JpaRepository<user, Integer> {

	@Query("select u from user u where u.email =?1 ")
	public user getUserByUserName(String email);
	
	public user getUserById(int id);
}
