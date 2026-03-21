package com.smart.smartcontactmanager.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;

import com.smart.smartcontactmanager.dao.userRepo;
import com.smart.smartcontactmanager.entities.user;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;


@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private userRepo userRepo;
    
 // ✅ Bean inject nahi, seedha instance — no circular dependency
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
   
    Logger logger = LoggerFactory.getLogger(OAuthSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        DefaultOAuth2User detailUser = (DefaultOAuth2User) authentication.getPrincipal();
        String email = detailUser.getAttribute("email");
        String name = detailUser.getAttribute("name");
        
       

        user existingUser = userRepo.getUserByEmail(email);
        if (existingUser == null) {
        	 // ✅ Naya random secret — har user ke liye alag, koi guess nahi kar sakta
            String randomSecret = passwordEncoder.encode(UUID.randomUUID().toString());
        	
            user newUser = new user();
            newUser.setName(name);
            newUser.setEmail(email);
            newUser.setPassword(randomSecret);
            newUser.setRole("ROLE_USER");
            newUser.setEnabled(true);
            newUser.setImageUrl("default.png");

            userRepo.save(newUser);
            existingUser = newUser;

            logger.info("New OAuth user registered: {}", email);
        }
        
        // ✅ DB se role lo aur manually SecurityContext mein set karo
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(existingUser.getRole())  // ROLE_ADMIN ya ROLE_USER
        );

        // ✅ Naya authentication object banao DB role ke saath
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
            authentication.getPrincipal(),
            authentication.getCredentials(),
            authorities
        );

        // ✅ SecurityContext update karo
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        logger.info("OAuth login - email: {}, role: {}, authorities: {}",
                    email, existingUser.getRole(), authorities);

        boolean isAdmin = existingUser.getRole().equals("ROLE_ADMIN");
        response.sendRedirect(isAdmin ? "/admin/dashboard" : "/user/index");
    }
}