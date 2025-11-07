package com.dialog.user.controller;

import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.service.MeetingService;
import com.dialog.user.domain.AdminResponse;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.domain.UserSettingsUpdateDto;
import com.dialog.user.service.AdminService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final MeetingService meetingService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") Long userId) {    	
        adminService.deleteUser(userId);        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/meetings")
    public ResponseEntity<List<MeetingCreateResponseDto>> getAllMeetings() {
        List<MeetingCreateResponseDto> meetings = meetingService.getAllMeetings();        
        return ResponseEntity.ok(meetings);
    }
    
    @PutMapping("/users/settings/{userId}")
    public ResponseEntity<Void> updateUserSettings(
            @PathVariable("userId") Long userId,
            @RequestBody UserSettingsUpdateDto updateDto) {
        
        adminService.updateUserSettings(userId, updateDto);
        return ResponseEntity.ok().build();
    }
    
}
