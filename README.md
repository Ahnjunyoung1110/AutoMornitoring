1. 프로젝트 개요

**HLS Stream Health Monitor (HSHM)**는
다수의 HLS(.m3u8) 라이브 스트림이 정상적으로 송출되고 있는지를 주기적으로 자동 검증하고,
장애 발생 시 이를 즉시 감지·전파하기 위한 실시간 모니터링 시스템입니다.

스트리밍 서비스 환경에서 수백 개에 달하는 라이브 채널을 사람이 직접 24시간 확인하는 것은 현실적으로 불가능합니다.
또한 장애 인지 지연은 곧바로 서비스 품질 저하와 사용자 신뢰 하락으로 이어집니다.

본 프로젝트는 이러한 문제를 해결하기 위해 다음을 목표로 설계되었습니다.

수백 개 이상의 라이브 HLS 채널 상태 자동 점검

장애 발생 시 즉각적인 알람 전송

전체 상태를 직관적으로 파악할 수 있는 대시보드 제공

향후 기능 확장을 고려한 구조적 설계

2. 프로젝트 필요성
1) 대규모 라이브 채널 운영의 한계

라이브 채널 수가 수백 개로 증가할 경우, 수동 점검은 사실상 불가능

장애 인지 지연 시 사용자 이탈 및 서비스 신뢰도 하락

2) SSAI 기반 광고 송출 검증 문제

회사 수익 구조가 SSAI(Server-Side Ad Insertion)에 의존

광고 노출 여부를 사람이 직접 확인하는 비효율적인 운영 방식

광고 구간의 m3u8을 자동으로 검증·저장할 수 있는 시스템 필요

3. 아키텍처 개요

본 시스템은 모듈러 모놀리식(Modular Monolith) 구조를 기반으로 설계되었습니다.

단일 애플리케이션이지만 도메인별로 명확히 분리된 모듈 구조

향후 MSA 전환을 고려한 경계 설계

모듈 간 비동기 통신은 RabbitMQ 사용

즉각적인 응답이 필요한 조회성 기능은 REST API 기반 처리

전체 처리 흐름은 다음과 같이 구성됩니다.

초기 등록 파이프라인

신규 모니터링 채널 등록

FFmpeg(ffprobe)를 이용한 초기 스트림 검증

DB 저장 후 모니터링 시작

주기적 모니터링 루프

m3u8 주기 파싱

상태 정보 수집

유효성 검증

스트림 상태 합당성 판단

오류 발생 시 알람 전송

지연 실행

TTL + DLX 기반 주기적 재실행

스케줄러 없이 분산된 주기 실행 구현

오류 처리 및 재시도

NACK 기반 재시도 큐

최종 실패 메시지는 Dead Queue로 분리

(상세 구조는 Mermaid 다이어그램 참고)

4. 주요 기능
1) 실시간 모니터링 대시보드

모든 채널의 상태를 실시간으로 시각화

정상 / 실패 / 중지 상태 구분

Trace ID, 채널명 기반 검색 및 정렬

2) 신규 모니터링 등록 및 제어

Manifest URL만으로 즉시 모니터링 시작

UI를 통한 모니터링 중지 및 수동 갱신

3) 상세 스트림 정보 관리

Master / Media Playlist URL 관리

해상도별 스트림 상태 확인

m3u8 저장 옵션 설정

4) 동적 설정 관리

알람 조건, 재시도 간격, 타임아웃 설정

서버 재시작 없이 설정 즉시 반영

5. 기술 스택
Backend

Spring Boot

Spring WebFlux

WebClient

Reactive Programming

Message Queue

RabbitMQ

Topic Exchange

TTL + DLX 기반 지연 처리

Partitioned Queue를 통한 병렬 처리

Data Store / Cache

MySQL

Redis

채널 상태

설정 정보

휘발성 데이터 캐싱

6. 비동기 아키텍처 설계 배경
문제 상황

초기에는 약 50개 채널 기준으로 문제가 없었으나,
100개 이상으로 확장되자 시스템이 주기적으로 멈추는 현상이 발생했습니다.

모니터링 결과 반영 지연

전체 처리 속도 급격히 저하

원인 분석

RabbitMQ Consumer 수는 제한적

Consumer 내부에서 동기 Blocking HTTP 호출(httpClient.send()) 사용

모든 Consumer가 외부 URL 응답 대기 상태에 빠지며 Consumer Block 발생

결과적으로:

5초 동안 약 60개 요청만 처리

360개 채널 1회 순회에 과도한 시간 소요

7. 해결 과정
1) Non-Blocking I/O 전환

기존 동기 HTTP 호출 제거

Spring WebFlux 기반 WebClient 도입

Netty Event Loop 기반 비동기 처리

2) Reactive 구조 정합성 확보

Reactive RabbitMQ Consumer 구조와 호환

@Async는 스레드 고갈 가능성으로 배제

8. 개선 결과

API 서버 스레드가 I/O 대기에서 완전히 해방

초당 약 200건 이상의 검사 요청 안정적 처리

360개 이상 채널을 지연 없이 순환 검사 가능

대규모 트래픽 환경에서 Blocking I/O의 한계와 비동기 아키텍처의 필요성을 명확히 체감

9. 문서 및 참고 링크

시스템 아키텍처
https://www.notion.so/2c654232a5bd800a8253c57735ff2ab5?pvs=21

RabbitMQ 구조
https://www.notion.so/2c654232a5bd8081aa37cd011a7b9381?pvs=21

API 기본 명세서
https://www.notion.so/API-2c754232a5bd80718d3fda099f311474?pvs=21

확인된 문제점 정리
https://www.notion.so/2cb54232a5bd808f9c04eec12db89d4a?pvs=21
