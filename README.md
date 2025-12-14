# 🔧 DialoG Backend

> Spring Boot 기반 실시간 회의 관리 및 AI 분석 백엔드 서버

---

## 📌 프로젝트 소개

DialoG Backend는 **실시간 회의 녹음 및 AI 기반 회의록 생성**을 지원하는 REST API 서버입니다.

**Spring Boot 3.x**와 **Spring Security**를 기반으로 JWT 인증, OAuth 2.0 소셜 로그인, Google Calendar 연동 등 회의 관리에 필요한 모든 백엔드 기능을 제공합니다.

**핵심 특징**
- JWT + OAuth 2.0 (Google, Kakao) 인증 시스템
- 회의 생성부터 AI 요약까지 전체 라이프사이클 관리
- 실시간 발화 로그 저장 및 화자 매핑
- Google Calendar 양방향 연동 (일정 자동 생성/동기화)
- FastAPI AI 서버 연동 (회의록 검색, IT 용어 FAQ)
- AWS RDS 기반 데이터 관리
- 전역 예외 처리 및 RESTful API 설계

---

## 🛠️ 기술 스택

| 카테고리 | 기술 스택 | 용도 |
|---------|----------|------|
| **언어 & 프레임워크** | ![Java](https://img.shields.io/badge/Java_17-007396?logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.x-6DB33F?logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=springsecurity&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/JPA-6DB33F?logo=spring&logoColor=white) | REST API 서버 구현<br/>JWT + OAuth2 인증, ORM 매핑 |
| **데이터베이스** | ![MySQL](https://img.shields.io/badge/MySQL_8.0-4479A1?logo=mysql&logoColor=white) ![AWS RDS](https://img.shields.io/badge/AWS_RDS-527FFF?logo=amazonrds&logoColor=white) | 관계형 데이터베이스 (MySQL 8.0)<br/>AWS RDS 호스팅 |
| **인증 & 보안** | ![OAuth 2.0](https://img.shields.io/badge/OAuth_2.0-EB5424?logo=auth0&logoColor=white) ![JWT](https://img.shields.io/badge/JWT-000000?logo=jsonwebtokens&logoColor=white) | Google/Kakao 소셜 로그인<br/>Access Token(3h) + Refresh Token(7d) |
| **외부 API** | ![Google Calendar](https://img.shields.io/badge/Google_Calendar-4285F4?logo=google-calendar&logoColor=white) ![NAVER Cloud](https://img.shields.io/badge/NAVER_Object_Storage-03C75A?logo=naver&logoColor=white) | 일정 연동 (WebClient)<br/>녹음 파일 저장 |
| **AI 연동** | ![FastAPI](https://img.shields.io/badge/FastAPI-009688?logo=fastapi&logoColor=white) | Python AI 서버 통신 (RestTemplate)<br/>회의록 검색, IT 용어 FAQ 챗봇 |
| **컨테이너 & CI/CD** | ![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white) ![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?logo=githubactions&logoColor=white) | Docker Compose 배포<br/>develop 브랜치 자동 배포 |
| **인프라** | ![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?logo=amazonec2&logoColor=white) | Ubuntu 24 서버 호스팅 |

---

## 📋 목차

- [팀원 구성](#-팀원-구성)
- [프로젝트 구조](#-프로젝트-구조)
- [주요 기능](#-주요-기능)
- [보안 설정](#-보안-설정)

---

## 👥 팀원 구성

| 이름 | 역할 | 담당 영역 |
|------|------|-----------|
| **강승훈** | Authentication & Admin | OAuth2 소셜 로그인, RefreshToken 관리<br/>비밀번호 재설정, 전역 예외 처리<br/>CORS/쿠키 설정 |
| **김나운** | STT Integration & Config | STT 실시간 스트리밍/발화자 구분 서버 연결<br/>CORS/쿠키 설정 개선 |
| **박인호** | Calendar & Admin | Google Calendar 연동, To-Do 상태 동기화<br/>관리자 페이지(사용자/회의 관리) |
| **장문선** | Chatbot & Deployment | FastAPI 챗봇 연동, CI/CD 구축<br/>HTTPS/도메인 설정, OAuth 배포 최적화 |
| **지승엽** | Meeting & Transcript | Meeting/Transcript 엔티티 설계 및 API<br/>recordFinish API 구현 |

---

## 📁 프로젝트 구조

```
src/main/java/com/dialog/
├── user/                   # 사용자 인증 및 관리
├── meeting/                # 회의 CRUD 및 상태 관리
├── transcript/             # 발화 로그 저장
├── recording/              # 녹음 파일 메타데이터
├── meetingresult/          # AI 분석 결과
├── actionitem/             # 액션아이템 관리
├── keyword/                # 키워드 추출
├── participant/            # 회의 참가자
├── todo/                   # 할일 관리
├── calendarevent/          # Google Calendar 연동
├── token/                  # JWT/OAuth Token 관리
├── security/               # Spring Security 설정
├── exception/              # 전역 예외 처리
├── config/                 # CORS, WebClient 설정
├── googleauth/             # Google 인증
├── chatbot/                # FastAPI AI 서버 연동
├── email/                  # 이메일 발송
└── global/                 # 공통 유틸

src/main/resources/
├── application.properties  # 환경변수 설정
└── application.yml         # Spring Boot 설정
```

---

## 🎯 주요 기능

### 1. 사용자 인증

**일반 회원가입 / 로그인**
- BCrypt 암호화 (12자 이상)
- 이메일 중복 체크
- 약관 동의 필수

**OAuth 2.0 소셜 로그인**
- Google, Kakao 지원
- 신규 사용자 자동 회원가입
- 기존 사용자 프로필 업데이트 (이름, 프로필 사진)
- OAuth2 토큰 자동 저장 (UserSocialToken 테이블)

**JWT 토큰 관리**
- Access Token: 3시간 유효 (HttpOnly 쿠키)
- Refresh Token: 7일 유효 (자동 재발급)
- 만료 시 `/api/reissue` 엔드포인트로 자동 갱신

**비밀번호 재설정**
- 이메일 인증 토큰 발송 (JavaMailSender)
- 토큰 유효기간 1시간
- URL: `/api/auth/resetPassword`

---

### 2. 회의 관리

**회의 생성**
- 제목, 설명, 예약 시간, 참가자 목록 입력
- 관심 키워드 사전 입력 (highlightKeywords)
- 초기 상태: `SCHEDULED`

**회의 녹음 및 종료**
- 녹음 시작: `RECORDING` 상태 전환
- 녹음 종료: Recording 저장 시 자동 `COMPLETED` 전환
- Transcript 일괄 저장 (발화 로그)

**AI 요약 생성**
- FastAPI 서버와 통신 (`POST /api/meetings/summarize`)
- HyperCLOVA X 기반 요약 생성
- 반환 데이터:
  - purpose (회의 목적)
  - agenda (주요 안건)
  - summary (전체 요약)
  - importance (HIGH/MEDIUM/LOW + 판단 사유)
  - keywords (AI 추출 키워드)

**액션아이템 자동 생성**
- 발화 로그에서 AI가 할 일 추출
- 담당자, 마감 기한 자동 매핑
- Google Calendar 자동 연동 가능

---

### 3. 발화 로그 (Transcript)

**저장 구조**
- 화자 ID (speakerId): STT 원본 ID (예: "Speaker 1")
- 화자 이름 (speakerName): 사용자 매핑 이름 (예: "가나디")
- 텍스트, 시작/종료 시간(ms), 순서(sequenceOrder)

**화자 매핑**
- 프론트엔드에서 "Speaker 1" → "가나디" 매핑
- `/api/transcripts/meeting/{id}/speaker` PATCH 요청으로 일괄 변경

**소프트 삭제**
- `isDeleted` 플래그로 관리
- 삭제된 발화는 UI에서 숨김 처리
- 복구 가능 (`PATCH /api/transcripts/{id}/restore`)

---

### 4. Google Calendar 연동

**인증 플로우**
1. `/api/calendar/link/start` 호출 → authUrl 생성
2. 사용자가 Google 인증 페이지에서 권한 승인
3. `/auth/google/link/callback`으로 code 전달
4. code → Access/Refresh Token 교환
5. UserSocialToken 테이블에 저장

**일정 동기화**
- 회의 생성 시 자동으로 Google Calendar 일정 생성
- 액션아이템 생성 시 일정 자동 추가
- Google Calendar에서 일정 수정 시 DB 동기화

**API 기능**
- 일정 조회 (기간별)
- 일정 생성/수정/삭제
- 중요 표시 토글
- 완료 상태 토글

---

### 5. 관리자 대시보드

**사용자 관리**
- 전체 사용자 목록 조회
- 사용자 삭제 (계정 비활성화)
- 직무/직급 강제 수정

**회의 관리**
- 전체 회의 목록 조회
- 회의 삭제 (연관 데이터 CASCADE)

**통계**
- 총 가입자 수
- 최근 7일 가입자 수
- 오늘/어제 가입자/회의 생성 수 비교
- 월별 회의 생성 추이

---

### 6. 챗봇 (FastAPI 연동)

**회의록 검색 챗봇**
- 사용자 질문을 FastAPI 서버로 전달
- RAG 기반 회의록 검색
- 사용자 컨텍스트 포함 (userId, job, position, name)

**IT 용어 FAQ 챗봇**
- JSON 검색 → CLOVA Chatbot Builder → HyperCLOVA X 3단계 Fallback
- 하이브리드 방식으로 AI API 호출 최소화

---

## 🔒 보안 설정

- **JWT 인증**: Access Token (3시간), Refresh Token (7일)
- **비밀번호**: BCrypt 암호화 (strength 12)
- **쿠키**: HttpOnly, Secure, SameSite 설정
- **CORS**: dialogai.ddns.net, localhost:5500 허용
- **OAuth 2.0**: Google, Kakao 소셜 로그인
