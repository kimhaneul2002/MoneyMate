# 서버 실행 흐름

MoneyMate 백엔드가 요청을 어떻게 처리하는지 단계별로 정리한 문서입니다.
서버가 켜질 때의 초기화 흐름과, 요청이 들어왔을 때의 처리 흐름(일반 CRUD / AI 분석)을
나눠서 설명합니다.

---

## 1. 서버 시작 시 (컴포넌트 초기화)

```
MoneymateApplication.java 실행
        │
        ▼
Spring Boot가 컴포넌트 스캔 시작
        │
        ├─ @Entity 클래스 발견 (Category, User, LedgerItem)
        │       → JPA(Hibernate)가 DB에 categories / user / ledger_items 테이블 자동 생성·갱신
        │
        ├─ Repository 인터페이스 발견 (LedgerRepository, CategoryRepository)
        │       → Spring Data JPA가 save() / findAll() / delete() 등 구현체를 자동 생성
        │
        └─ @Service, @RestController 클래스 발견
                → Spring 컨테이너에 빈(Bean)으로 등록
                → @Autowired로 서로 자동 연결 (의존성 주입)
        │
        ▼
Tomcat 내장 서버가 8080 포트로 기동 → 요청 대기 상태
```

실제로는 위 단계들이 "1번 끝나고 2번 시작" 순서로 눈에 보이게 진행되기보다,
Spring Boot가 애너테이션이 붙은 클래스들을 한 번에 스캔하고 내부적으로 의존관계를
정리하는 방식에 가깝습니다. 위 순서는 "어떤 게 어떤 것에 의존하는지"를 이해하기 위한
개념적인 흐름입니다.

---

## 2. 일반 CRUD 요청 흐름 (등록 / 조회 / 수정 / 삭제)

```
브라우저 (app.js의 fetch 요청)
        │
        ▼
LedgerController        요청 URL을 해당 메서드에 매핑, @Valid로 1차 검증
        │
        ▼
LedgerService            비즈니스 로직 처리 (합계 계산, 존재 여부 확인 등)
        │
        ▼
LedgerRepository          JPA가 SQL로 변환해 DB에 저장/조회/삭제 요청
        │
        ▼
MySQL                    실제 데이터 저장소
        │
        ▼
(역순으로) Entity → Repository → Service → Controller → JSON 응답
```

### 예시: 삭제 요청 1건의 실제 흐름

```
DELETE /api/ledger/1
  → LedgerController.deleteLedger()
  → LedgerService.deleteLedger()
      - ledgerRepository.findById(1) 로 존재 확인, 없으면 LedgerNotFoundException
      - ledgerRepository.deleteById(1) 로 실제 삭제
  → GlobalExceptionHandler 개입 없이 정상 처리되면 { "message": "내역 삭제 완료" } 반환
```

### 예시: 등록 요청 1건의 실제 흐름

```
POST /api/ledger  (body: title, amount, transactionDate, category)
  → LedgerController.addLedger()
      - @Valid가 LedgerItem의 @NotBlank, @Min, @NotNull 검증 먼저 수행
      - 검증 실패 시 GlobalExceptionHandler.handleValidationError() 가 400 응답
  → LedgerService.addLedger()
  → ledgerRepository.save() 로 INSERT
  → 저장된 LedgerItem(id, createdAt 등 채워짐)을 그대로 응답
```

---

## 3. AI 소비 분석 요청 흐름 (외부 API 연동이 추가되는 경로)

일반 CRUD와 달리, DB 조회에서 끝나지 않고 **외부 OpenAI 서버까지 한 번 더 왕복**합니다.

```
브라우저: GET /api/ledger/ai-analysis?year=2026&month=7
        │
        ▼
LedgerController.getAiAnalysis(year, month)
        │
        ▼
LedgerService.getAiAnalysis(year, month)
        │
        ├─ ① getLedgerByMonth(year, month) 호출
        │       → LedgerRepository로 DB에서 해당 월 거래 내역 조회 (일반 CRUD와 동일 경로)
        │
        ├─ 지출 내역이 없으면 → AI API 호출 없이 즉시 고정 안내 문구 반환
        │
        └─ ② aiAnalysisService.analyze(items) 호출
                │
                ▼
        AiAnalysisService
                │
                ├─ 거래 내역을 바탕으로 프롬프트 문자열 조립
                ├─ OpenAiService 객체 생성 (application.properties의 환경변수 API 키 사용)
                └─ OpenAI API(gpt-3.5-turbo-instruct)에 실제 네트워크 요청 전송
                        │
                        ▼
                OpenAI 서버가 분석 텍스트 생성 후 응답
                        │
                        ▼
        응답 텍스트를 그대로 LedgerService → LedgerController → 브라우저로 반환 (text/plain)
```

### CRUD 흐름과의 차이점

- DB 접근(Repository) 한 번, 외부 API 접근(AiAnalysisService → OpenAI) 한 번, 총 두 단계를 거칩니다.
- 외부 네트워크 호출이 포함되므로 응답 시간이 CRUD보다 오래 걸리고, 실패 가능성(키 오류,
  타임아웃 등)도 존재합니다. 현재는 별도 타임아웃/재시도 로직 없이 기본 설정을 사용 중입니다.
- 응답 형식이 JSON이 아니라 순수 텍스트(`text/plain`)입니다.
- `LedgerService`는 "DB에서 데이터 가져오기"까지만 책임지고, "가져온 데이터로 무엇을
  할지"는 `AiAnalysisService`에 위임합니다. 이렇게 나눈 이유는 [`REFACTORING.md`](../REFACTORING.md)를
  참고하세요.
