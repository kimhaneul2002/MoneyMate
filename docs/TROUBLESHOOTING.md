# 트러블슈팅

개발 중 겪었던 문제와 원인 분석, 해결 과정을 정리한 문서입니다.

---

## 1. Spring Boot 4.1.0 ↔ openai-java 0.18.2 Jackson 버전 충돌

### 증상

AI 분석 API(`/api/ledger/ai-analysis`)를 호출하면 500 에러가 발생했고, 서버 콘솔에는
아래 예외가 찍혔습니다.

```
java.lang.NoSuchFieldError: SNAKE_CASE
	at com.theokanning.openai.service.OpenAiService.defaultObjectMapper(OpenAiService.java:584)
	at com.theokanning.openai.service.OpenAiService.<clinit>(OpenAiService.java:73)
```

### 원인

- `openai-java 0.18.2`는 2023년 11월 릴리즈된 이후 업데이트가 없는 라이브러리로, 내부적으로
  `com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE`라는 정적 필드를
  참조합니다.
- Jackson 2.17부터 `PropertyNamingStrategy`의 정적 필드들이 제거되고 `PropertyNamingStrategies`
  클래스로 대체되었습니다.
- Spring Boot 4.1.0은 Jackson 3(`tools.jackson`, 새 그룹 ID)을 기본 JSON 라이브러리로
  관리하며, Jackson 2 계열 라이브러리를 위한 버전 관리 프로퍼티명도 기존 `jackson.version`이
  아니라 `jackson-2-bom.version`으로 바뀌어 있었습니다.
- 처음에는 이 사실을 모른 채 존재하지 않는 `jackson.version` 프로퍼티를 낮은 버전으로
  지정했는데, Spring Boot 4.1.0에서 인식하지 못하는 이름이라 계속 무시되었습니다.
- 이후 `jackson-2-bom.version`으로 프로퍼티명을 바로잡아 낮췄더니, 이번에는 `jackson-databind`,
  `jackson-core`뿐 아니라 `jackson-annotations`까지 통째로 낮은 버전으로 끌려 내려가면서,
  Spring Boot 4 자체가 요구하는 Jackson 3 초기화(`JacksonAnnotationIntrospector`)가
  `jackson-annotations`의 최신 클래스(`JsonSerializeAs`)를 찾지 못해 서버 자체가
  기동에 실패하는 2차 문제가 발생했습니다.

```
Caused by: java.lang.NoClassDefFoundError: com/fasterxml/jackson/annotation/JsonSerializeAs
	at tools.jackson.databind.introspect.JacksonAnnotationIntrospector.<clinit>(...)
```

### 해결

`pom.xml`에 `dependencyManagement`로 `jackson-databind`, `jackson-core` **두 라이브러리만**
낮은 버전(2.16.1)으로 정확히 지정하고, `jackson-annotations`는 Spring Boot가 관리하는
버전을 그대로 두었습니다.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.1</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>2.16.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

이렇게 하면 `openai-java`가 필요로 하는 낮은 버전의 `jackson-databind`/`jackson-core`와,
Spring Boot 4가 필요로 하는 높은 버전의 `jackson-annotations`를 동시에 만족시킬 수
있었습니다.

### 교훈

- 서드파티 라이브러리가 오래 업데이트되지 않았다면, 최신 프레임워크와의 버전 궁합을
  먼저 의심해볼 필요가 있습니다.
- 프레임워크 메이저 버전이 올라가면(Spring Boot 3 → 4) 내부적으로 관리하는 라이브러리
  생태계 자체가 바뀔 수 있으므로(Jackson 2 → 3), 버전을 강제로 낮출 때는 "무엇을 얼마나
  좁게 지정하는지"가 중요합니다. 범위를 너무 넓게 잡으면(예: 전체 Jackson 계열을 한
  프로퍼티로 통일) 의도치 않은 다른 충돌을 유발할 수 있습니다.

---

## 2. `CategoryType` enum 전환 중 발생한 "총수입 0원" 버그

### 증상

`Category`의 `type` 필드를 `String`에서 `CategoryType`(enum)으로 리팩토링한 뒤,
월별 조회 화면에서 총수입이 항상 0원으로 표시되고 모든 금액이 총지출에 합산되었습니다.

### 원인

리팩토링 과정에서 여러 파일을 순서대로 고치다가, `LedgerService.getLedgerByMonth()`의
합계 계산 로직 한 곳을 놓쳤습니다.

```java
// Category.getType()의 반환 타입은 이미 CategoryType(enum)으로 바뀐 상태
if (item.getCategory().getType().equals("INCOME")) { // 그런데 비교는 여전히 String과
    totalIncome += item.getAmount();
} else {
    totalExpense += item.getAmount();
}
```

`CategoryType`과 `String`은 서로 다른 타입이라 `.equals()` 비교가 항상 `false`를
반환했고, 그 결과 모든 거래가 예외 없이 `else`(지출) 쪽으로 계산되었습니다.

이 버그는 **컴파일 에러 없이 조용히 발생**했습니다. `.equals(Object)` 메서드 자체는
어떤 타입과 비교해도 문법적으로 유효한 코드이기 때문에, IDE도 컴파일러도 문제를
잡아내지 못했고 실제로 화면에서 결과를 확인하기 전까지는 알아챌 수 없었습니다.

### 해결

문자열 비교를 enum 비교로 수정했습니다.

```java
if (item.getCategory().getType() == CategoryType.INCOME) {
    totalIncome += item.getAmount();
} else {
    totalExpense += item.getAmount();
}
```

### 교훈

이 버그는 역설적으로 애초에 `String` 대신 `enum`으로 리팩토링하려 했던 이유를
그대로 보여주는 사례가 되었습니다. 문자열 비교는 오타나 타입 불일치가 있어도 컴파일러가
잡아주지 못하고 실행 후에야 드러나는 반면, 만약 모든 비교 지점이 처음부터 `==` enum
비교로 통일되어 있었다면 애초에 타입이 안 맞는 순간 컴파일 에러로 즉시 드러났을
문제였습니다. 리팩토링을 여러 파일에 걸쳐 진행할 때는, 변경 대상 타입을 사용하는
**모든 지점**을 검색(`grep`, IDE의 "사용처 찾기")으로 한 번 더 확인하는 습관이
필요하다는 걸 확인한 경험이었습니다.

---

관련 코드 변경 내역은 [`REFACTORING.md`](../REFACTORING.md)를 참고하세요.
