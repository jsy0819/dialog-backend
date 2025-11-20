package com.dialog.keyword.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KeywordResponseDto {

    private Long keywordId;
    private String name;
    
    // [!!] 1. (User's name) 님이 3-1에서 추가한 '출처'를 담을 필드
    private String source; // "AI" 또는 "USER"

    // [!!] 2. 기존 생성자 수정 (source를 null로 설정)
    public KeywordResponseDto(Keyword keyword) {
        this.keywordId = keyword.getId();
        this.name = keyword.getName();
        this.source = null; // 또는 "UNKNOWN"
    }

    // [!!] 3. 3-2b 단계의 Service가 사용하는 새 생성자 추가
    public KeywordResponseDto(Keyword keyword, String source) {
        this.keywordId = keyword.getId();
        this.name = keyword.getName();
        this.source = source;
    }
}