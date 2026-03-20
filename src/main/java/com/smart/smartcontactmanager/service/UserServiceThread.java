package com.smart.smartcontactmanager.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.smart.smartcontactmanager.entities.user;


import com.smart.smartcontactmanager.dao.*;

@Service
public class UserServiceThread {
	
	@Autowired
    private userRepo userRepo;

    @Autowired
    @Qualifier("io")
    private ExecutorService ioExecutor;

    public Future<user> getUserByEmail(String email) {
        return ioExecutor.submit(() -> userRepo.getUserByEmail(email));
    }

    public Future<user> getUserById(int id) {
        return ioExecutor.submit(() -> userRepo.getUserById(id));
    }

    public Future<user> getUserByUserName(String email) {
        return ioExecutor.submit(() -> userRepo.getUserByUserName(email));
    }

    public Future<user> findByEmailToken(String token) {
        return ioExecutor.submit(() -> userRepo.findByEmailToken(token));
    }


}
