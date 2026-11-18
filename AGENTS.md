# FitLog V2 서버 개발 규칙

## 공식 문서와 작업 범위

1. [문서 관리 규칙](https://github.com/pby1360/fitlog-v2-docs/blob/main/AGENTS.md), [현재 상태](https://github.com/pby1360/fitlog-v2-docs/blob/main/status/CURRENT_STATUS.md), [백로그](https://github.com/pby1360/fitlog-v2-docs/blob/main/status/BACKLOG.md)를 읽는다. [기능 목록](https://github.com/pby1360/fitlog-v2-docs/blob/main/features/README.md)에서 해당 PRD·Tech Spec과 완료 조건, 관련 Issue·ADR을 찾는다.
2. 문서 저장소의 원격 `main`을 기준으로 확인한다. 같은 작업 환경에 문서 체크아웃이 있으면 원격 기준 커밋과 대조한다. 체크아웃이 없어도 공식 GitHub 링크를 사용하며, 기준 문서와 코드가 어긋나면 확인한 커밋과 근거를 기록한다.
3. 요구사항이나 공개 API 계약, 데이터 모델·마이그레이션 정책, 인증·보안, 장기 아키텍처를 변경해야 하면 영향과 대안, 호환성·검증 계획을 제시하고 해당 변경의 사용자 승인을 기다린다. 승인된 설계 안의 수정과 검증은 요청 범위에서 진행한다.
4. 승인된 설계 변경은 관련 공식 문서와 ADR에 연결한다. 공식 문서 저장소가 읽기 전용인 작업에서는 필요한 문서 갱신과 출처를 코드 PR에 기록한다. 구현 완료 표시는 실제로 확인한 동작과 검사 범위에 근거한다.

## 아키텍처 및 코딩 표준

- Java 21, Spring Boot 3.5, Gradle Wrapper를 사용한다. 기능 코드는 `src/main/java/com/fitlog/fitlogv2server/domain/`의 해당 패키지에 두고 공통 설정·보안·예외는 `global/`의 기존 구성에 맞춘다. [백엔드 설계](https://github.com/pby1360/fitlog-v2-docs/blob/main/architecture/BACKEND.md)
- 기존 Controller·DTO·Service·Repository와 해당 기능의 Facade를 따른다. 요청 DTO에서 입력을 검증하고, 서비스에서 도메인 규칙·소유권·쓰기 트랜잭션을 다룬다. 오류는 기존 HTTP 상태와 공통 오류 응답 형식을 유지한다. [API 계약](https://github.com/pby1360/fitlog-v2-docs/blob/main/api/README.md)
- 스키마가 바뀌도록 승인된 경우 `src/main/resources/db/migration/`에 새 Flyway 마이그레이션을 추가하고 기존 데이터의 경로를 확인한다. 이미 적용된 마이그레이션과 JPA의 `validate` 설정을 유지한다. 일지·통계 날짜는 기존 `AppTimeZone`의 KST 기준을 따른다. [데이터 모델](https://github.com/pby1360/fitlog-v2-docs/blob/main/architecture/DATA_MODEL.md)

## 테스트 및 검증

- 저장소 루트에서 Java 21과 비루트 실행 계정을 사용한다. `src/test/java`의 단위·통합 테스트를 확인한다. `IntegrationTestSupport`의 `test` 프로필은 내장 PostgreSQL과 Flyway를 사용한다. 테스트 설정은 `src/test/resources/application-test.yml`을 따른다.
- 기본 검증은 `./gradlew test --no-daemon`이다. 실패를 좁힐 때 `./gradlew test --tests '*WorkoutSessionIntegrationTest' --no-daemon`처럼 관련 클래스를 선택할 수 있다. 최종 검증은 필터 없이 전체를 실행한다. 빌드·패키징 변경에는 `./gradlew build --no-daemon`도 확인한다. 의존성이 캐시에 준비된 경우 `--offline`을 사용할 수 있다.
- `build/test-results/test/` 및 `build/reports/tests/test/`에서 실행 범위와 실패·건너뛴 테스트를 대조한다. 테스트 삭제·비활성화, `-x test`, 필터 적용으로 실패를 숨기지 않는다. 기본 브랜치에서 재현되는 기존 실패나 환경 차단은 근거와 함께 기록한다.
- [워크플로](.github/workflows/deploy.yml)는 `main`·`dev` 대상 PR에서 Gradle 테스트를 실행한다. 운영 DB·운영 계정·배포 환경에 접근하거나 배포를 실행하지 않는다. 테스트에는 로컬 데이터와 테스트 자격증명만 사용하고 비밀정보를 저장소·PR·로그에 노출하지 않는다.

## Git 및 PR

- 원격 `main`을 기준으로 저장소별 작업 브랜치를 사용한다. 기존 작업과 변경을 보존한다. 여러 저장소를 수정하는 요청에서는 검증과 Draft PR을 저장소마다 관리하고 의존하는 PR을 서로 연결한다.
- 코드 변경은 [`$fitlog-loop`](.agents/skills/fitlog-loop/SKILL.md)로 진행한다. 저장소 루트에서 호출해 해당 저장소의 지침과 검증 명령을 사용한다.
- 자체 검토 후 요청에 포함된 Draft PR을 만든다. 본문에 백로그 또는 Issue, 관련 PRD·Tech Spec, 변경 요약, 검증 명령·결과, 남은 항목을 기록한다. 배포 잡을 수동 실행하지 않는다.
- `main` 직접 수정과 푸시를 금지한다. 사용자 승인 없이 병합하지 않는다.
