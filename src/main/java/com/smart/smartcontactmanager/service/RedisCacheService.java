package com.smart.smartcontactmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.smart.smartcontactmanager.entities.contact;

@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, contact> redisTemplate;

    @Resource(name = "io")
    private ExecutorService ioExecutor;

    // ✅ Prefix consistent with entity name
    private static final String CONTACT_KEY_PREFIX = "contact:user:";

    // Save contact with TTL
    public void saveContact(contact contact) throws InterruptedException, ExecutionException {
        ioExecutor.submit(() -> {
            String key = CONTACT_KEY_PREFIX + contact.getUser().getId() + ":" + contact.getCid();
            redisTemplate.opsForValue().set(key, contact, 30, TimeUnit.MINUTES);
           
            System.err.println("redis with vth exe for save contact");
            
        }).get();
    }

    // Get contact by ID
    public contact getContact(int userId, Long contactId) throws InterruptedException, ExecutionException {
        return ioExecutor.submit(() -> {
            String key =  CONTACT_KEY_PREFIX + userId + ":" + contactId;
            System.err.println("redis with vth exe for get contact");

            return redisTemplate.opsForValue().get(key);
        }).get();
    }

    // Delete contact by ID
    public void deleteContact(int userId, Long contactId) throws InterruptedException, ExecutionException {
        ioExecutor.submit(() -> {
        	 String key = CONTACT_KEY_PREFIX + userId + ":" + contactId;            
        	 redisTemplate.delete(key);
             System.err.println("redis with vth exe for del contact");

            return null;
        }).get();
    }

    // Get all contacts
    public Future<List<contact>> getAllContacts(int userId) throws InterruptedException, ExecutionException {
        return ioExecutor.submit(() -> {
        	 Set<String> keys = redisTemplate.keys(CONTACT_KEY_PREFIX + userId + ":*");
             List<contact> contacts = new ArrayList<>();
             for (String key : keys) {
                 contact c = redisTemplate.opsForValue().get(key);
                 if (c != null) {
                     contacts.add(c);
                 }
             }
             System.err.println("redis with vth exe for get all contact");

             return contacts;
         });
     }

}