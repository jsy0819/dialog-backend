package com.dialog.user.domain;


// 사용자(회원)의 직무 또는 역할을 정의하는 Enum
public enum Job {

	// 1. 정해지지 않음
	NONE,

	// 2. 기획자 (PM)
	PROJECT_MANAGER,

	// 3. 프론트엔드 개발자
	FRONTEND_DEVELOPER,

	// 4. 백엔드 개발자
	BACKEND_DEVELOPER,

	// 5. 데이터베이스 관리자 (DBA)
	DATABASE_ADMINISTRATOR,

	// 6. 보안 개발자 (Security Engineer)
	SECURITY_DEVELOPER

}
