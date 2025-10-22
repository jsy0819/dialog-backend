package com.dialog.user.domain;

/**
 * 사용자(회원)의 직무 또는 역할을 정의하는 Enum
 * (이유: 회의록에서 '어떤 역할을 맡은 사람'인지 구분하기 위해 필요)
 */
public enum Job {

	// 1. '정해지지 않음' (기본값)
	// (이유: 회원가입 시 직무를 선택하지 않으므로,
	// 가입 직후 모든 사용자는 이 'NONE' 상태를 기본값으로 가집니다.
	// 이후 '설정 -> 개인정보' 메뉴에서 실제 직무로 변경 가능합니다.)
	NONE,

	// 2. 기획자 (PM)
	// (이유: 프로젝트의 방향성과 요구사항을 정의하는 핵심 역할)
	PROJECT_MANAGER,

	// 3. 프론트엔드 개발자
	// (이유: 사용자 인터페이스(UI) 개발 담당)
	FRONTEND_DEVELOPER,

	// 4. 백엔드 개발자
	// (이유: 서버 로직 및 API 개발 담당. '님'의 주 직무)
	BACKEND_DEVELOPER,

	// 5. 데이터베이스 관리자 (DBA)
	// (이유: 데이터 저장 및 관리를 책임지는 역할)
	DATABASE_ADMINISTRATOR,

	// 6. 보안 개발자 (Security Engineer)
	// (이유: 시스템 보안 및 취약점 분석 담당)
	SECURITY_DEVELOPER

	// 참고: 만약 '기획자'를 'PM'으로 줄여쓰고 싶다면 PM으로 해도 되지만,
	// 코드는 명확한 것이 좋으므로 풀 네임(PROJECT_MANAGER)을 추천합니다.

}
