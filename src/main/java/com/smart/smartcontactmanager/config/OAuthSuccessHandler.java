package com.smart.smartcontactmanager.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collections;

import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.user;


@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private userRepo userRepo;
    
   


    Logger logger = LoggerFactory.getLogger(OAuthSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        DefaultOAuth2User detailUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = detailUser.getAttribute("email");
        String name = detailUser.getAttribute("name");

        user existingUser = userRepo.getUserByEmail(email);
        if (existingUser == null) {
            user newUser = new user();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPassword("oauth123");
            newUser.setRole("ROLE_USER");
            newUser.setEnabled(true);
            newUser.setImageUrl("default.png");

            userRepo.save(newUser);
            existingUser = newUser;

            logger.info("New OAuth user registered: {}", email);
        }
        
       


        response.sendRedirect("/user/index");
    }
}