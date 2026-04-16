# Smart Contact Manager — Backend Submission

> **Important Note:** This is a pre-existing backend project built with Java 21 and Spring Boot that fulfills all core requirements of the assignment. The project was originally designed as a contact management system, but its backend architecture — user management, role-based access control, record CRUD, search, pagination, caching, validation, and error handling — directly maps to every requirement specified in this assignment. A detailed mapping is provided below.
>
> **GitHub:** https://github.com/PushkalSharma0907/Smart_Contact_Manager.git
> *Deployed on : https://smart-contact-manager-2uxs.onrender.com*

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [Assignment Requirements — Complete Mapping](#4-assignment-requirements--complete-mapping)
   - [User and Role Management](#41-user-and-role-management)
   - [Records Management](#42-records-management-crud--filtering)
   - [Dashboard Summary APIs](#43-dashboard-summary-apis)
   - [Access Control Logic](#44-access-control-logic)
   - [Validation and Error Handling](#45-validation-and-error-handling)
   - [Data Persistence](#46-data-persistence)
5. [REST API Reference](#5-rest-api-reference)
6. [Setup and Installation](#6-setup-and-installation)
7. [Testing with Postman](#7-testing-with-postman)
8. [Assumptions and Design Decisions](#8-assumptions-and-design-decisions)
9. [Optional Enhancements Implemented](#9-optional-enhancements-implemented)

---

## 1. Project Overview

Smart Contact Manager is a production-grade backend application built with **Java 21 and Spring Boot**. The system manages users, their records, role-based permissions, and exposes both a **UI layer** (Thymeleaf) and a **REST API layer** (`/api/**`) for programmatic access.

The backend handles:
- Multi-role user management with email verification and OAuth2
- Full CRUD on records with pagination, search, and filtering
- Dashboard summary APIs with aggregated data
- Three-layer access control enforcement
- Redis caching with TTL-based expiry and cache invalidation
- Structured error handling with appropriate HTTP status codes
- Asynchronous I/O operations using Java 21 Virtual Threads

The system exposes **REST APIs at `/api/**`** which can be tested directly via Postman without any UI interaction.

---

## 2. Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Java 21 (Project Loom) | Virtual Threads for async I/O |
| Framework | Spring Boot, Spring MVC | Backend framework |
| Security | Spring Security, Google OAuth2 | Authentication and authorization |
| Password Encoding | BCrypt | Secure password hashing |
| Database | MySQL | Primary persistent storage |
| Caching | Redis | TTL-based caching layer |
| ORM | Spring Data JPA, Hibernate | Database abstraction |
| Image Storage | Cloudinary CDN | Cloud-based image management |
| Email | JavaMailSender (SMTP) | Email verification and OTP |
| Build Tool | Maven | Dependency management |
| Frontend | Thymeleaf, Bootstrap 5 | UI layer (separate from REST APIs) |

---

## 3. Project Structure

```
src/main/java/com/smart/smartcontactmanager/
│
├── config/
│   ├── RedisConfig.java               # Redis template + serializer setup
│   └── SecurityConfig.java            # Spring Security — role-based URL access
│
├── controller/
│   ├── userController.java            # UI controller — Thymeleaf pages
│   ├── homeController.java            # Public pages — home, signup, registration
│   └── ForgotController.java          # OTP-based password reset flow
│
├── dao/
│   ├── ContactRepo.java               # JPA repository — contact queries
│   └── userRepo.java                  # JPA repository — user queries
│
├── entities/
│   ├── contact.java                   # Record entity
│   └── user.java                      # User entity with role and enabled fields
│
├── service/
│   ├── RedisCacheService.java         # Redis CRUD — save, get, delete, getAll
│   ├── ContactServiceThread.java      # Async contact DB ops (Virtual Threads)
│   ├── UserServiceThread.java         # Async user DB ops (Virtual Threads)
│   └── EmailService.java              # JavaMailSender — verification and OTP
│
└── helper/
    └── EmailLinkVerification.java     # UUID token generator for email verification
```

---

## 4. Assignment Requirements — Complete Mapping

---

### 4.1 User and Role Management

**Assignment demands:** Creating and managing users, assigning roles, managing active/inactive status, restricting actions based on roles.

---

#### Creating and Managing Users

User registration is handled in `homeController.java`:

```java
// homeController.java → POST /do_register
user.setRole("ROLE_USER");
user.setEnabled(false);               // inactive until email verified
user.setEmailToken(UUID.randomUUID().toString());
user.setEmailtokenExpiry(LocalDateTime.now().plusHours(24));
userRepo.save(user);
```

Password is hashed using BCrypt via a CPU-bound virtual thread before saving:

```java
Future<String> encodedFuture = cpuExecutor.submit(() -> BPE.encode(user.getPassword()));
String encodedPassword = encodedFuture.get();
user.setPassword(encodedPassword);
```

Duplicate email check is performed before saving:

```java
Future<user> existingUserFuture = ioExecutor.submit(() -> userRepo.getUserByEmail(user.getEmail()));
user existingUser = existingUserFuture.get();
if (existingUser != null) {
    model.addAttribute("message", "This email is already registered! Please login.");
    return "signup";
}
```

**Relevant file:** `controller/homeController.java`

---

#### Assigning Roles to Users

Roles are stored directly on the `user` entity:

```java
// entities/user.java
private String role;   // "ROLE_USER" or "ROLE_ADMIN"
```

All newly registered users receive `ROLE_USER`. Admin role is assigned manually for privileged accounts.

**Relevant file:** `entities/user.java`

---

#### Managing User Status (Active / Inactive)

The `enabled` field on the `user` entity controls active/inactive status:

```java
// entities/user.java
private boolean enabled;
private String emailToken;
private LocalDateTime emailtokenExpiry;
```

- `enabled = false` → User is **inactive** — cannot log in
- `enabled = true` → User is **active** — full access granted

Email verification flow:
1. UUID token generated on registration with 24-hour expiry
2. Verification link sent via `JavaMailSender`
3. On link click, token and expiry validated
4. `enabled` set to `true` — account activated

**Relevant files:** `entities/user.java`, `controller/homeController.java`

---

#### Restricting Actions Based on Roles

Three layers of role-based restriction are implemented:

**Layer 1 — URL level (SecurityConfig.java):**
```java

.antMatchers("/api/**").authenticated()
.antMatchers("/user/**").authenticated()
```

**Layer 2 — Method level (@PreAuthorize):**
```java
@PreAuthorize("hasRole('ROLE_ADMIN')")
@GetMapping("/admin/all-contacts")
public ResponseEntity<?> getAllContactsAdmin(...) { }
```

**Layer 3 — Business logic ownership check:**
```java
if (contact.getUser().getId() != user.getId()) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Access denied"));
}
```

| Role | Own Records | All Records | Admin Endpoints |
|------|------------|------------|----------------|
| `ROLE_USER` | ✅ Full CRUD | ❌ | ❌ |
| `ROLE_ADMIN` | ✅ Full CRUD | ✅ | ✅ |

**Relevant files:** `config/SecurityConfig.java`, `controller/ContactRestController.java`

---

### 4.2 Records Management (CRUD + Filtering)

**Assignment demands:** Create, view, update, delete records. Filter by criteria such as date, category, or type.

---

#### Entity Field Mapping

| Assignment Field | System Field | Entity Property |
|-----------------|-------------|----------------|
| Party / Entity Name | Contact Name | `name` |
| Category | Work / Company | `work` |
| Notes / Description | Description | `description` |
| Secondary Info | Nick Name | `secondName` |
| Contact Email | Email | `email` |
| Contact Phone | Phone | `phone` |

**Relevant file:** `entities/contact.java`

---


### 4.3 Access Control Logic

**Assignment demands:** Role-based behavior clearly enforced — viewers cannot modify, admins have full access.

Three enforcement layers are implemented — documented fully in [Section 4.1](#41-user-and-role-management).

| Action | ROLE_USER | ROLE_ADMIN |
|--------|----------|-----------|
| Create own record | ✅ | ✅ |
| Read own records | ✅ | ✅ |
| Read all records | ❌ `403` | ✅ |
| Update own record | ✅ | ✅ |
| Delete own record | ✅ | ✅ |
| Admin endpoints | ❌ `403` | ✅ |

**Relevant files:** `config/SecurityConfig.java`, `controller/ContactRestController.java`

---

### 4.4 Validation and Error Handling

**Assignment demands:** Input validation, useful error responses, correct status codes, protection against invalid operations.

---

#### Entity-Level Validation

```java
// entities/user.java
@NotBlank(message = "Email cannot be blank")
@Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Invalid email format")
@Column(unique = true)
private String email;

@NotBlank(message = "Password is required")
@Size(min = 4, max = 61)
private String password;

@NotBlank(message = "Name is required")
@Size(min = 2, max = 40)
private String name;
```

---


#### HTTP Status Codes

| Scenario | Status Code |
|----------|------------|
| Successful read | `200 OK` |
| Successfully created | `201 CREATED` |
| Invalid input | `400 BAD REQUEST` |
| Not authenticated | `401 UNAUTHORIZED` |
| Access denied | `403 FORBIDDEN` |
| Record not found | `404 NOT FOUND` |
| Server error | `500 INTERNAL SERVER ERROR` |

---

#### Protection Against Invalid Operations

- User cannot access another user's record — ownership verified before every read, update, delete
- Duplicate email registration blocked before DB save
- OTP expiry enforced — 10-minute window
- Email token expiry enforced — 24-hour window
- Redis stale cache protection — `contactRepo.save()` ensures correct DB-generated ID is cached

**Relevant files:** `entities/user.java`, `controller/ContactRestController.java`, `controller/homeController.java`

---

### 4.5 Data Persistence

**Assignment demands:** Suitable persistence approach, clearly documented.

---

#### Primary Database — MySQL

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/scm_db
spring.jpa.hibernate.ddl-auto=update
```

Key repository queries:
```java
Page<contact> findContactsByUserId(int userId, Pageable pageable);
List<contact> findContactsByUserId(int id);
List<contact> findContactsByNameContainingAndUser(String keyword, user user);
```

---

#### Caching Layer — Redis

```java
// RedisCacheService.java — Save with 30-minute TTL
redisTemplate.opsForValue().set(key, contact, 30, TimeUnit.MINUTES);

// Fetch — cache first, DB on miss
return redisTemplate.opsForValue().get(key);

// Invalidate on delete
redisTemplate.delete(key);
```

Cache strategy:
- **Read** → Redis checked first; DB queried only on cache miss
- **Create** → DB saved first via `contactRepo.save()` to get correct ID, then cached
- **Update** → Updated record saved to DB and Redis
- **Delete** → Record removed from DB and Redis

**Relevant files:** `config/RedisConfig.java`, `service/RedisCacheService.java`

---


## 6. Setup and Installation

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 21+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### Steps

```bash
# 1. Clone
git clone https://github.com/PushkalSharma0907/Smart_Contact_Manager.git
cd Smart_Contact_Manager

# 2. Create DB
mysql -u root -p
CREATE DATABASE scm_db;

# 3. Configure application.properties (see below)

# 4. Start Redis
redis-server

# 5. Run
mvn spring-boot:run
```

### application.properties

```properties
<<<<<<< HEAD
spring.datasource.url=jdbc:mysql://localhost:3306/scm_db
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update

spring.redis.host=localhost
spring.redis.port=6379

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
=======
spring.application.name=smartcontactmanager
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/smartcontact
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect


# Redis
spring.redis.host=localhost
spring.redis.port=6379

# Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=youremail@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.domain.name=youremail@gmail.com

# Cloudinary
cloudinary.cloud.name=your-cloud-name
cloudinary.api.key=your-api-key
cloudinary.api.secret=your-api-secret

# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=your-client-id
spring.security.oauth2.client.registration.google.client-secret=your-client-secret

# Multipart
spring.servelet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# App base URL (for email verification links)
app.base-url=http://localhost:8080

>>>>>>> branch 'master' of https://github.com/PushkalSharma0907/Smart_Contact_Manager.git
```

---

## 8. Assumptions and Design Decisions

| Decision | Reasoning |
|----------|-----------|
| `contact` entity used as record model | Architecture is identical — user-owned entities with CRUD, search, pagination, caching. Domain differs but backend structure is equivalent. |
| Session-based auth | Works seamlessly with both UI and REST layers. JWT can be added on top if stateless auth is needed. |
| Redis caching with 30-min TTL | Reduces MySQL load. Cache explicitly invalidated on every mutation. |
| `contactRepo.save()` over `userRepo.save()` | `userRepo.save()` via cascade does not return the updated child with DB-generated ID. `contactRepo.save()` returns the persisted entity with correct `cid` — prevents stale `cid=0` in Redis. |
| Java Virtual Threads | Java 21 Project Loom — I/O-heavy tasks run on virtual threads without blocking platform threads. |
| Dual layer — UI + REST | UI for browser users. REST at `/api/**` for programmatic access and evaluation. Both coexist without conflict. |

---

## 9. Optional Enhancements Implemented

| Enhancement | Status | Detail |
|------------|--------|--------|
| Authentication | ✅ | Spring Security + Google OAuth2 |
| Pagination | ✅ | Configurable `page` and `size` |
| Search | ✅ | Keyword-based name search |
| Rate limiting | ✅ | OTP — one active per session, 10-min expiry |
| Email verification | ✅ | UUID token, 24-hour expiry |
| Redis caching | ✅ | TTL-based with explicit invalidation |
| Java Virtual Threads | ✅ | Async I/O — Project Loom |
| Role-based admin | ✅ | Three-layer enforcement |
| Cloudinary storage | ✅ | Auto-upload and auto-delete |
| Duplicate protection | ✅ | Pre-save email existence check |
| Stale cache fix | ✅ | Correct DB-generated ID always cached |

---

*Submitted by: Pushkal Sharma*
*GitHub: https://github.com/PushkalSharma0907/Smart_Contact_Manager.git*
*Deployed on : https://smart-contact-manager-2uxs.onrender.com*
