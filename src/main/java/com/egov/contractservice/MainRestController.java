package com.egov.contractservice;

import com.netflix.discovery.converters.Auto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("api/v1")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MainRestController
{
       private static final Logger log = LoggerFactory.getLogger(MainRestController.class);

       @Autowired
       ProjectRepository projectRepository;

       @Autowired
       ApplicationContext ctx;

       @Autowired
       TokenService tokenService;

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

       @PostMapping("float/project")
       public ResponseEntity<?> floatProject(@RequestBody Project project,
                                             @RequestHeader("Authorization") String token,
                                             HttpServletResponse httpServletResponse,
                                             HttpServletRequest httpServletRequest)
       {
              if(tokenService.validateToken(token,ctx))
              {
                     // Cookie verification code
                     List<Cookie> cookieList = null;
                     //Optional<String> healthStatusCookie = Optional.ofNullable(request.getHeader("health_status_cookie"));
                     Cookie[] cookies = httpServletRequest.getCookies();

                     if(cookies == null)
                     {
                            cookieList = new ArrayList<>();
                     }
                     else
                     {
                            // REFACTOR TO TAKE NULL VALUES INTO ACCOUNT
                            cookieList = List.of(cookies);
                     }

                  Cookie cookie1 = new Cookie("cs-cookie-1", String.valueOf(new Random().nextInt(1000000)));
                  cookie1.setMaxAge(3600);


                     if( cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-1")).findAny().isEmpty()) // COOKIE_CHECK
                     {
                            //Fresh Request Logic

                            project.setStatus("FLOATED");
                            projectRepository.save(project);

                            WebClient webClient = ctx.getBean("profileNotifyContractorsEurekaBalanced", WebClient.class);
                            // forward ASYNC request to the profile service for shortlisting the contractors

                            Mono<String> responseMono = webClient.post()
                                    .uri("/{location}", project.getLocation()) // Append the path variable to the base URL
                                    .header("Authorization", token)
                                    .retrieve()
                                    .bodyToMono(String.class); // ASYNCHRONOUS

                         // Async Handler Code
                            responseMono.subscribe(
                                    response -> {
                                           log.info(response+" from the profile service");
                                           redisTemplate.opsForValue().set(String.valueOf(cookie1.getValue()), response);
                                           //redisTemplate.opsForValue().set(credential.getCitizenid().toString(), credential.getPassword());
                                    },
                                    error ->
                                    {
                                           log.info("error processing the response from profile service"+error);
                                    });
                            //
//
//                            // Cookie generation code

                            httpServletResponse.addCookie(cookie1);

                            return ResponseEntity.ok("Project Floated Succesfully");//
                     }
                     else
                     {
                            // Cookie Already Present - Follow Up Logic
                            String cacheValue = (String)redisTemplate.opsForValue().
                                    get((cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-1")).findAny()).get().getValue());
                            log.info("Cookie already present");
                            return ResponseEntity.status(200).body(cacheValue);
                     }

              }
              else
              {
                     return ResponseEntity.status(401).body("Invalid token");
              }

       }


}
