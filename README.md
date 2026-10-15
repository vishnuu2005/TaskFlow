# TaskFlow — Secure Task & Learning Management System

A full-stack **Task & Learning Management System (LMS)** with role-based access control (ADMIN, TEACHER, STUDENT), Spring Boot 3 backend (JWT auth, MySQL/H2), and a dark glassmorphism responsive frontend.

---

## ✨ Features

### 🔐 Multi-Role Security & Accounts (RBAC)
- **ADMIN**: User account management (activate/deactivate, role adjustment, detail updates), course oversight, category management, platform-wide announcements, and platform analytics.
- **TEACHER**: Course creation & management, student enrollment, assignment publishing (instructions, due dates, points, attachments), student submission review, score grading, and constructive feedback.
- **STUDENT**: Personal task planner (with categories, priorities, due dates, and subtasks checklist), class course enrollment, assignment work submissions, teacher grade & feedback reviews, and class announcements.
- **Zero-Trust Backend Authorization**: Server-side permission enforcement on every protected endpoint and service, preventing Insecure Direct Object References (IDOR).
- **BCrypt Password Hashing** (Strength 12) & **JWT Auth** (HMAC-SHA256 with role claims).

### 📚 Learning Management System (LMS)
- **Courses & Classes**: Unique course codes, course descriptions, instructor attribution, student rosters, and progress metrics.
- **Assignments & Submissions**: Points-based assignments with deadlines, priority tags, and file attachments. Students submit project work and notes; teachers review and grade with comments.
- **Calendar & Agenda**: Unified timeline displaying both personal task deadlines and class assignment deliverables.
- **In-App Notifications**: Real-time notification bell with unread counters for new assignments, approaching deadlines, grade feedback, and announcements.
- **Scheduled Due Reminders**: Automated hourly background check notifying students of assignments and personal tasks due the next day.

### 🎨 Responsive Glassmorphism UI
- Dark glass aesthetics with blurred glass cards, glowing animated orbs, responsive layouts for mobile and desktop, interactive modals, and real-time live statistics.

---

## 👥 Default Demo Credentials (Local Development)

| Role | Email | Password | Permissions |
|------|-------|----------|-------------|
| **ADMIN** | `admin@taskflow.com` | `AdminPassword123!` | Platform analytics, user roles, system categories, sitewide announcements |
| **TEACHER** | `teacher@taskflow.com` | `TeacherPassword123!` | Create & manage classes, assign coursework, grade submissions |
| **STUDENT** | `student@taskflow.com` | `StudentPassword123!` | Personal to-do tasks, enroll in classes, submit assignments, view grades |

> [!NOTE]
> Public registration as `ADMIN` is strictly forbidden. The initial administrator account is safely seeded upon first startup if no administrator exists.

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| **Frontend** | HTML5, Vanilla CSS (Glassmorphism), Vanilla JavaScript (SPA) |
| **Backend** | Java 21, Spring Boot 3.5.7, Spring Security, Spring Data JPA |
| **Database** | MySQL 8 (Production profile) / H2 In-Memory (Dev profile) |
| **Auth** | JWT (`jjwt 0.12.6`), BCrypt Password Encoder |
| **Testing** | JUnit 5, Mockito, Spring Security Test, Spring Boot Test (`MockMvc`) |

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven wrapper (`./mvnw` or `mvnw.cmd` included)
- MySQL 8 (optional; H2 in-memory mode works out of the box)

### Zero-Config Quick Start (Dev Profile with H2)

**Fastest (Windows):**
```cmd
run.bat
```
*(or `./run.ps1` in PowerShell)*

**Cross-Platform (Maven Wrapper):**
```bash
cd backend
./mvnw spring-boot:run
```
*(On Windows: `mvnw.cmd spring-boot:run`)*


Once started:
- 🌐 **Web Application:** [http://localhost:8081/](http://localhost:8081/)
- 🗄️ **H2 Database Console:** [http://localhost:8081/h2-console](http://localhost:8081/h2-console) (JDBC URL: `jdbc:h2:mem:taskflow_db`, User: `sa`, Password: blank)
- 🧪 **Run Test Suite:** `./mvnw test`

### Running with MySQL (Production Profile)

Create the database:
```sql
CREATE DATABASE taskflow_db;
```

Run with MySQL environment variables:
```bash
cd backend
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://localhost:3306/taskflow_db
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_base64_encoded_secret_min32bytes
./mvnw spring-boot:run
```

---

## 📡 API Endpoints

### 🔐 Authentication & Users
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new STUDENT or TEACHER |
| POST | `/api/auth/login` | Public | Login and receive JWT token with role claims |
| GET | `/api/users/me` | Authenticated | Get current authenticated user profile |
| PUT | `/api/users/me` | Authenticated | Update user name and change password |

### 📋 Personal Tasks (All Roles)
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/tasks` | Authenticated | List personal tasks (supports `search`, `category`, `status`, `sort`) |
| POST | `/api/tasks` | Authenticated | Create task with category, priority, due date, subtasks |
| GET | `/api/tasks/{id}` | Owner | Get specific personal task |
| PUT | `/api/tasks/{id}` | Owner | Update task details & subtasks |
| PATCH | `/api/tasks/{id}/toggle` | Owner | Toggle task completion |
| PATCH | `/api/tasks/{taskId}/subtasks/{subId}/toggle` | Owner | Toggle subtask checkbox |
| DELETE | `/api/tasks/{id}` | Owner | Delete personal task |
| GET | `/api/tasks/stats` | Authenticated | Get personal task statistics |

### 🏫 Courses & Enrollment
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/courses` | Authenticated | List courses (Admin: all, Teacher: taught, Student: enrolled/active) |
| GET | `/api/courses/{id}` | Authenticated | Get course details |
| POST | `/api/courses` | Teacher/Admin | Create new course |
| PUT | `/api/courses/{id}` | Owner Teacher/Admin | Update course |
| DELETE | `/api/courses/{id}` | Owner Teacher/Admin | Delete course |
| POST | `/api/courses/{id}/enroll` | Authenticated | Student self-enroll or Teacher/Admin enroll by email |
| DELETE | `/api/courses/{id}/enroll/{studentId}` | Owner/Student/Admin | Unenroll student from course |
| GET | `/api/courses/{id}/students` | Owner Teacher/Admin | List enrolled students and completion progress |

### 📑 Assignments & Submissions
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/assignments` | Authenticated | Get assignments according to role context |
| GET | `/api/courses/{courseId}/assignments` | Enrolled/Teacher/Admin | Get assignments for specific course |
| POST | `/api/courses/{courseId}/assignments` | Owner Teacher/Admin | Create assignment (notifies enrolled students) |
| GET | `/api/assignments/{id}` | Enrolled/Teacher/Admin | Get assignment details |
| PUT | `/api/assignments/{id}` | Owner Teacher/Admin | Update assignment |
| DELETE | `/api/assignments/{id}` | Owner Teacher/Admin | Delete assignment |
| POST | `/api/assignments/{id}/submit` | Enrolled Student | Submit coursework with notes & link |
| GET | `/api/assignments/{id}/my-submission` | Student | View student's own submission & grade feedback |
| GET | `/api/assignments/{id}/submissions` | Owner Teacher/Admin | List all student submissions for assignment |
| PATCH | `/api/assignments/{id}/submissions/{sId}/grade` | Owner Teacher/Admin | Grade submission and leave teacher feedback |

### 📊 Dashboards & Calendar
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/dashboard/admin` | ADMIN | Platform metrics, user breakdown, course stats |
| GET | `/api/dashboard/teacher` | TEACHER/ADMIN | Teacher class overview, pending submissions, completion rates |
| GET | `/api/dashboard/student` | STUDENT | Personal tasks, enrolled classes, grades, upcoming deadlines |
| GET | `/api/calendar/events` | Authenticated | Unified timeline of personal tasks and assignment due dates |

### 📢 Announcements & Notifications
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/announcements` | Authenticated | List announcements relevant to user |
| POST | `/api/announcements` | Teacher/Admin | Publish platform or class announcement |
| DELETE | `/api/announcements/{id}` | Author/Admin | Delete announcement |
| GET | `/api/notifications` | Authenticated | List user in-app notifications |
| GET | `/api/notifications/unread-count` | Authenticated | Count unread notifications |
| PATCH | `/api/notifications/{id}/read` | Owner | Mark notification as read |
| PATCH | `/api/notifications/read-all` | Authenticated | Mark all notifications as read |

### 🏷️ Categories & Administration
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/categories` | Authenticated | List available categories |
| POST | `/api/categories` | ADMIN | Create new category tag |
| PUT | `/api/categories/{id}` | ADMIN | Update category |
| DELETE | `/api/categories/{id}` | ADMIN | Delete category |
| GET | `/api/admin/users` | ADMIN | List and search users with role filter |
| GET | `/api/admin/users/{id}` | ADMIN | Get user details |
| PUT | `/api/admin/users/{id}` | ADMIN | Update user name, email, role, status |
| PATCH | `/api/admin/users/{id}/role` | ADMIN | Change user role (ADMIN, TEACHER, STUDENT) |
| PATCH | `/api/admin/users/{id}/status` | ADMIN | Activate or deactivate user account |
| GET | `/api/admin/stats` | ADMIN | Platform-wide user and activity counts |

---

## 🔒 Security Architecture

- **Stateless Session Management**: Backed by JWT with token signing and role claims.
- **Layered Authorization**: Controller-level `@PreAuthorize` combined with strict service-level checks verify resource ownership and role permissions on every query and mutation.
- **CORS Hardening**: Strict origin control configurable via `app.cors.allowed-origins`, rejecting wildcard access in production environments.
- **Deactivated Account Enforcement**: `JwtAuthFilter` checks account status on every request, revoking access immediately upon deactivation.

---

## 📝 License

MIT License — Feel free to use, modify, and distribute.
