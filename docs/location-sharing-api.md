# Location Sharing API

## REST

### 위치 공유 On/Off

`PUT /api/v1/locations/share-status`

```json
{
  "coupleId": 1,
  "userId": 10,
  "enabled": true
}
```

### 위치 공유 상태 조회

`GET /api/v1/locations/couples/{coupleId}/users/{userId}/share-status`

### 현재 위치 저장

`PUT /api/v1/locations/current`

프론트는 좌표만 보내면 된다. 백엔드는 `KAKAO_REST_API_KEY`가 설정되어 있으면 카카오 Local API로 장소명과 주소명을 자동 변환해 저장한다.

```json
{
  "coupleId": 1,
  "userId": 10,
  "latitude": 37.2221,
  "longitude": 127.1875,
  "accuracyMeters": 20,
  "recordedAt": "2026-05-09T14:30:00"
}
```

위치 공유가 꺼져 있으면 `403 Forbidden`을 반환한다.

응답에는 `locationName`, `placeName`, `addressName`이 포함된다. `locationName`은 프론트 표시용 값이며 장소명이 있으면 장소명, 없으면 주소명을 사용한다.

```json
{
  "success": true,
  "data": {
    "coupleId": 1,
    "userId": 10,
    "latitude": 37.2221,
    "longitude": 127.1875,
    "locationName": "명지대학교 자연캠퍼스",
    "placeName": "명지대학교 자연캠퍼스",
    "addressName": "경기 용인시 처인구 명지로 116"
  },
  "message": ""
}
```

### 상대방 위치 조회

`GET /api/v1/locations/couples/{coupleId}/partners/{requesterId}`

상대방이 위치 공유를 켜지 않았거나 저장된 위치가 없으면 `shared: false`로 응답한다.

## WebSocket

STOMP endpoint: `/ws`

Publish:

`/pub/locations/current`

Subscribe:

`/sub/locations/couples/{coupleId}`

위치 공유가 켜진 사용자만 WebSocket으로 위치를 전송할 수 있다.

### 실시간 마커 갱신 주기

프론트는 위치 공유가 켜져 있고 지도 화면이 열려 있을 때 `5초마다` 현재 위치를 전송한다.

권장 정책:

- 기본 이동 중: 5초마다 전송
- 정지 상태: 15~30초마다 전송
- 위치 공유 Off: 전송 중단
- 앱 백그라운드 상태: 전송 중단 또는 OS 백그라운드 위치 권한 정책에 맞춰 별도 처리
- 이전 좌표와 10m 미만 차이: 전송 생략 가능

상대방 앱은 `/sub/locations/couples/{coupleId}`를 구독하고, 메시지를 받을 때마다 지도 마커 좌표를 새 위치로 애니메이션 이동시킨다.

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
