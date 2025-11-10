package com.dialog.user.controller;

import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.service.MeetingService;
import com.dialog.user.domain.AdminResponse;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.domain.TodayStatsDto;
import com.dialog.user.domain.UserSettingsUpdateDto;
import com.dialog.user.service.AdminService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;
    private final MeetingService meetingService;
    
    // 관리자만 접근 가능하도록 PreAuthorize 적용 (ROLE_ADMIN 등)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<AdminResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) {    	
        adminService.deleteUser(userId);        
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/meetings")
    public ResponseEntity<List<MeetingCreateResponseDto>> getAllMeetings() {
        List<MeetingCreateResponseDto> meetings = meetingService.getAllMeetings();        
        return ResponseEntity.ok(meetings);
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/settings/{userId}")
    public ResponseEntity<Void> updateUserSettings(
            @PathVariable("userId") Long userId,
            @RequestBody UserSettingsUpdateDto updateDto) {
        
        adminService.updateUserSettings(userId, updateDto);
        return ResponseEntity.ok().build();
    }
    
    // 어드민 페이지 가입자 통계, 전체 회의수 조회 
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics/users")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUserCount", adminService.getTotalUserCount());
        result.put("newUsersLast7Days", adminService.getNewUserCountLast7Days());
        result.put("totalMeetingCount", adminService.getTotalMeetingCount());
        result.put("meetingCountThisMonth", adminService.getMeetingCountThisMonth());
        return ResponseEntity.ok(result);
    }
      
    // 오늘 가입한 사용자
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/todayregisteruser")
    public long getTodayRegisterUser() {
        return adminService.countTodayRegisteredUsers();
    }

    // 오늘 생성한 회의	
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/todaycreatemeet")
    public long getTodayCreateMeet() {
        return adminService.countTodayCreatedMeetings();
    }
    
    // 어제 오늘 생성, 가입 한 사용자 회의 차이수 조회
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/todaystats")
    public TodayStatsDto getTodayStats() {
        return adminService.getTodayStats();
    }
    
    
    
    
}  
