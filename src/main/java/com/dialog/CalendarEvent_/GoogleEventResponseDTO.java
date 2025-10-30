package com.dialog.CalendarEvent_;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;

@Getter
@Setter
//⭐ JSON 응답에 이 DTO에 없는 필드가 있어도 무시하고 파싱합니다. (필수!)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoogleEventResponseDTO {

	// Google이 발급한 이벤트의 고유 ID (로컬 DB의 googleEventId 필드에 저장됨)
	private String id;

	// 이벤트의 제목
	private String summary;

	// 이벤트의 상태 (예: "confirmed")
	private String status;

	// 이벤트가 생성된 시각 (RFC3339 format)
	private String created;

	// 이벤트의 HTML 링크 (사용자가 Google Calendar에서 이 일정을 볼 수 있는 URL)
	private String htmlLink;

	// 이벤트의 시작 시간 정보 (복잡한 구조이므로 단순하게 객체를 받거나, 별도의 DTO를 사용할 수 있습니다.)
	private EventDateTimeDTO start;
	
	private EventDateTimeDTO end;

	// ⭐⭐ 목록 조회 응답 (GET 응답 시 사용)
    // Google API 응답의 "items" 배열과 매핑됩니다. 
    @JsonProperty("items") // 필드 이름을 명시적으로 지정
	private List<GoogleEventResponseDTO> items; // ⭐ 스스로의 리스트를 담도록 변경

	public List<GoogleEventResponseDTO> getItems() {
        return items;
    }
}
