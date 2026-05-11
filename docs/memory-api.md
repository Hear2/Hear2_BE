# Memory Album API

## Create Memory

```http
POST /api/v1/memories
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

Form data:

| Key | Type | Required | Description |
| --- | --- | --- | --- |
| `photo` | File | Yes | `jpg`, `jpeg`, or `png` memory photo. |
| `request` | JSON | Yes | Memory metadata. Set this part's content type to `application/json`. |

`request` example:

```json
{
  "memo": "직접 테스트",
  "takenAt": "2026-05-09T04:10:00",
  "latitude": 37.5445,
  "longitude": 127.0374
}
```

Notes:

- `coupleId` is resolved from the JWT access token. Clients must not send or trust couple IDs in the request body.
- `uploaderId` is resolved from the JWT access token. Clients must not send or trust uploader IDs in the request body.
- The authenticated user must have an active couple connection.
- `locationName` is optional. If it is omitted and coordinates are available, the server resolves `placeName` and `addressName` with Kakao Local API.
- If `takenAt`, `latitude`, or `longitude` are omitted, the server tries to read them from photo EXIF metadata.
- The server extracts only needed EXIF values before storage, then stores a sanitized image without EXIF metadata.
- AI tags are generated automatically when `OPENAI_API_KEY` is configured.

Response example:

```json
{
  "success": true,
  "data": {
    "id": 6,
    "coupleId": 1,
    "uploaderId": 1,
    "memo": "직접 테스트",
    "memoryDate": "2026-05-09",
    "photoUrl": "/api/v1/memories/items/6/photo",
    "metadata": {
      "takenAt": "2026-05-09T04:10:00",
      "latitude": 37.5445,
      "longitude": 127.0374,
      "locationName": "성동올레길",
      "placeName": "성동올레길",
      "addressName": "서울 성동구 성수동1가 720"
    },
    "tags": [
      {
        "tagName": "노트북",
        "confidence": 0.9000,
        "source": "AI"
      }
    ],
    "aiAnalysisStatus": "COMPLETED"
  },
  "message": ""
}
```

## Get Album

```http
GET /api/v1/memories
Authorization: Bearer {accessToken}
```

Returns all memories for the authenticated user's couple, ordered by memory date and creation time descending.

Use each response item's `id` as `memoryId` when calling detail, photo, update, or delete APIs.

## Get Memories By Date

```http
GET /api/v1/memories/dates/{memoryDate}
Authorization: Bearer {accessToken}
```

`memoryDate` format:

```text
yyyy-MM-dd
```

## Get Memory Detail

```http
GET /api/v1/memories/items/{memoryId}
Authorization: Bearer {accessToken}
```

## Get Memory Photo

```http
GET /api/v1/memories/items/{memoryId}/photo
Authorization: Bearer {accessToken}
```

Use this URL in the app image component. The API returns image bytes and does not expose the raw R2 object URL.

## Update Memory Memo

```http
PATCH /api/v1/memories/items/{memoryId}
Content-Type: application/json
Authorization: Bearer {accessToken}
```

```json
{
  "memo": "수정된 메모"
}
```

## Delete Memory

```http
DELETE /api/v1/memories/items/{memoryId}
Authorization: Bearer {accessToken}
```

Deletes the database record and the stored image object.

## Required Environment Variables

```text
DB_URL=
DB_USERNAME=
DB_PASSWORD=

MEMORY_STORAGE_TYPE=r2
R2_ACCESS_KEY=
R2_SECRET_KEY=
R2_ENDPOINT=
R2_BUCKET=
R2_REGION=auto

OPENAI_API_KEY=
KAKAO_REST_API_KEY=
```
