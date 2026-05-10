\# Hear2 ERD 기준 문서



이 문서는 Hear2 백엔드 개발 시 사용하는 ERD 기준 문서이다.  

Codex 및 백엔드 개발자는 Entity, DTO, Repository, Service, Controller 생성 시 이 문서의 테이블명, 컬럼명, 타입, PK/FK 기준을 우선 따른다.



\## 공통 규칙



\- DB 컬럼명은 snake\_case 사용

\- Java 변수명은 camelCase 사용

\- ERD에 없는 필드는 임의 생성 금지

\- 필요한 필드가 문서에 없으면 TODO 주석으로 표시

\- 아래 Type은 DB 타입 기준

\- Entity 생성 시 DB Type을 Java Type으로 변환해서 사용



\## Java 타입 변환 규칙



| DB Type | Java Type |

|---|---|

| BIGINT | Long |

| INT | Integer |

| VARCHAR | String |

| TEXT | String |

| DOUBLE | Double |

| BOOLEAN | Boolean |

| DATETIME | LocalDateTime |

| DATE | LocalDate |



\---



\# USER



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| user\_id | userId | BIGINT | PK |

| email | email | VARCHAR |  |

| password | password | VARCHAR |  |

| nickname | nickname | VARCHAR |  |

| profile\_image | profileImage | VARCHAR |  |

| provider | provider | VARCHAR |  |

| created\_at | createdAt | DATETIME |  |



\---



\# COUPLE



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| couple\_id | coupleId | BIGINT | PK |

| couple\_code | coupleCode | VARCHAR |  |

| start\_date | startDate | DATE |  |

| created\_at | createdAt | DATETIME |  |



\---



\# COUPLE\_MEMBER



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| couple\_member\_id | coupleMemberId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| user\_id | userId | BIGINT | FK |

| role | role | VARCHAR |  |

| joined\_at | joinedAt | DATETIME |  |



\---



\# CHAT\_MESSAGE



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| message\_id | messageId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| sender\_id | senderId | BIGINT | FK |

| content | content | TEXT |  |

| message\_type | messageType | VARCHAR |  |

| sent\_at | sentAt | DATETIME |  |



\---



\# EMOTION\_ANALYSIS



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| analysis\_id | analysisId | BIGINT | PK |

| message\_id | messageId | BIGINT | FK |

| emotion\_type | emotionType | VARCHAR |  |

| emotion\_score | emotionScore | DOUBLE |  |

| risk\_detected | riskDetected | BOOLEAN |  |

| analyzed\_at | analyzedAt | DATETIME |  |



\---



\# DAILY\_QUESTION



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| question\_id | questionId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| question | question | TEXT |  |

| question\_date | questionDate | DATE |  |



\---



\# DAILY\_ANSWER



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| answer\_id | answerId | BIGINT | PK |

| question\_id | questionId | BIGINT | FK |

| user\_id | userId | BIGINT | FK |

| answer | answer | TEXT |  |

| answered\_at | answeredAt | DATETIME |  |



\---



\# COUPLE\_SCHEDULE



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| schedule\_id | scheduleId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| title | title | VARCHAR |  |

| start\_time | startTime | DATETIME |  |

| end\_time | endTime | DATETIME |  |

| schedule\_type | scheduleType | VARCHAR |  |



\---



\# ANNIVERSARY



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| anniversary\_id | anniversaryId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| name | name | VARCHAR |  |

| anniversary\_date | anniversaryDate | DATE |  |

| repeat\_yearly | repeatYearly | BOOLEAN |  |



\---



\# AI\_REPORT



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| report\_id | reportId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| report\_type | reportType | VARCHAR |  |

| period\_start | periodStart | DATE |  |

| period\_end | periodEnd | DATE |  |

| summary | summary | TEXT |  |

| relationship\_score | relationshipScore | DOUBLE |  |



\---



\# COUPLE\_DNA



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| dna\_id | dnaId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| communication\_type | communicationType | VARCHAR |  |

| love\_language | loveLanguage | VARCHAR |  |

| compatibility\_score | compatibilityScore | DOUBLE |  |



\---



\# TIME\_CAPSULE



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| capsule\_id | capsuleId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| content | content | TEXT |  |

| title | title | VARCHAR |  |

| open\_date | openDate | DATE |  |

| is\_opened | isOpened | BOOLEAN |  |



\---



\# COUPLE\_CHARACTER



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| character\_id | characterId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| name | name | VARCHAR |  |

| level | level | INT |  |

| exp | exp | INT |  |

| evolution\_stage | evolutionStage | VARCHAR |  |



\---



\# MEMORY



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| memory\_id | memoryId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| photo\_url | photoUrl | VARCHAR |  |

| title | title | VARCHAR |  |

| memo | memo | TEXT |  |

| memory\_date | memoryDate | DATETIME |  |

| created\_at | createdAt | DATETIME |  |



\---



\# LOCATION\_SHARE



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| location\_id | locationId | BIGINT | PK |

| user\_id | userId | BIGINT | FK |

| latitude | latitude | DOUBLE |  |

| longitude | longitude | DOUBLE |  |

| is\_sharing | isSharing | BOOLEAN |  |

| updated\_at | updatedAt | DATETIME |  |



\---



\# AI\_JUDGE\_RESULT



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| judge\_id | judgeId | BIGINT | PK |

| couple\_id | coupleId | BIGINT | FK |

| conflict\_summary | conflictSummary | TEXT |  |

| verdict | verdict | TEXT |  |

| reconciliation\_advice | reconciliationAdvice | TEXT |  |

| created\_at | createdAt | DATETIME |  |



\---



\# PHOTO\_METADATA



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| metadata\_id | metadataId | BIGINT | PK |

| memory\_id | memoryId | BIGINT | FK |

| latitude | latitude | DOUBLE |  |

| longitude | longitude | DOUBLE |  |

| location\_name | locationName | VARCHAR |  |

| taken\_at | takenAt | DATETIME |  |



\---



\# MEMORY\_TAG



| DB column | Java field | DB Type | Key |

|---|---|---|---|

| tag\_id | tagId | BIGINT | PK |

| memory\_id | memoryId | BIGINT | FK |

| tag\_name | tagName | VARCHAR |  |

| confidence | confidence | DOUBLE |  |

