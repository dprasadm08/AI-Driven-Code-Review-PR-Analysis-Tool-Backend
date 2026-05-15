# AI-Driven Code Review PR Analysis Tool - Backend

A Spring Boot backend application that leverages AI (OpenAI GPT-4 and Claude) to perform automated code review and analysis on GitHub Pull Requests.

## 🚀 Features

- **Authentication & Authorization**: JWT-based security with Spring Security
- **GitHub Integration**: Pull request fetching, diff parsing, and webhook support
- **AI-Powered Analysis**: 
  - Bug detection
  - Security vulnerability scanning
  - Performance optimization suggestions
  - Code quality assessment
  - Test case recommendations
- **MongoDB Storage**: Persist analysis results and user data
- **RESTful APIs**: Well-structured endpoints for frontend integration

## 📋 Prerequisites

- **Java 17** or higher
- **MongoDB** (local or Atlas)
- **Maven 3.9+** (or use included Maven wrapper)
- **GitHub Personal Access Token**
- **OpenAI API Key** or **Claude API Key**

## 🛠️ Technology Stack

- **Spring Boot 3.2.5**
- **Spring Data MongoDB**
- **Spring Security + JWT**
- **Spring WebFlux** (for async HTTP calls)
- **OpenFeign** (for GitHub API integration)
- **Lombok**
- **JWT (io.jsonwebtoken)**

## 📦 Package Structure

```
com.aiprreview/
├── config/          # Configuration classes (Security, MongoDB, JWT, WebClient, CORS)
├── controller/      # REST Controllers (Auth, Repository, PR, Analysis, Webhook, Health)
├── service/         # Business logic services
├── repository/      # MongoDB repositories
├── entity/          # MongoDB entities
├── dto/             # Data Transfer Objects
│   ├── auth/
│   ├── repository/
│   ├── pullrequest/
│   └── analysis/
├── security/        # JWT filters, token provider, user details service
├── github/          # GitHub API clients and parsers
├── webhook/         # GitHub webhook handling
├── ai/              # AI service integrations (OpenAI, Claude)
├── analysis/        # Analysis services (Bug, Security, Performance, etc.)
└── exception/       # Exception handlers
```

## ⚙️ Configuration

### 1. Clone the Repository

```bash
git clone https://github.com/dprasadm08/AI-Driven-Code-Review-PR-Analysis-Tool-Backend.git
cd AI-Driven-Code-Review-PR-Analysis-Tool-Backend
```

### 2. Configure Environment Variables

Create a `.env` file or set environment variables:

```properties
# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-min-256-bits

# OpenAI Configuration
OPENAI_API_KEY=sk-your-openai-api-key

# Claude Configuration (optional)
CLAUDE_API_KEY=sk-ant-your-claude-api-key

# MongoDB (if not using default)
SPRING_DATA_MONGODB_URI=mongodb://localhost:27017/ai-pr-review

# GitHub Token (for API calls)
GITHUB_TOKEN=ghp_your-github-personal-access-token
```

### 3. Update application.yml

Edit `src/main/resources/application.yml` with your specific configurations.

## 🏃 Running the Application

### Option 1: Using Maven Wrapper (Recommended)

```bash
# Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw clean install
./mvnw spring-boot:run
```

### Option 2: Using Maven

```bash
mvn clean install
mvn spring-boot:run
```

### Option 3: Using Docker

```bash
# Build Docker image
docker build -t ai-pr-review-backend .

# Run container
docker run -p 8080:8080 \
  -e JWT_SECRET=your-secret \
  -e OPENAI_API_KEY=your-key \
  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/ai-pr-review \
  ai-pr-review-backend
```

## 🔍 Verify Application

Once the application starts, verify it's running:

### Health Check Endpoints

```bash
# Basic health check
curl http://localhost:8080/api/health

# Readiness probe
curl http://localhost:8080/api/health/ready

# Liveness probe
curl http://localhost:8080/api/health/live
```

Expected response:
```json
{
  "status": "UP",
  "service": "AI PR Review Backend",
  "timestamp": "2026-05-15T10:30:00",
  "version": "1.0.0"
}
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api
```

### Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check |
| `/auth/login` | POST | User authentication |
| `/auth/signup` | POST | User registration |
| `/repositories` | GET, POST | Manage repositories |
| `/pull-requests` | GET | List pull requests |
| `/analysis` | POST | Trigger AI analysis |
| `/webhooks/github` | POST | GitHub webhook receiver |

## 🔧 Development

### Build the Project

```bash
mvnw.cmd clean package -DskipTests
```

### Run Tests

```bash
mvnw.cmd test
```

### Generate JAR

```bash
mvnw.cmd clean package
java -jar target/ai-pr-review-backend-1.0.0.jar
```

## 📝 Environment Setup

### MongoDB Setup

1. **Local MongoDB**:
   ```bash
   # Install and start MongoDB
   mongod --dbpath /path/to/data
   ```

2. **MongoDB Atlas** (Cloud):
   - Create a free cluster at https://www.mongodb.com/cloud/atlas
   - Get connection string
   - Update `application.yml`

### GitHub Token Setup

1. Go to GitHub Settings → Developer Settings → Personal Access Tokens
2. Generate new token with scopes: `repo`, `read:org`, `webhook`
3. Set as environment variable or in application.yml

## 🐛 Troubleshooting

### Application won't start

- Check if MongoDB is running
- Verify Java version: `java -version` (should be 17+)
- Check port 8080 is not already in use

### MongoDB connection failed

- Verify MongoDB URI in application.yml
- Check MongoDB is running: `mongosh` or MongoDB Compass

### JWT errors

- Ensure JWT_SECRET is at least 256 bits (32+ characters)

## 📄 License

This project is licensed under the MIT License.

## 👥 Contributors

- [dprasadm08](https://github.com/dprasadm08)

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📧 Contact

For questions or support, please open an issue on GitHub.
