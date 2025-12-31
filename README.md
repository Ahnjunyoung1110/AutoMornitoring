# HLS Stream Health Monitor (HSHM)

실시간 HLS 스트림 상태 자동 모니터링 시스템  
**주기적 검사 → 이상 감지 → 알람 자동화**

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [프로젝트 필요성](#2-프로젝트-필요성)
3. [아키텍처 개요](#3-아키텍처-개요)
4. [주요 기능](#4-주요-기능)
5. [기술 스택](#5-기술-스택)
6. [Quick Start](#6-quick-start)
7. [FFmpeg 설치](#7-ffmpeg-설치-ffprobe-포함)
8. [실행 방법](#8-실행-방법-docker-compose)
9. [Configuration](#9-configuration)
10. [Profiles](#10-profiles)
11. [문서 및 참고 링크](#11-문서-및-참고-링크)

---

## 1. 프로젝트 개요

**HLS Stream Health Monitor (HSHM)**는 다수의 HLS(.m3u8) 라이브 스트림이 정상적으로 송출되고 있는지를 주기적으로 자동 검증하고, 장애 발생 시 이를 즉시 감지·전파하기 위한 실시간 모니터링 시스템입니다.

라이브 채널 수가 수백 개로 증가하는 환경에서 사람이 직접 24시간 스트림 상태를 점검하는 것은 현실적으로 불가능하며, 장애 인지 지연은 서비스 품질 저하와 사용자 신뢰 하락으로 직결됩니다.

본 프로젝트는 이러한 운영 문제를 해결하고, 대규모 스트리밍 환경에서도 안정적인 모니터링이 가능하도록 설계되었습니다.

---

## 2. 프로젝트 필요성

### 대규모 라이브 채널 운영의 한계

- 수백 개 이상의 라이브 채널 수동 점검 불가
- 장애 인지 지연 시 사용자 이탈 및 신뢰도 하락

### SSAI 기반 광고 송출 검증 문제

- 회사 수익 구조가 SSAI(Server-Side Ad Insertion)에 의존
- 광고 노출 여부를 사람이 직접 확인하는 비효율적인 운영 방식
- 광고 구간의 m3u8을 자동으로 검증·저장할 시스템 필요

---

## 3. 아키텍처 개요

본 시스템은 **모듈러 모놀리식(Modular Monolith)** 구조를 기반으로 설계되었습니다.

- 단일 애플리케이션 구조
- 도메인 단위로 명확히 분리된 모듈 구성
- 향후 MSA 전환을 고려한 경계 설계
- 모듈 간 비동기 통신은 RabbitMQ 사용
- 조회 및 즉각 응답이 필요한 기능은 REST API 기반 처리

### 전체 처리 흐름

1. 신규 모니터링 채널 등록
2. FFmpeg(ffprobe)를 통한 초기 스트림 검증
3. 검증 결과 DB 저장
4. 주기적 m3u8 모니터링 시작
5. 상태 유효성 판단 및 알람 전송
6. TTL + DLX 기반 주기 재실행

---

## 4. 주요 기능

### 실시간 모니터링 대시보드

- 모든 채널 상태 실시간 시각화
- 정상 / 실패 / 중지 상태 구분
- Trace ID, 채널명 기반 검색 및 정렬

### 신규 모니터링 등록 및 제어

- Manifest URL만으로 모니터링 즉시 시작
- UI를 통한 모니터링 중지 및 수동 갱신

### 상세 스트림 정보 관리

- Master / Media Playlist URL 관리
- 해상도별 스트림 상태 확인
- m3u8 저장 옵션 설정

### 동적 설정 관리

- 알람 조건, 재시도 간격, 타임아웃 설정
- 서버 재시작 없이 설정 즉시 반영

---

## 5. 기술 스택

### Backend

- Spring Boot
- Spring WebFlux
- WebClient
- Reactive Programming

### Message Queue

- RabbitMQ
  - Topic Exchange
  - TTL + DLX 기반 지연 처리
  - Partitioned Queue를 통한 병렬 처리

### Data Store / Cache

- MySQL
- Redis
  - 채널 상태
  - 설정 정보
  - 휘발성 데이터 캐싱

---

## 6. Quick Start

### Requirements

- Docker
- Docker Compose
- Java 21 (IDE에서 직접 실행 시 필요)

> **참고**: 모든 필수 구성 요소(Java, RabbitMQ, MySQL, Redis, FFmpeg)는 Docker Compose 설정에 포함되어 있어 `docker compose up`으로 실행 시 별도의 설치가 필요 없습니다.

---

## 7. FFmpeg/ffprobe 정보

`docker compose`를 사용하여 프로젝트를 실행하는 경우, **호스트(사용자 PC)에 FFmpeg를 설치할 필요가 없습니다.**

- `Dockerfile`은 애플리케이션 이미지 내에 `ffmpeg`와 `ffprobe`를 자동으로 포함하여 빌드합니다. (멀티 스테이지 빌드 사용)
- 모든 스트림 분석은 컨테이너 내부에서 실행됩니다.

### 로컬 개발 환경에서 직접 실행 시

IDE에서 Spring Boot 애플리케이션을 직접 실행하는 경우에만 아래와 같이 `ffmpeg`를 수동으로 설치해야 합니다.

#### macOS (Homebrew)
```bash
brew install ffmpeg
ffprobe -version
```

#### Ubuntu / Debian
```bash
sudo apt update
sudo apt install -y ffmpeg
ffprobe -version
```

#### Windows
1. FFmpeg 공식 사이트 접속: https://ffmpeg.org/download.html
2. Windows용 FFmpeg 다운로드 후 압축 해제
3. `ffmpeg/bin` 디렉토리를 **환경 변수 PATH**에 추가
4. 새 터미널에서 `ffprobe -version` 명령으로 설치를 확인합니다.

---

## 8. 실행 방법 (Docker Compose)

```bash
docker compose up -d --build
```

### 접근 주소

- **Web Dashboard**: http://localhost:8080
- **RabbitMQ Management UI**: http://localhost:15672

### 인프라 서비스만 실행 (로컬 개발용)

Spring Boot 애플리케이션을 IDE에서 직접 실행하려는 경우, 인프라 서비스(MySQL, RabbitMQ, Redis)만 Docker Compose로 실행할 수 있습니다.

```bash
docker compose -f compose-infra.yaml up -d
```

이후 IDE에서 Spring Boot 애플리케이션을 `prod` 또는 `dev` 프로파일로 실행하면 됩니다.

---

## 9. Configuration

### Slack 알림 설정 (선택)

#### 1) 환경 변수로 설정

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
```

#### 2) properties로 설정 (resources/slack.properties)

```properties
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
```

#### 3) yml로 설정

```yaml
slack:
  webhook-url: ${SLACK_WEBHOOK_URL:}
```

### 파일 저장 경로

```yaml
storage:
  m3u8-dir: ${APP_BASE_M3U8_DIR:/file/m3u8}
  ts-dir: ${APP_BASE_TS_DIR:/file/ts}
```

### ffprobe 실행 경로

#### 기본값 (권장)

```yaml
ffmpeg:
  ffprobe-path: ffprobe
```

#### 리눅스에서 절대경로로 지정하는 경우

```yaml
ffmpeg:
  ffprobe-path: /usr/bin/ffprobe
```

---

## 10. Profiles

### prod

- MySQL + RabbitMQ + Redis (Docker Compose)

### test

- H2 + Testcontainers (RabbitMQ / Redis)

---

## 11. 문서 및 참고 링크

- [시스템 아키텍처](https://www.notion.so/2c654232a5bd800a8253c57735ff2ab5?pvs=21)
- [RabbitMQ 구조](https://www.notion.so/2c654232a5bd8081aa37cd011a7b9381?pvs=21)
- [API 기본 명세서](https://www.notion.so/API-2c754232a5bd80718d3fda099f311474?pvs=21)
- [확인된 문제점 정리](https://www.notion.so/2cb54232a5bd808f9c04eec12db89d4a?pvs=21)

---

## License

이 프로젝트의 라이선스 정보를 여기에 추가하세요.

## Contributors

프로젝트 기여자 정보를 여기에 추가하세요.
