# CrunchyCode Task 5 – Cab Booking Management System

A REST API-based Cab Booking Management System developed using **Java 21, Spring Boot 3.5.7, MySQL, Spring Security, JWT, and Flyway** as part of the CrunchyCode Java Backend Developer Internship.

The application manages Drivers, Rides, and Vehicle Types with authentication, authorization, validation, pagination, and centralized exception handling.

## Project Links

| Resource | Link |
|---|---|
| GitHub Repository | [crunchy-java-backend-t5-sameer](https://github.com/sameerkr056/crunchy-java-backend-t5-sameer) |
| Live Application | [Railway Deployment](https://crunchy-java-backend-t5-sameer-production.up.railway.app/) |
| Swagger UI | [Open Swagger](https://crunchy-java-backend-t5-sameer-production.up.railway.app/swagger-ui/index.html) |
| Demo Video | [Watch Demo](https://drive.google.com/file/d/1qyk_uDFBvB5db6CF6nb1vEdG9Bm9Dgn3/view?usp=sharing) |

## 1. Project Overview

The Cab Booking Management System provides REST APIs to manage drivers and their rides.

Main functionalities include:

- Create, retrieve, update, and delete driver records.
- Create, retrieve, update, and manage rides.
- Manage vehicle types.
- Associate rides with existing drivers using `driverId`.
- Secure APIs using JWT authentication.
- Apply role-based authorization.
- Validate incoming request data.
- Handle exceptions with meaningful HTTP responses.
- Support pagination for driver records.
- Manage database schema changes using Flyway.

## 2. Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming Language |
| Spring Boot 3.5.7 | Backend Framework |
| Spring Web | REST API Development |
| Spring Data JPA | Database Operations |
| MySQL 8 | Relational Database |
| Spring Security | Authentication and Authorization |
| JWT | Token-Based Authentication |
| Flyway | Database Migration |
| Maven | Build and Dependency Management |
| Swagger / OpenAPI | API Documentation |
| Docker | Containerization |
| Railway | Cloud Deployment |
| JUnit / Mockito | Testing |

## 3. Project Architecture

The application follows a layered architecture.

```text
Client
  |
  v
Controller Layer
  |
  v
Service Layer
  |
  v
Repository Layer
  |
  v
MySQL Database
```

**Controller Layer:** Handles HTTP requests and responses.

**Service Layer:** Contains application business logic.

**Repository Layer:** Communicates with the database using Spring Data JPA.

**Database Layer:** Stores drivers, rides, vehicle types, and related information.

## 4. Project Structure

```text
crunchy-java-backend-t5-sameer
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── crunchycode
│   │   │           └── cabbooking
│   │   │               ├── config
│   │   │               ├── controller
│   │   │               ├── dto
│   │   │               ├── entity
│   │   │               ├── exception
│   │   │               ├── repository
│   │   │               ├── security
│   │   │               ├── service
│   │   │               └── CabBookingApplication.java
│   │   │
│   │   └── resources
│   │       ├── db
│   │       │   └── migration
│   │       └── application.properties
│   │
│   └── test
│
├── Dockerfile
├── pom.xml
└── README.md
```

## 5. Database Management

The project uses MySQL 8 as its relational database.

Flyway manages database schema changes through versioned migration scripts.

Migration scripts are maintained inside:

```text
src/main/resources/db/migration
```

The project includes versioned migrations from `V1` to `V10`.

## 6. Main Modules

### Driver Management

- Create a new driver.
- Retrieve all drivers.
- Retrieve a driver by ID.
- Update driver details.
- Delete a driver.
- Retrieve driver records using pagination.

### Ride Management

- Create a ride for an existing driver.
- Retrieve all rides.
- Retrieve a ride by ID.
- Update ride details.
- Manage ride status.
- Validate ride status transitions.

### Vehicle Type Management

- Manage vehicle type records.
- Retrieve available vehicle types.
- Associate vehicle types with drivers.

## 7. Authentication and Security

Spring Security and JWT are used to secure the REST APIs.

Authentication flow:

1. User registers or logs in.
2. The application validates the credentials.
3. A JWT token is generated after successful authentication.
4. The client sends the token in the Authorization header.
5. Spring Security validates the token before allowing access to protected APIs.

Authorization header format:

```http
Authorization: Bearer <your-jwt-token>
```

## 8. API Endpoints

Base URL:

```text
https://crunchy-java-backend-t5-sameer-production.up.railway.app
```

### Authentication APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/register` | Register user |
| POST | `/api/v1/auth/login` | Login user |

### Driver APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/drivers` | Create driver |
| GET | `/api/v1/drivers` | Get all drivers |
| GET | `/api/v1/drivers/{id}` | Get driver by ID |
| PUT | `/api/v1/drivers/{id}` | Update driver |
| DELETE | `/api/v1/drivers/{id}` | Delete driver |

### Ride APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/rides` | Create ride |
| GET | `/api/v1/rides` | Get all rides |
| GET | `/api/v1/rides/{id}` | Get ride by ID |
| PUT | `/api/v1/rides/{id}` | Update ride |
| DELETE | `/api/v1/rides/{id}` | Delete ride |

### Vehicle Type APIs

Vehicle Type APIs are available through the `/api/v1/vehicle-types` resource.

Refer to Swagger UI for the complete endpoint list, request formats, and response details.

## 9. Pagination

Pagination is implemented for retrieving driver records.

Example request:

```http
GET /api/v1/drivers?page=0&size=100
```

Parameters:

- `page`: Page number (starts from 0).
- `size`: Number of records per page.

Pagination helps retrieve large datasets efficiently instead of loading all records at once.

## 10. Performance Testing

Performance testing was conducted on the Driver List API using 1,000 driver records.

**Test endpoint:**

```http
GET /api/v1/drivers?page=0&size=100
```

### Dataset

| Description | Count |
|---|---:|
| Original driver records | 25 |
| Generated test records | 975 |
| Total driver records | 1,000 |

### Local Performance Results

| Page | Response Time |
|---|---:|
| Page 0 | 81 ms |
| Page 1 | 46 ms |
| Page 5 | 57 ms |
| Page 9 | 39 ms |
| **Average** | **55.75 ms** |

### Railway Performance Results

| Page | Response Time |
|---|---:|
| Page 0 | 408 ms |
| Page 1 | 386 ms |
| **Average** | **397 ms** |

### Performance Summary

| Environment | Average Response Time |
|---|---:|
| Local | 55.75 ms |
| Railway | 397 ms |

The average response time on Railway was below the 500 ms target for the tested Driver List API.

**Note:** These measurements represent the tested endpoint and conditions, not every API in the application.

## 11. Running the Project Locally

### Prerequisites

- Java 21
- Maven
- MySQL 8
- IntelliJ IDEA or another Java IDE

### Step 1: Clone the Repository

```bash
git clone https://github.com/sameerkr056/crunchy-java-backend-t5-sameer.git
```

### Step 2: Open the Project

Open the cloned project in IntelliJ IDEA.

### Step 3: Configure MySQL

Create a MySQL database and configure the database connection and other required application properties.

### Step 4: Run the Application

Using Maven:

```bash
mvn spring-boot:run
```

Or run `CabBookingApplication.java` from IntelliJ IDEA.

### Step 5: Open Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

Use Swagger UI to test the available REST APIs.

## 12. Docker

The project includes a Dockerfile for containerizing the Spring Boot application.

Build the Docker image:

```bash
docker build -t crunchy-java-backend-t5 .
```

Run the container:

```bash
docker run -p 8080:8080 crunchy-java-backend-t5
```

Make sure the required database configuration is available to the running application.

## 13. Deployment

The application is deployed on Railway.

- Backend application: Railway
- Database: Railway MySQL
- API documentation: Swagger UI

**Live Application:**

https://crunchy-java-backend-t5-sameer-production.up.railway.app/

**Swagger Documentation:**

https://crunchy-java-backend-t5-sameer-production.up.railway.app/swagger-ui/index.html

## 14. Conclusion

The Cab Booking Management System demonstrates the development of a backend REST API using Java and Spring Boot.

The project covers CRUD operations, database integration, authentication, authorization, validation, exception handling, pagination, database migrations, Docker, and cloud deployment.

It also includes performance testing with 1,000 driver records to evaluate the response time of the paginated Driver List API.

---

**Developed as part of the CrunchyCode Java Backend Developer Internship.**