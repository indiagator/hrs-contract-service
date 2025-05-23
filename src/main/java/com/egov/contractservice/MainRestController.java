package com.egov.contractservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        Producer producer;

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

       @PostMapping("float/project")
       public ResponseEntity<?> floatProject(@RequestBody Project project,
                                             @RequestHeader("Authorization") String token,
                                             @RequestHeader("traceparent") String traceparent,
                                             HttpServletResponse httpServletResponse,
                                             HttpServletRequest httpServletRequest) throws JsonProcessingException {
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


               // Cookie generation code
               Cookie cookie1 = new Cookie("cs-cookie-1", String.valueOf(new Random().nextInt(1000000)));
               cookie1.setMaxAge(3600);

               // Cookie generation code
               Cookie cookie3 = new Cookie("cs-cookie-2", String.valueOf(new Random().nextInt(1000000)));
               cookie1.setMaxAge(3600);

               // Cookie generation code
               Cookie cookie4 = new Cookie("domain-event-cookie", String.valueOf(new Random().nextInt(1000000)));
               cookie1.setMaxAge(3600);

               redisTemplate.opsForValue().set(cookie1.getValue(), "Stage 1 Complete - Stage 2 Processing");
               redisTemplate.opsForValue().set(cookie3.getValue(), "Stage 2 Complete - Stage 3 Processing");


               if( cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-1")).findAny().isEmpty()) // COOKIE_CHECK
               {
                   //Fresh Request Logic

                   project.setStatus("FLOATED");
                   Project savedProject = projectRepository.save(project);

                   DomainEvent domainEvent  = new DomainEvent();
                   domainEvent.setEventType("CREATE");
                   domainEvent.setDbname("hrs-mongodb");
                   domainEvent.setDocumentid(savedProject.getProjectid());
                   domainEvent.setCollectionname("projects");
                   domainEvent.setPrincipalid(project.getCustomerid());
                   domainEvent.setEndpoint("contract-service/api/v1/float/project");
                   domainEvent.getTraceparent().add(traceparent);

                   log.info("Domain Event updation in Progress: "+domainEvent);
                   redisTemplate.opsForValue().set(cookie4.getValue(), domainEvent);
                   log.info("Domain Event updated in Redis with key : "+ cookie4.getValue());

                   // Cookie generation code
                   Cookie cookie2 = new Cookie("projectid", String.valueOf(savedProject.getProjectid()));
                   cookie1.setMaxAge(3600);

                   WebClient webClient = ctx.getBean("profileNotifyContractorsEurekaBalanced", WebClient.class);
                   // forward ASYNC request to the profile service for shortlisting the contractors

                   Mono<String> responseMono = webClient.post()
                           .uri("/{location}", project.getLocation()) // Append the path variable to the base URL
                           .header("Authorization", token)
                           .retrieve()
                           .bodyToMono(String.class); // ASYNCHRONOUS

                   // Async Handler Code
                   responseMono.subscribe(
                           response ->
                           {
                               log.info(response+" from the profile service");
                               redisTemplate.opsForValue().set(cookie1.getValue(), response);
                           },
                           error ->
                           {
                               log.info("error processing the response from profile service"+error);
                           });
                   //

                   httpServletResponse.addCookie(cookie1);
                   httpServletResponse.addCookie(cookie2);
                   httpServletResponse.addCookie(cookie4);

                   return ResponseEntity.ok("Project Floated Succesfully");
               }
               else if( cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-2")).findAny().isPresent())
               {
                   // follow up logic for the second cookie

                   String cacheKey =  cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-2")).findAny().get().getValue();
                   String cacheValue = (String)redisTemplate.opsForValue().get(cacheKey);


                   if(cacheValue.equals("Stage 2 Complete - Stage 3 Processing"))
                   {
                       log.info("Cookie already present | Response not yet ready");
                       return ResponseEntity.status(200).body(cacheValue);
                   }
                   else
                   {
                       String domainEventKey =  cookieList.stream().filter(cookie -> cookie.getName().equals("domain-event-cookie")).findAny().get().getValue();
                       DomainEvent domainEvent = (DomainEvent) redisTemplate.opsForValue().get(domainEventKey);
                       domainEvent.getTraceparent().add(traceparent);
                       redisTemplate.opsForValue().set(domainEventKey, domainEvent);

                       producer.pubDomainEvent(domainEvent);

                       return ResponseEntity.status(200).body(cacheValue);
                   }
               }
               else if( cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-1")).findAny().isPresent())
               {
                   // Cookie Already Present - Follow Up Logic

                   String cacheKey =  cookieList.stream().filter(cookie -> cookie.getName().equals("cs-cookie-1")).findAny().get().getValue();
                   String cacheValue = (String)redisTemplate.opsForValue().get(cacheKey);

                   if(cacheValue.equals("Stage 1 Complete - Stage 2 Processing"))
                   {
                       log.info("Cookie already present");
                       return ResponseEntity.status(200).body(cacheValue);
                   }
                   else
                   {
                       ProConLink proConLink = new ProConLink();
                       ObjectMapper mapper  = new ObjectMapper();

                       List<Long> list = mapper.readValue(cacheValue, new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {});


                       String projectid =  cookieList.stream().filter(cookie -> cookie.getName().equals("projectid")).findAny().get().getValue();

                       proConLink.setProjectid(projectid);
                       proConLink.setContractorids(list);

                       //forward the request to the match service

                       WebClient webClient = ctx.getBean("matchProConEurekaBalanced", WebClient.class);
                       // forward ASYNC request to the profile service for shortlisting the contractors

                       Mono<String> responseMono = webClient.post()
                               .header("Authorization", token)
                               .bodyValue(proConLink) // Send ProConLink as the request body
                               .retrieve()
                               .bodyToMono(String.class); // ASYNCHRONOUS

                       // Async Handler Code
                       responseMono.subscribe(
                               response ->
                               {
                                   log.info(response+" from the match service");
                                   redisTemplate.opsForValue().set(cookie3.getValue(), response);
                               },
                               error ->
                               {
                                   log.info("error processing the response from match service"+error);
                               });
                       //

                       String domainEventKey =  cookieList.stream().filter(cookie -> cookie.getName().equals("domain-event-cookie")).findAny().get().getValue();
                       DomainEvent domainEvent = (DomainEvent) redisTemplate.opsForValue().get(domainEventKey);
                       domainEvent.getTraceparent().add(traceparent);
                       redisTemplate.opsForValue().set(domainEventKey, domainEvent);


                       httpServletResponse.addCookie(cookie3);
                       return ResponseEntity.status(200).body(cacheValue + " - Stage 2 Complete - Stage 3 Processing");
                   }

               }
               else
               {
                   return ResponseEntity.status(500).body("Internal Server Error");
               }
           }
           else
           {
               return ResponseEntity.status(401).body("Invalid token");
           }

       }

    @GetMapping("get/projects/{customerid}")
    public ResponseEntity<?> getProjectsOfCustomer(@PathVariable("customerid") Long customerid) throws JsonProcessingException
    {
        List<Project> projects =  projectRepository.findByCustomerid(customerid);

        ObjectMapper objectMapper = new ObjectMapper();
        String datum = objectMapper.writeValueAsString(projects);

        return ResponseEntity.ok(datum);
    }


}
