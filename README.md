# Autumn — E-Learning Platform API

Spring Boot REST API behind **Autumn**, an online learning platform where instructors build courses
(video lessons, blog-style lesson pages, quizzes) and students learn, track progress and earn
verifiable certificates. The UI is available in **English and Khmer**.

- Frontend (React + Vite): [visal-sak/Learning-Frontend](https://github.com/visal-sak/Learning-Frontend)
- Base URL: `http://localhost:1444/api/v1`
- API docs: `http://localhost:1444/swagger-ui.html` and `http://localhost:1444/scalar`

---

## Features

| Area | What it does |
|---|---|
| **Accounts** | Registration through Keycloak, my profile, public profiles, password change via the Keycloak account console |
| **Categories** | Public list; admins create, edit and delete |
| **Courses** | Catalog search (keyword, category, level, paging), course page, drafts, review workflow: `DRAFT → PENDING_REVIEW → PUBLISHED / REJECTED → ARCHIVED` |
| **Curriculum** | Sections and items in order. An item is a **lesson** (video, intro text, files, blog-style blocks) or a **quiz**, placed anywhere in a section |
| **Lesson blocks** | Headings, paragraphs, tip/note/warning callouts, lists, quotes, tables, images and code examples with HTML/CSS/JS parts for the in-browser "Try it Yourself" editor |
| **Media** | Upload images, videos and files; lesson videos and files are served through short-lived signed URLs |
| **Enrollment & progress** | Free enrollment, sequential unlocking, items-based progress, completion |
| **Lesson quizzes** | Quick checks after a lesson or section reviews: graded on the server, optional pass mark, answers revealed only after submitting |
| **Final quiz** | One per course: single choice, multiple choice, true/false and short answer; attempts, time limit, pass mark |
| **Certificates** | Issued automatically when every lesson and quiz is complete and (if required) the final quiz is passed. Public verification by serial `LN-YYYY-XXXXXXXX` |
| **Course settings** | Enable certificate, require final quiz for the certificate, require passing quiz items to continue |
| **Learning tools** | Timestamped lesson notes, lesson bookmarks, AI lesson tutor (Claude) answering in English or Khmer |
| **Reviews & moderation** | Ratings and comments after starting a course; users report reviews, admins work a moderation queue |
| **Teaching insights** | Students per course, quiz assessments (pass rate, score spread, hardest questions), certificate tracking, dashboard reports |
| **Khmer messages** | Error messages are returned in Khmer when the request sends `Accept-Language: km` |

---

## Roles

Roles come from Keycloak realm roles in the access token (`realm_access.roles`).

| Role | Can do |
|---|---|
| **Guest** (no login) | Browse catalog and course pages, watch free-preview lessons, read reviews, verify certificates |
| **User** (`USER`, default) | Everything above, plus enroll, notes, bookmarks, report reviews, profile |
| **Student** (`STUDENT`, or any user enrolled in a course) | Lessons, quizzes, progress, reviews, certificate for that course |
| **Instructor** (`INSTRUCTOR`) | Create and manage **their own** courses, curriculum, quizzes; see their students, results and certificates |
| **Admin** (`ADMIN`) | Everything on every course, plus review queue, categories, moderation, platform reports |

Two layers of checks: `SecurityConfig` and `@PreAuthorize` check the **role**, and
`CourseAccessService` checks **ownership** (instructor ↔ course) and **enrollment** (student ↔ course).

---

## Tech stack

- Java 25, Spring Boot 4.1 (Web MVC, Security, Data JPA, Validation), virtual threads
- PostgreSQL, Hibernate (`ddl-auto=update`)
- Keycloak (OAuth2 resource server with JWT; admin client for registration)
- MapStruct, Lombok
- springdoc-openapi (Swagger UI + Scalar)
- Anthropic Java SDK (AI lesson tutor)
- Gradle

---

## Getting started

### Prerequisites

| Service | Default |
|---|---|
| JDK | 25 |
| PostgreSQL | `localhost:1400`, database `postgres` |
| Keycloak | `localhost:1401`, realm `elearning`, roles `USER`, `STUDENT`, `INSTRUCTOR`, `ADMIN`, a confidential client for the admin API (`private-app`) and a public client for the frontend |

### Configure

Settings live in `src/main/resources/application.properties`. Keep secrets out of git: use
environment variables (Spring reads `${ENV_NAME:default}`).

| Property | Purpose |
|---|---|
| `server.port` | API port (`1444`) |
| `spring.datasource.url` / `username` / `password` | PostgreSQL connection |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` / `jwk-set-uri` | Keycloak realm that signs tokens |
| `keycloak.server-url` / `realm` / `client-id` / `client-secret` | Keycloak admin client (user registration) |
| `app.cors.allowed-origins` | Frontend origins, e.g. `http://localhost:5173` |
| `media.location` / `media.client-path` / `media.base-uri` | Where uploads are stored and how they are served (`/media/**`) |
| `app.storage.*` | Lesson file storage and the key that signs video/file URLs |
| `anthropic.api-key` | **Set `ANTHROPIC_API_KEY` in the environment** to turn on the AI tutor (off when empty) |
| `anthropic.model` | Model for the tutor (`claude-opus-5-5`) |

Example (PowerShell):

```powershell
$env:ANTHROPIC_API_KEY = "sk-ant-..."   # optional, enables the AI tutor
```

### Run

```bash
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

or run `ELearningApplication` from IntelliJ. Tables are created and updated automatically on start.

### Build

```bash
./gradlew build
java -jar build/libs/e-learning-*.jar
```

---

## API overview

All paths are under `/api/v1`. Full request and response schemas are in Swagger / Scalar.

| Feature | Main endpoints |
|---|---|
| Auth & users | `POST /auth/register` · `GET/PUT /users/me` · `GET /users/{id}/profile` |
| Categories | `GET /categories` · `POST/PUT/DELETE /categories/{id}` (admin) |
| Courses | `GET /courses` · `GET /courses/{id}` · `GET /courses/mine` · `POST /courses` · `PUT /courses/{id}` · `PATCH /courses/{id}/status` · `GET /courses/pending-review` |
| Curriculum | `GET /lessons/{id}` · `POST/PUT/DELETE /sections` · `POST/PUT/DELETE /lessons` · `PATCH …/order` · `DELETE /lesson-files/{id}` |
| Enrollment & progress | `POST/DELETE /courses/{id}/enroll` · `GET /my/enrollments` · `POST/DELETE /lessons/{id}/complete` · `GET /courses/{id}/students` |
| Lesson quizzes | `GET/POST /lessons/{id}/check` · `GET /lessons/{id}/check/edit` · `PUT /lessons/{id}/check` |
| Final quiz | `GET /courses/{id}/quiz/take` · `POST /quizzes/{id}/attempts` · `GET /quizzes/{id}/my-attempts` · quiz and question management under `/quizzes`, `/questions` |
| Assessments | `GET /assessments` · `GET /assessments/{courseId}` |
| Certificates | `GET /certificates/{serial}` (public) · `GET /my/certificates` · `POST /my/certificates/{courseId}` · `GET /manage/certificates` |
| Reviews | `GET /courses/{id}/reviews` · `GET /courses/{id}/reviews/summary` · `PUT/DELETE /courses/{id}/reviews/mine` · `GET /my/reviews` |
| Moderation | `POST /reviews/{id}/report` · `GET /moderation/reviews` · `POST /moderation/reviews/{id}/dismiss` · `DELETE /moderation/reviews/{id}` |
| Media | `POST /medias` · `POST /medias/multiples` · `GET /medias` · `DELETE /medias/{name}` |
| Notes & bookmarks | `/my/notes` · `/my/bookmarks/{lessonId}` |
| AI tutor | `GET /assistant/status` · `POST /lessons/{id}/assistant` |
| Reports | `GET /reports/platform` · `/reports/my-teaching` · `/reports/top-courses` · `/reports/top-rated` |

Errors use one shape:

```json
{ "timestamp": "…", "status": 409, "error": "Conflict", "message": "Pass the quiz to continue", "path": "/api/v1/lessons/…/complete" }
```

---

## Project structure

```
src/main/java/co/istad/visal/e_learning
├── auth/          registration
├── config/        CORS, JPA, OpenAPI, media resource handler
├── expection/     error handling and Khmer message translation
├── security/      SecurityConfig, JWT role converter, course access checks, Keycloak clients
├── utils/
└── features/
    ├── assessment/    quiz results for teaching staff
    ├── assistant/     AI lesson tutor
    ├── bookmark/
    ├── category/
    ├── certificate/   issuing, verification, tracking
    ├── content/       sections, lessons, lesson blocks, files
    ├── course/        courses, publishing workflow, settings
    ├── enrollment/    enrollment and progress
    ├── lessoncheck/   lesson and section quizzes
    ├── media/
    ├── moderation/    reported reviews
    ├── note/          timestamped notes
    ├── quiz/          final course quiz
    ├── report/        dashboard numbers
    ├── review/
    └── user/
```

Each feature keeps its entity, repository, service, controller and `dto/` package together.

---

## Notes

- Uploaded media is stored on disk at `media.location` and served at `/media/**`.
- Lesson videos and files are never exposed as permanent links: `GET /lessons/{id}` returns signed URLs that expire after 60 seconds.
- The AI tutor is rate limited (20 questions per 10 minutes per user) and only answers about lessons the user can access.
