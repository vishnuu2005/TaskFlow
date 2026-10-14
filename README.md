# TaskFlow

A full-stack **To-Do List & Task Planner** with a Spring Boot backend (JWT auth, MySQL) and a stunning glassmorphism frontend (vanilla HTML/CSS/JS).

---

## ✨ Features

- 🔐 **JWT Authentication** — secure register & login
- ✅ **Full CRUD** — create, read, update, delete tasks
- 🔄 **Toggle Completion** — one-click done/undone
- 🎯 **Priority Levels** — High, Medium, Low with color coding
- 📅 **Due Dates** — overdue detection
- 📊 **Live Stats** — circular progress ring + stat cards
- 🔍 **Search & Sort** — instant search, sort by newest/oldest/priority/due
- 📂 **Filter Views** — All, Pending, Completed, High Priority, Overdue
- 🔔 **Toast Notifications** — elegant slide-in feedback
- 🎨 **Dark Glassmorphism UI** — animated orbs, blur cards, gradient accents
- 📱 **Responsive** — works on mobile and desktop

---

## 🏗️ Tech Stack

| Layer    | Technology                              |
|----------|-----------------------------------------|
| Frontend | HTML5, Vanilla CSS, Vanilla JS          |
| Backend  | Java 21, Spring Boot 3, Spring Security |
| Auth     | JWT (JJWT 0.12.6)                       |
| Database | MySQL 8                                 |
| Hosting  | GitHub Pages (frontend) + Render (backend) |

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven
- MySQL 8

### Quick Start (Zero Config)

Run the backend with in-memory H2 database (no MySQL setup required):

```bash
cd backend
./mvnw spring-boot:run
```

Once started:
- 🌐 **Web UI:** Open [http://localhost:8081/](http://localhost:8081/) in your browser
- 🗄️ **H2 Console:** [http://localhost:8081/h2-console](http://localhost:8081/h2-console) (JDBC URL: `jdbc:h2:mem:taskflow_db`, User: `sa`, Password: blank)
- 🧪 **Run Tests:** `./mvnw test`

### Running with MySQL (Production Profile)

Create the database:
```sql
CREATE DATABASE taskflow_db;
```

Run with MySQL environment variables:
```bash
cd backend

export SPRING_PROFILES_ACTIVE=prod
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET=your_base64_encoded_secret_min32bytes

./mvnw spring-boot:run
```

### Frontend Options

1. **Integrated:** The frontend is served directly by the Spring Boot server at [http://localhost:8081/](http://localhost:8081/).
2. **Standalone:** You can also open `frontend/index.html` directly in any browser or deploy it to GitHub Pages / Vercel. It automatically detects and connects to your local or deployed backend.

---

## 📡 API Endpoints

### Auth
| Method | Path                  | Description       |
|--------|-----------------------|-------------------|
| POST   | `/api/auth/register`  | Register new user |
| POST   | `/api/auth/login`     | Login & get JWT   |

### Tasks _(all require `Authorization: Bearer <token>`)_
| Method | Path                      | Description         |
|--------|---------------------------|---------------------|
| GET    | `/api/tasks`              | Get all tasks       |
| POST   | `/api/tasks`              | Create task         |
| GET    | `/api/tasks/{id}`         | Get task by ID      |
| PUT    | `/api/tasks/{id}`         | Update task         |
| PATCH  | `/api/tasks/{id}/toggle`  | Toggle completion   |
| DELETE | `/api/tasks/{id}`         | Delete task         |
| GET    | `/api/tasks/stats`        | Get task statistics |

### Users
| Method | Path           | Description         |
|--------|----------------|---------------------|
| GET    | `/api/users/me`| Get current profile |

---

## 🐳 Docker (for Render)

```bash
cd backend
docker build -t taskflow-backend .
docker run -p 8080:8080 \
  -e DB_USERNAME=user \
  -e DB_PASSWORD=pass \
  -e JWT_SECRET=base64secret \
  taskflow-backend
```

---

## 🌐 Deployment

### Frontend → GitHub Pages
1. Push the repo to GitHub
2. Go to Settings → Pages → Source: Deploy from branch `main` → folder `/frontend`
3. Your app will be live at `https://yourusername.github.io/TaskFlow`

### Backend → Render
1. Connect your GitHub repo to Render
2. Set root directory to `backend`
3. Add environment variables: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
4. Set build command: `./mvnw package -DskipTests`
5. Set start command: `java -jar target/*.jar`

---

## 📁 Project Structure

```
TaskFlow/
├── frontend/
│   └── index.html              # Complete SPA (HTML + CSS + JS)
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/taskflow/
│       ├── controller/         # REST controllers
│       ├── service/            # Business logic
│       ├── entity/             # JPA entities
│       ├── dto/                # Request/Response records
│       ├── repository/         # Spring Data JPA
│       ├── security/           # JWT filter & service
│       ├── config/             # Security config
│       └── exception/          # Global error handler
└── README.md
```

---

## 🔒 Security Notes

- Passwords hashed with BCrypt (strength 12)
- JWT signed with HMAC-SHA256
- JWT expires in 1 hour (configurable)
- CORS enabled for all origins (restrict in production)
- No sensitive credentials in source code

---

## 📝 License

MIT License — feel free to use, modify, and distribute.
