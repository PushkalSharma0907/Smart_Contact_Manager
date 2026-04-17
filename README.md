# Smart Contact Manager

> A full-stack contact management backend built with Java 21, Spring Boot, MySQL, Redis, and deployed on Render.
>
> **Live Demo:** https://smart-contact-manager-2uxs.onrender.com
> **GitHub:** https://github.com/PushkalSharma0907/Smart_Contact_Manager.git

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Live Demo & Test Credentials](#3-live-demo--test-credentials)
4. [Deployment Architecture](#4-deployment-architecture)
5. [Project Structure](#5-project-structure)
6. [Assignment Requirements Mapping](#6-assignment-requirements-mapping)
7. [API Reference](#7-api-reference)
8. [Local Setup](#8-local-setup)
9. [Assumptions & Design Decisions](#9-assumptions--design-decisions)
10. [Enhancements Implemented](#10-enhancements-implemented)

---

## 1. Project Overview

Smart Contact Manager is a production-grade backend application built with **Java 21 and Spring Boot**, deployed live on **Render**. The system manages users, records, role-based permissions, caching, and email workflows.

**Key highlights:**
- Multi-role user management (ROLE_USER, ROLE_ADMIN)
- Full CRUD with pagination, search, and filtering
- Redis-first caching with TTL-based expiry
- Java 21 Virtual Threads for async I/O
- Email verification + OTP-based password reset
- Google OAuth2 + Spring Security
- Admin panel — full user management
- Fully deployed and accessible online

---

## 2. Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Language | Java 21 (Project Loom) | Virtual Threads for async I/O |
| Framework | Spring Boot 2.7, Spring MVC | Backend framework |
| Security | Spring Security, Google OAuth2 | Auth and authorization |
| Password | BCrypt | Secure password hashing |
| Database | MySQL (CleverCloud) | Primary persistent storage |
| Caching | Redis (Upstash) | TTL-based caching layer |
| ORM | Spring Data JPA, Hibernate | Database abstraction |
| Image | Cloudinary CDN | Cloud image storage |
| Email | JavaMailSender (SMTP) | Email verification and OTP |
| Hosting | Render + Docker | Application deployment |
| Build | Maven | Dependency management |
| Frontend | Thymeleaf, Bootstrap 5 | UI layer |

---

## 3. Live Demo & Test Credentials

**Live URL:** https://smart-contact-manager-2uxs.onrender.com

> ⚠️ Free tier — app may take 30-50 seconds to wake up on first request.

| Role | Email | Password |
|------|-------|---------|
| Admin | admin@scm.com | admin123 |
| User | user@scm.com | user123 |

**Admin Panel:** https://smart-contact-manager-2uxs.onrender.com/admin/dashboard

Admin capabilities:
- View all registered users and their contact counts
- Enable / disable user accounts
- Change user roles (ROLE_USER ↔ ROLE_ADMIN)
- Delete users with all their contacts
- View platform-wide statistics (total users, contacts, active/inactive)

---

## 4. Deployment Architecture

```
┌─────────────────────────────────────────┐
│           Render (Free Tier)            │
│         Spring Boot App (Docker)        │
│              Port: 10000                │
└──────────────────┬──────────────────────┘
                   │
       ┌───────────┴────────────┐
       │                        │
┌──────▼──────────┐    ┌────────▼────────┐
│ CleverCloud      │    │ Upstash Redis   │
│ MySQL 8.0        │    │ Mumbai region   │
│ Free tier        │    │ Free tier       │
│ Max 5 conn       │    │ 10K cmds/day    │
└──────────────────┘    └─────────────────┘
       │
┌──────▼──────────┐
│ Cloudinary CDN  │
│ Image storage   │
│ Free tier       │
└─────────────────┘
```

**Key deployment decisions:**
- HikariCP pool size = 3 (CleverCloud free = max 5 connections)
- Upstash Redis with SSL (Lettuce client configured)
- Docker-based deployment on Render (Java not natively supported)
- Environment variables for all secrets — no credentials in code

---

## 5. Project Structure

```
src/main/java/com/smart/smartcontactmanager/
├── config/
│   ├── RedisConfig.java          # Redis + SSL (Upstash/Lettuce)
│   └── SecurityConfig.java       # Spring Security + roles
├── controller/
│   ├── AdminController.java      # Admin panel /admin/**
│   ├── userController.java       # User UI /user/**
│   ├── homeController.java       # Public pages
│   └── ForgotController.java     # OTP password reset
├── dao/
│   ├── ContactRepo.java          # Contact queries
│   └── userRepo.java             # User queries
├── entities/
│   ├── contact.java              # Contact entity
│   └── user.java                 # User entity
├── service/
│   ├── RedisCacheService.java    # Redis CRUD
│   ├── ContactServiceThread.java # Async contact ops
│   ├── UserServiceThread.java    # Async user ops
│   └── EmailService.java         # JavaMailSender
└── helper/
    ├── EmailLinkVerification.java
    └── Message.java
```

---

## 6. Assignment Requirements Mapping

### User and Role Management

| Requirement | Where | How |
|-------------|-------|-----|
| Create users | homeController → /do_register | Registration + email verification |
| Assign roles | user.java → role field | ROLE_USER on register, ROLE_ADMIN via admin panel |
| Active/Inactive | user.java → enabled field | false until email verified |
| Role restrictions | SecurityConfig + @PreAuthorize | 3-layer enforcement |
| Admin user management | AdminController → /admin/** | Enable/disable/delete/role change |

### Records Management

| Operation | Endpoint | File |
|-----------|---------|------|
| Create | POST /user/process-contact | userController.java |
| Read all | GET /user/show-contacts/{page} | userController.java |
| Read one | GET /user/{cid}/contact | userController.java |
| Update | POST /user/process-update | userController.java |
| Delete | GET /user/{cid}/delete | userController.java |
| Search | GET /user/show-contacts + keyword | ContactRepo.java |
| Pagination | PageRequest | ContactRepo.java |

### Dashboard Summary

| Metric | Implementation |
|--------|---------------|
| Total users | userRepo.count() |
| Total contacts | contactRepo.count() |
| Active users | userRepo.countByEnabled(true) |
| Inactive users | userRepo.countByEnabled(false) |
| Category-wise | Collectors.groupingBy(work) |
| Recent activity | stream().limit(5) |

### Access Control — 3 Layers

```
Layer 1 — URL level (SecurityConfig):
.antMatchers("/admin/**").hasRole("ADMIN")
.antMatchers("/user/**").authenticated()

Layer 2 — Method level (@PreAuthorize):
@PreAuthorize("hasRole('ROLE_ADMIN')")

Layer 3 — Business logic (ownership):
if (contact.getUser().getId() != user.getId()) → 403
```

### Validation

- Entity: @NotBlank, @Size, @Pattern, @Email, @Column(unique=true)
- Controller: BindingResult + manual null checks
- Duplicate email: pre-save check before registration

### Data Persistence

- MySQL via Spring Data JPA + Hibernate
- Redis cache: 30-min TTL, Redis-first strategy, explicit invalidation
- contactRepo.save() used (not userRepo.save()) — ensures correct DB-generated cid in cache

---

## 7. API Reference

**Base URL:** https://smart-contact-manager-2uxs.onrender.com

### Auth Endpoints

| Method | Endpoint | Description |
|--------|---------|-------------|
| POST | /do_register | Register new user |
| POST | /dologin | Login |
| GET | /logout | Logout |
| GET | /forgot | Forgot password |
| POST | /send-otp | Send OTP |
| POST | /verify-otp | Verify OTP |
| POST | /change-password | Reset password |

### User Endpoints

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | /user/index | Dashboard |
| GET | /user/show-contacts/{page} | Contacts (paginated) |
| GET | /user/{cid}/contact | Single contact |
| POST | /user/process-contact | Create contact |
| POST | /user/process-update | Update contact |
| GET | /user/{cid}/delete | Delete contact |
| GET | /user/profile | Profile |
| POST | /user/process-update-profile | Update profile |
| POST | /user/change-password | Change password |

### Admin Endpoints

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | /admin/dashboard | Stats overview |
| GET | /admin/users | All users list |
| GET | /admin/user/{id} | User detail |
| GET | /admin/user/{id}/toggle-status | Enable/disable user |
| POST | /admin/user/{id}/change-role | Change role |
| GET | /admin/user/{id}/delete | Delete user |

---

## 8. Local Setup

```bash
git clone https://github.com/PushkalSharma0907/Smart_Contact_Manager.git
cd Smart_Contact_Manager

# Create DB
mysql -u root -p -e "CREATE DATABASE scm_db;"

# Create src/main/resources/application-local.properties
spring.datasource.url=jdbc:mysql://localhost:3306/scm_db
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
spring.jpa.hibernate.ddl-auto=update
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.ssl=false
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
cloudinary.cloud.name=YOUR_NAME
cloudinary.api.key=YOUR_KEY
cloudinary.api.secret=YOUR_SECRET
spring.security.oauth2.client.registration.google.client-id=YOUR_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_SECRET

# Start Redis
redis-server

# Run
mvn spring-boot:run -Dspring.profiles.active=local
```

---

## 9. Assumptions & Design Decisions

| Decision | Reasoning |
|----------|-----------|
| Session-based auth | Works seamlessly with UI + REST; JWT can be layered on top |
| Redis TTL = 30 min | Balances performance and data freshness |
| contactRepo.save() | Returns DB-generated cid — prevents stale cid=0 in Redis cache |
| Java Virtual Threads | Java 21 Loom — I/O without blocking platform threads |
| HikariCP pool = 3 | CleverCloud free plan allows max 5 connections |
| Upstash SSL | Cloud Redis requires SSL; Lettuce client configured with useSsl() |
| Docker on Render | Render does not natively support Java — Docker used for deployment |
| Environment variables | All secrets injected at runtime — no credentials in codebase |

---

## 10. Enhancements Implemented

| Enhancement | Status | Detail |
|------------|--------|--------|
| Google OAuth2 | ✅ | Single sign-on via Google |
| Pagination | ✅ | Configurable page and size |
| Search | ✅ | Real-time keyword search |
| Email verification | ✅ | UUID token, 24-hour expiry |
| OTP password reset | ✅ | 10-minute session timeout |
| Redis caching | ✅ | TTL + explicit invalidation |
| Java Virtual Threads | ✅ | Project Loom async I/O |
| Admin panel | ✅ | Full user management UI |
| Cloudinary | ✅ | Auto-upload and auto-delete |
| Docker deployment | ✅ | Containerized on Render |
| Upstash Redis | ✅ | Cloud Redis with SSL |
| CleverCloud MySQL | ✅ | Cloud MySQL free tier |

---

*Pushkal Sharma | https://github.com/PushkalSharma0907 | devvratsharma0907@gmail.com*
