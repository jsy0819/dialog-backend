package com.dialog.user.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 오늘 가입한 사용자 및 오늘 생성한 회의 조회
// 어제 오늘 가입 사용자 생성 회의 차이를 비교하기 위한 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatsDto {
	
    private long todayCreateMeetCount;
    private long todayCreateMeetChange;
    private long todayRegisterUserCount;
    private long todayRegisterUserChange;
    
}
