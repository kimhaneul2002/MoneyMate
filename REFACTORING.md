# 리팩토링 노트

MoneyMate 백엔드 기능 구현을 마친 뒤, 코드 품질을 개선하기 위해 진행한 리팩토링 기록입니다.
동작(API 응답)은 그대로 유지하면서 내부 구조만 개선했습니다.

---

## 1. AI 분석 로직 분리 — `AiAnalysisService` 신설

**Before**

`LedgerService` 안에 CRUD 로직과 OpenAI API 호출 로직(프롬프트 조립, API 키 관리, 응답 파싱)이
함께 섞여 있었습니다.

```java
@Service
public class LedgerService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    public String getAiAnalysis(int year, int month) {
        List<LedgerItem> items = getLedgerByMonth(year, month).getItems();
        // 프롬프트 조립 + OpenAiService 호출 로직 40여 줄이 이 안에 그대로 있었음
        ...
    }
}
```

**After**

AI 연동만 전담하는 `AiAnalysisService`를 새로 만들어 역할을 분리했습니다.

```java
// LedgerService.java — 이제 CRUD와 조율만 담당
@Autowired
private AiAnalysisService aiAnalysisService;

public String getAiAnalysis(int year, int month) {
    List<LedgerItem> items = getLedgerByMonth(year, month).getItems();
    return aiAnalysisService.analyze(items);
}
```

```java
// AiAnalysisService.java — OpenAI 연동만 담당 (신규)
@Service
public class AiAnalysisService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    public String analyze(List<LedgerItem> items) {
        if (items.isEmpty()) return "지출 내역이 없어 분석할 수 없습니다.";
        String prompt = buildPrompt(items);
        // OpenAI 호출 로직
        ...
    }
}
```

**왜 했는가**: `LedgerService`가 "가계부 CRUD"와 "외부 AI API 연동"이라는 서로 다른 책임을
동시에 지고 있었습니다. 관심사를 분리해두면, 나중에 AI 분석 기능(예: 주간 분석, 다른 모델로
교체 등)을 확장할 때 `AiAnalysisService`만 건드리면 되고, `LedgerService`의 CRUD 로직에는
영향이 없습니다.

컨트롤러(`LedgerController`)는 변경하지 않았습니다 — 여전히 `LedgerService.getAiAnalysis()`
하나만 호출하며, 그 내부에서 `AiAnalysisService`로 위임한다는 사실은 몰라도 됩니다.

---

## 2. 카테고리 타입 `String` → `CategoryType` enum

**Before**

```java
// Category.java
String type; // "INCOME" 또는 "EXPENSE" 문자열

// LedgerService.java
if (item.getCategory().getType().equals("INCOME")) { ... }
```

**After**

```java
// entity/CategoryType.java (신규)
public enum CategoryType {
    INCOME, EXPENSE
}

// Category.java
@Enumerated(EnumType.STRING) // DB에는 여전히 "INCOME"/"EXPENSE" 문자열로 저장 (기존 데이터 호환)
CategoryType type;

// LedgerService.java
if (item.getCategory().getType() == CategoryType.INCOME) { ... }
```

**왜 했는가**: 문자열 비교(`.equals("INCOME")`)는 오타가 나도 컴파일러가 잡아주지 못합니다.
실제로 리팩토링 도중 `.equals("INCOME")`를 enum 비교로 바꾸는 걸 한 군데 빠뜨렸다가
**총수입이 항상 0원으로 집계되는 버그**가 발생했는데, enum이었다면 애초에 타입이 안 맞아
컴파일 단계에서 바로 걸러졌을 문제였습니다. (자세한 원인은 [`docs/TROUBLESHOOTING.md`](./docs/TROUBLESHOOTING.md) 참고)

`@Enumerated(EnumType.STRING)`을 사용해 DB 컬럼은 여전히 문자열로 저장되므로, 기존에
등록된 카테고리 데이터를 마이그레이션할 필요는 없었습니다.

---

## 3. 예외 처리 세분화 — `LedgerNotFoundException` 신설

**Before**

`GlobalExceptionHandler`가 모든 `RuntimeException`을 뭉뚱그려 404로 응답했습니다.
"존재하지 않는 id 조회"든 "AI API 호출 실패" 같은 예상치 못한 서버 에러든 구분 없이
같은 404가 나가는 구조였습니다.

```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors); // 항상 404
}
```

**After**

`LedgerNotFoundException`을 새로 만들어 "id를 못 찾은 경우"만 정확히 잡아내고,
그 외의 예상치 못한 런타임 에러는 500으로 분리했습니다.

```java
// exception/LedgerNotFoundException.java (신규)
public class LedgerNotFoundException extends RuntimeException {
    public LedgerNotFoundException(String message) {
        super(message);
    }
}
```

```java
// GlobalExceptionHandler.java
@ExceptionHandler(LedgerNotFoundException.class)
public ResponseEntity<Map<String, String>> handleLedgerNotFound(LedgerNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors); // id 못 찾음 → 404
}

@ExceptionHandler(RuntimeException.class)
public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors); // 그 외 → 500
}
```

`LedgerService`의 `updateLedger`, `deleteLedger`에서 던지던 `RuntimeException`도
`LedgerNotFoundException`으로 교체했습니다.

**왜 했는가**: HTTP 상태 코드는 클라이언트(프론트엔드)가 에러 종류를 구분하는 신호입니다.
"없는 데이터를 요청함(404, 클라이언트 잘못일 수 있음)"과 "서버 내부에서 뭔가 잘못됨(500,
서버 문제)"을 구분해야 프론트에서도 각 상황에 맞는 안내를 보여줄 수 있습니다.

---

## 트러블슈팅: enum 전환 중 발생한 "총수입 0원" 버그

2번 리팩토링(enum 전환) 도중, `LedgerService`의 합계 계산 로직 한 곳에서
`.equals("INCOME")` 문자열 비교를 enum 비교로 바꾸는 걸 빠뜨려 총수입이 항상
0원으로 집계되는 버그가 있었습니다. 원인 분석과 해결 과정은
[`docs/TROUBLESHOOTING.md`](./docs/TROUBLESHOOTING.md)에 정리했습니다.

---

## 부가 변경: OpenAI API 키 환경변수 분리

`application.properties`에 평문으로 있던 OpenAI API 키를 환경변수 참조로 변경했습니다.

```properties
# Before
openai.api.key=sk-proj-실제키값...

# After
openai.api.key=${OPENAI_API_KEY}
```

실제 키 값은 IntelliJ의 Run Configuration → Environment variables에 `OPENAI_API_KEY`로
등록해 로컬에서만 보관하고, 소스 코드에는 남기지 않았습니다. GitHub에 이 프로젝트를
업로드하기 전 필수로 처리한 작업입니다.
