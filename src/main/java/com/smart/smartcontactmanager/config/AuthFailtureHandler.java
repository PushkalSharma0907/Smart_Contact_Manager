package com.smart.smartcontactmanager.config;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.smart.smartcontactmanager.helper.Message;
import com.smart.smartcontactmanager.helper.MessageType;



@Component
public class AuthFailtureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        if (exception instanceof DisabledException) {
        	
        	Message message = new Message("Your account is disabled. Please contact support.", MessageType.red);
            // user is disabled
            HttpSession session = request.getSession();
            session.setAttribute("message", message);
            
            System.out.println("User account is disabled: " + exception.getMessage());
				
            response.sendRedirect("/signin");
        } else {
            response.sendRedirect("signin/?error=true");
            // request.getRequestDispatcher("/login").forward(request, response);

        }

    }

}
