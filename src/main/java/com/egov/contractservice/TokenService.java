package com.egov.contractservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TokenService
{
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    public boolean validateToken(String token, ApplicationContext ctx)
    {
        // Forward request to Auth-Service for Token Validation
        // If token is valid, update user PII in the database
        log.info("forwarding request to auth-service for token validation");
        WebClient authValidateWebClient = ctx.getBean("authValidateWebClientEurekaDiscovered", WebClient.class);

        String authResponse =authValidateWebClient.get()
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Thread is Blocked until the response is received

        log.info("Response from auth-service: {}", authResponse);

        if(authResponse.equals("INVALID"))
        {
            log.info("Invalid token: {}", token);
            return false;
        }
        else if (authResponse.equals("VALID"))
        {
            log.info("Valid token: {}", token);
            // Return success or failure response
            return true;
        }
        else
        {
            log.info("Error in auth-service: {}", authResponse);
            return false;
        }
    }



}
