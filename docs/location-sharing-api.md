# Location Sharing API

프론트는 모든 요청에 `Authorization: Bearer <accessToken>`을 보낸다. `userId`와 `coupleId`는 요청으로 받지 않고, 백엔드가 JWT의 사용자 ID와 `couple_member` 기준으로 자동 찾는다.

## REST

### 위치 공유 ON/OFF

`PUT /api/v1/location/sharing`

```json
{
  "enabled": true
}
```

OFF로 변경하면 저장된 내 최신 위치도 즉시 삭제한다.

### 내 위치 공유 상태 조회

`GET /api/v1/location/sharing`

```json
{
  "success": true,
  "data": {
    "coupleId": 2,
    "userId": 7,
    "enabled": true,
    "updatedAt": "2026-05-13T12:00:00"
  },
  "message": ""
}
```

### 내 현재 위치 업로드

`POST /api/v1/location`

프론트는 OS 위치 권한으로 받은 좌표만 보내면 된다. 백엔드는 `KAKAO_REST_API_KEY`가 설정되어 있으면 카카오 Local API로 장소명과 주소명을 자동 변환해 최신 위치에 저장한다.

```json
{
  "lat": 37.2221,
  "lng": 127.1875,
  "accuracy": 20,
  "capturedAt": "2026-05-11T11:42:13Z"
}
```

위치 공유가 꺼져 있으면 `403 Forbidden`을 반환한다.

```json
{
  "success": true,
  "data": {
    "ok": true,
    "savedAt": "2026-05-13T12:00:00"
  },
  "message": ""
}
```

### 커플 위치 조회

`GET /api/v1/couple/location`

내 위치 공유가 꺼져 있으면 `403 Forbidden`을 반환한다. 상대방이 위치 공유를 꺼둔 경우 `partner`는 `null`이다. 위치가 5분 이상 갱신되지 않았으면 `stale: true`로 내려간다.

```json
{
  "success": true,
  "data": {
    "me": {
      "coupleId": 2,
      "userId": 7,
      "lat": 37.2221,
      "lng": 127.1875,
      "accuracy": 20,
      "locationName": "명지대학교 자연캠퍼스",
      "placeName": "명지대학교 자연캠퍼스",
      "addressName": "경기 용인시 처인구 명지로 116",
      "capturedAt": "2026-05-11T11:42:13",
      "updatedAt": "2026-05-13T12:00:00",
      "stale": false
    },
    "partner": null
  },
  "message": ""
}
```

## WebSocket

STOMP endpoint: `/ws`

Publish:

`/pub/locations/current`

Subscribe:

`/sub/locations/couples/{coupleId}`

WebSocket도 CONNECT 시 `Authorization: Bearer <accessToken>`을 보내야 한다. 백엔드는 구독하려는 `{coupleId}`가 JWT 사용자의 커플과 일치하는지 검증한다.

## 실시간 마커 갱신 주기

프론트는 위치 공유가 켜져 있고 지도 화면이 열려 있을 때 `5초마다` 현재 위치를 전송한다.

권장 정책:

- 기본 이동 중: 5초마다 전송
- 정지 상태: 15~30초마다 전송
- 위치 공유 OFF: 전송 중단
- 앱 백그라운드 상태: 전송 중단 또는 OS 백그라운드 위치 권한 정책에 맞춰 별도 처리
- 이전 좌표와 10m 미만 차이: 전송 생략 가능

상대방 앱은 REST MVP에서는 `GET /api/v1/couple/location`을 5초 간격으로 polling하고, WebSocket 연결 시에는 `/sub/locations/couples/{coupleId}` 메시지로 마커를 갱신한다.

백엔드는 위치 기록을 누적 저장하지 않고 사용자별 최신 위치만 갱신한다.

카카오 Local API 호출을 줄이기 위해 이전 위치와 50m 미만 차이면 기존 장소명/주소명을 재사용한다. 이 값은 `LOCATION_NAME_REFRESH_DISTANCE_METERS` 환경변수로 조정할 수 있다.

## Environment Variables

```text
KAKAO_REST_API_KEY=
KAKAO_LOCAL_RADIUS_METERS=1000
LOCATION_NAME_REFRESH_DISTANCE_METERS=50
```

## Privacy Rules

- 위치 공유 기본값은 꺼짐이다.
- 위치 공유가 꺼진 사용자의 위치는 상대방 조회 응답에 포함하지 않는다.
- 위치 공유를 끄면 저장된 최신 위치도 삭제한다.
- 위치 좌표, 주소, 장소명은 로그로 출력하지 않는다.
- 오래된 위치 기록을 누적 저장하지 않고 사용자별 최신 위치만 갱신한다.
