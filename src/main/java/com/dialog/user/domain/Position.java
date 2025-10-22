package com.dialog.user.domain;

/**
 * 사용자의 직급 (position)
 * * 이유: 향후 개발 직군 외(예: 마케팅, 영업, 기획)로 서비스를 확장할 것을 고려하여,
 * 특정 산업군에 종속되지 않는 가장 표준적이고 일반적인 비즈니스 영어 직급 체계를 사용합니다.
 * (사원 -> 대리 -> 과장 -> 차장 -> 부장 순서)
 */
public enum Position {

	// 1. '정해지지 않음' (기본값)
	// (이유: 회원가입 시 직급을 선택하지 않으므로,
	// 가입 직후 모든 사용자는 이 'NONE' 상태를 기본값으로 가집니다.
	// 이후 '설정 -> 개인정보' 메뉴에서 실제 직급으로 변경 가능합니다.)
	NONE,

	// 2. 사원 (Staff / Associate)
	// 이유: 조직의 기본 구성원이자 실무자입니다.
	// 'STAFF'가 가장 포괄적이고 일반적인 용어입니다.
	STAFF,

	// 3. 대리 (Assistant Manager / Senior Staff)
	// 이유: '사원'보다 상위 레벨의 실무자이며 '과장'의 바로 아랫단계입니다.
	// 한국의 '대리' 직급에 대한 번역으로 'ASSISTANT_MANAGER'(보조 관리자)가
	// 대외적으로 가장 널리 통용됩니다.
	ASSISTANT_MANAGER,

	// 4. 과장 (Manager)
	// 이유: 팀의 중간 관리자(파트장) 또는 핵심 실무 책임자입니다.
	// 전 세계 모든 비즈니스에서 통용되는 표준 직급입니다.
	MANAGER,

	// 5. 차장 (Senior Manager / Deputy General Manager)
	// 이유: '과장(Manager)'보다 상위 직급이며 '부장'의 아랫단계입니다.
	// 'MANAGER'보다 숙련되었음을 의미하는 'SENIOR_MANAGER'가
	// 이 직급을 표현하기에 가장 적절하고 보편적입니다.
	SENIOR_MANAGER,

	// 6. 부장 (General Manager / Director)
	// 이유: 하나의 '부(Department)'를 총괄하는 책임자(부서장)입니다.
	// 'GENERAL_MANAGER'(총괄 관리자)가 이 의미에 가장 가깝습니다.
	GENERAL_MANAGER

	// 참고: '사장' 등과 같은 '임원(Executive)' 직급
	// (예: DIRECTOR(이사), VICE_PRESIDENT(상무/전무), CEO(사장))은
	// 실무자 중심의 회의록에서는 우선순위가 낮으므로,
	// 요청하신 대로 '부장'까지만 포함하는 것이 좋습니다.

}
