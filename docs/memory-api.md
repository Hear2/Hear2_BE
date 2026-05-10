# Memory Album API

## Create Memory

```http
POST /api/v1/memories
Content-Type: multipart/form-data
```

Form data:

| Key | Type | Required | Description |
| --- | --- | --- | --- |
| `photo` | File | Yes | `jpg`, `jpeg`, or `png` memory photo. |
| `request` | JSON | Yes | Memory metadata. Set this part's content type to `application/json`. |

`request` example:

```json
{
  "coupleId": 1,
  "uploaderId": 1,
  "memo": "직접 테스트",
  "takenAt": "2026-05-09T04:10:00",
  "latitude": 37.5445,
  "longitude": 127.0374
}
```

Notes:

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
    "photoUrl": "/api/v1/memories/couples/1/items/6/photo",
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
GET /api/v1/memories/couples/{coupleId}
```

Returns all memories for a couple, ordered by memory date and creation time descending.

## Get Memories By Date

```http
GET /api/v1/memories/couples/{coupleId}/dates/{memoryDate}
```

`memoryDate` format:

```text
yyyy-MM-dd
```

## Get Memory Detail

```http
GET /api/v1/memories/couples/{coupleId}/items/{memoryId}
```

## Get Memory Photo

```http
GET /api/v1/memories/couples/{coupleId}/items/{memoryId}/photo
```

Use this URL in the app image component. The API returns image bytes and does not expose the raw R2 object URL.

## Update Memory Memo

```http
PATCH /api/v1/memories/couples/{coupleId}/items/{memoryId}
Content-Type: application/json
```

```json
{
  "memo": "수정된 메모"
}
```

## Delete Memory

```http
DELETE /api/v1/memories/couples/{coupleId}/items/{memoryId}
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
