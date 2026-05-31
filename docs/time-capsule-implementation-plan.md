# Time Capsule Implementation Plan

## Scope

- Implement couple time capsules with sealed/open status.
- Store letters and photo object keys.
- Exclude voice messages for the first version.
- Keep BGM recommendation out of the first implementation.

## Package Structure

- `capsule/controller`: REST API surface.
- `capsule/dto`: request and response contracts for Swagger and frontend integration.
- `capsule/entity`: JPA model for capsule, letter, media, and snapshot.
- `capsule/repository`: persistence boundaries.
- `capsule/service`: business rules, snapshot calculation, media access, notification orchestration.
- `capsule/scheduler`: openAt based automatic open job.

## Implementation Order

1. Create capsule with JWT user and couple resolution.
2. Store sealedAt, openAt, coverStyle, options, letter, and photo object keys.
3. Save capsule snapshot at sealing time.
4. List sealed/open capsules by couple.
5. Return sealed detail without letter/photo signed URLs.
6. Return open detail with signed photo URLs and then-vs-now comparison.
7. Add scheduler to open due capsules.
8. Expose openAt so the calendar feature can display upcoming capsule open dates.
9. Add FCM notifications for opened capsules and D-7 reminders.

## Current Implementation

- `POST /api/v1/capsule`: creates a capsule for the authenticated user's couple.
- `GET /api/v1/capsule?status=sealed|open|all`: lists the authenticated user's couple capsules.
- `GET /api/v1/capsule/{capsuleId}`: hides letter/photos/then-vs-now while sealed and returns them after open.
- `GET /api/v1/capsule/{capsuleId}/share-card`: returns Instagram Story share card metadata for opened capsules.
- Scheduler opens due capsules every 60 seconds by default.
- FCM open and D-7 notifications are intentionally postponed.

## Calendar Integration Note

- The memory album does not automatically copy opened capsule photos.
- The future calendar feature should use `openAt` from capsule list/detail responses to show upcoming capsule open dates.
- If the calendar needs a unified event API later, expose capsule open dates as calendar events instead of saving them as memories.

## Instagram Story Share Note

- Backend does not open Instagram directly.
- Backend returns share-card metadata for opened capsules.
- Frontend should render a 9:16 story card from `shareCard` and open the Instagram Stories share flow.
- Sealed capsules cannot be shared.

## AI

- No AI is required for the first time capsule version.
- BGM recommendation can later use FastAPI/OpenAI, but it is intentionally out of scope now.

## Environment Variables

- `MEDIA_STORAGE_TYPE=r2`
- `R2_ACCESS_KEY`
- `R2_SECRET_KEY`
- `R2_ENDPOINT`
- `R2_BUCKET`
- `R2_REGION=auto`
- `MEDIA_PRESIGNED_UPLOAD_EXPIRATION_MINUTES`
- `MEDIA_PRESIGNED_READ_EXPIRATION_MINUTES`
- `TIME_CAPSULE_OPEN_SCHEDULER_DELAY_MS` (optional, default `60000`)
- `FCM_ENABLED=true`, `FCM_CREDENTIALS_PATH`, and `FCM_PROJECT_ID` when notifications are implemented.
