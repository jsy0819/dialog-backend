package com.dialog.chatbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.dialog.user.service.CustomUserDetails;
import java.util.Map;
import com.dialog.user.domain.MeetUser;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:5500", allowCredentials = "true")
public class ChatbotController {
    
    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public ChatbotController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    // 회의록 검색 챗봇 (Python으로 전달)
    @PostMapping("/search")
    public ResponseEntity<String> searchChat(  // [수정] Map → String
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        System.out.println("🔹 [ChatBot] 요청 시작");
        
        if (userDetails != null) {
            MeetUser meetUser = userDetails.getMeetUser();
            
            Long userId = meetUser.getId();
            String job = meetUser.getJob() != null 
                ? meetUser.getJob().name() 
                : "NONE";
            String position = meetUser.getPosition() != null 
                ? meetUser.getPosition().name() 
                : "NONE";
            String userName = meetUser.getName();
            
            request.put("user_id", userId);
            request.put("user_job", job);
            request.put("user_position", position);
            request.put("user_name", userName);
            
            System.out.println("[ChatBot] User: " + userName + " (ID: " + userId + ", Job: " + job + ", Position: " + position + ")");
        }
        
        String url = fastApiBaseUrl + "/api/chat";
        System.out.println("[ChatBot] 전송 데이터: " + request);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("[ChatBot] Python 응답 성공");
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());
            
        } catch (Exception e) {
            System.err.println("[ChatBot] Python 호출 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    
    // FAQ 챗봇 (Python으로 전달)
    @PostMapping("/faq")
    public ResponseEntity<String> faqChat(  // [수정] Map → String
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        System.out.println("[FAQ] 요청 시작");
        
        if (userDetails != null) {
            MeetUser meetUser = userDetails.getMeetUser();
            
            Long userId = meetUser.getId();
            String job = meetUser.getJob() != null 
                ? meetUser.getJob().name() 
                : "NONE";
            String position = meetUser.getPosition() != null 
                ? meetUser.getPosition().name() 
                : "NONE";
            String userName = meetUser.getName();
            
            request.put("user_id", userId);
            request.put("user_job", job);
            request.put("user_position", position);
            request.put("user_name", userName);
            
            System.out.println("[FAQ] User: " + userName + " (ID: " + userId + ", Job: " + job + ", Position: " + position + ")");
        }
        
        String url = fastApiBaseUrl + "/api/faq";
        System.out.println("[FAQ] 전송 데이터: " + request);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("[FAQ] Python 응답 성공");
            return ResponseEntity.ok(response.getBody());
            
        } catch (Exception e) {
            System.err.println("[FAQ] Python 호출 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}