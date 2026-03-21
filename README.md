# 📒 Smart Contact Manager

A full-stack **Spring Boot** web application for managing personal contacts — built with modern Java features including **Java 21 Virtual Threads**, **Redis caching**, **OAuth2 Google login**, **Cloudinary** image storage, and a role-based **Admin Panel**.

---

## 🚀 Live Features

- 🔐 **Authentication** — Form login + Google OAuth2
- 📧 **Email Verification** — Token-based account activation (24hr expiry)
- 🔑 **Forgot Password** — OTP-based reset via email (10 min expiry)
- 📒 **Contact Management** — Add, view, update, delete contacts with image upload
- 🔍 **Live Search** — Real-time contact search via REST API
- 🖼️ **Cloudinary** — Contact & profile image upload/delete
- ⚡ **Redis Cache** — Contact caching with TTL for fast pagination
- 🧵 **Virtual Threads** — Java 21 virtual threads for all I/O operations
- 🛡️ **Admin Panel** — User management, role control, enable/disable accounts
- 📄 **Pagination** — Server-side paginated contact listing

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring MVC |
| Security | Spring Security, BCrypt, OAuth2 (Google) |
| Database | MySQL + Spring Data JPA |
| Cache | Redis (TTL-based contact cache) |
| Storage | Cloudinary (images) |
| Email | JavaMail (MimeMessage, HTML emails, async via virtual threads) |
| Threading | Java 21 Virtual Threads + Fixed CPU thread pool |
| Frontend | Thymeleaf, Bootstrap 5, jQuery, SweetAlert2 |
| Build | Maven |

---

## ⚡ Virtual Threads — How It's Used

This project uses **Java 21 Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) for all I/O-bound operations — database calls, email sending, Redis operations, and Cloudinary uploads.

A fixed thread pool is used for CPU-bound operations like **BCrypt password hashing**.

```java
// threadConfig.java
@Bean(name = "io")
public ExecutorService ioBoundThreadPool() {
    return Executors.newVirtualThreadPerTaskExecutor(); // Virtual Threads
}

@Bean(name = "cpu")
public ExecutorService cpuBoundThreadPool() {
    int cores = Runtime.getRuntime().availableProcessors();
    return Executors.newFixedThreadPool(cores); // CPU-bound (BCrypt)
}
```

| Operation | Thread Pool | Why |
|---|---|---|
| DB queries (JPA) | `io` — Virtual Thread | I/O bound |
| Redis get/set | `io` — Virtual Thread | I/O bound |
| Email sending | `io` — Virtual Thread | I/O bound |
| Cloudinary upload | `io` — Virtual Thread | I/O bound |
| BCrypt hashing | `cpu` — Fixed Pool | CPU bound |

---

## 📧 JavaMail — How It's Used

All emails are sent asynchronously using **JavaMail** (`MimeMessage` + `MimeMessageHelper`) via the virtual thread pool — so email sending never blocks the main request thread.

```java
// EmailService.java
public Future<Boolean> sendSimpleEmail(String toEmail, String subject, String body) {
    return ioExecutor.submit(() -> {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(domainName);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, true); // true = HTML email
        mailSender.send(message);
        return true;
    });
}
```

**Emails sent by the app:**

| Trigger | Email Type |
|---|---|
| New registration | Email verification link (token, 24hr expiry) |
| Forgot password | OTP (4-digit, 10 min expiry) |

**Gmail setup** — use App Password (not your main password):
> Google Account → Security → 2-Step Verification → App Passwords

---

## 🗄️ Redis Caching — How It's Used

Contacts are cached in Redis with a **30-minute TTL** to reduce database load on paginated contact pages.

```
Cache Strategy: Cache-Aside Pattern
Key format   : contact:user:{userId}:{contactId}
TTL          : 30 minutes
```

**Flow:**
```
Request → Check Redis → HIT  → Return cached contacts
                      → MISS → Fetch from DB → Save to Redis → Return
```

On **add / update / delete** — Redis cache is invalidated/updated immediately to stay consistent with DB.

---

## 🔐 OAuth2 — Admin Role Fix

When a user logs in via Google OAuth2, Spring Security sets authorities from Google (always `ROLE_USER`) — not from the database. To fix this, `OAuthSuccessHandler` manually updates the `SecurityContext` with the DB role after login:

```java
// OAuthSuccessHandler.java
List<SimpleGrantedAuthority> authorities = List.of(
    new SimpleGrantedAuthority(existingUser.getRole()) // DB se sahi role
);
UsernamePasswordAuthenticationToken newAuth =
    new UsernamePasswordAuthenticationToken(
        authentication.getPrincipal(),
        authentication.getCredentials(),
        authorities
    );
SecurityContextHolder.getContext().setAuthentication(newAuth);
```

This ensures an admin user logging in via Google is correctly redirected to `/admin/dashboard`.

---

## 📁 Project Structure

```
src/main/
├── java/com/smart/smartcontactmanager/
│   ├── config/
│   │   ├── Security.java                  # Spring Security config
│   │   ├── CustomLoginSuccessHandler.java # Role-based redirect on login
│   │   ├── OAuthSuccessHandler.java       # Google OAuth2 handler
│   │   ├── AuthFailtureHandler.java       # Login failure handler
│   │   ├── CustomerUserDetails.java       # Custom UserDetails
│   │   ├── UserDetailsServiceImpl.java    # UserDetailsService impl
│   │   ├── CloudinaryConfig.java          # Cloudinary bean
│   │   ├── RedisConfig.java               # Redis template config
│   │   └── threadConfig.java              # Virtual + CPU thread pools
│   ├── controller/
│   │   ├── homeController.java            # Public pages, registration
│   │   ├── userController.java            # Contact CRUD, profile
│   │   ├── AdminController.java           # Admin panel
│   │   ├── AuthController.java            # Email verification
│   │   ├── ForgotController.java          # OTP password reset
│   │   └── searchController.java          # Live search REST endpoint
│   ├── dao/
│   │   ├── userRepo.java                  # User JPA repository
│   │   └── ContactRepo.java               # Contact JPA repository
│   ├── entities/
│   │   ├── user.java                      # User entity
│   │   └── contact.java                   # Contact entity
│   ├── service/
│   │   ├── EmailService.java              # Async email via virtual threads
│   │   ├── ContactServiceThread.java      # Contact DB ops via virtual threads
│   │   ├── UserServiceThread.java         # User DB ops via virtual threads
│   │   └── RedisCacheService.java         # Redis cache operations
│   └── helper/
│       ├── EmailLinkVerification.java     # Email token link generator
│       ├── Message.java                   # Flash message helper
│       └── MessageType.java              # Message type enum
└── resources/
    ├── templates/
    │   ├── base.html                      # Public layout
    │   ├── normal/base.html               # Authenticated layout (sidebar)
    │   └── admin/base.html               # Admin layout
    ├── static/
    │   ├── css/style.css
    │   └── js/jscript.js
    └── application.properties
```

---

## ⚙️ Setup & Installation

### Prerequisites

- Java 21+
- Maven
- MySQL
- Redis (running on default port 6379)
- Cloudinary account
- Google OAuth2 credentials

### 1. Clone the repo

```bash
git clone https://github.com/PushkalSharma0907/Smart_Contact_Manager.git
cd Smart_Contact_Manager
```

### 2. Configure `application.properties`

```properties
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
```

### 3. Create MySQL database

```sql
CREATE DATABASE smartcontact;
```

### 4. Run the application

```bash
mvn spring-boot:run
```

App will start at **http://localhost:8080**

---

## 🛡️ Admin Panel Setup

Admin role is set manually in the database:

```sql
UPDATE USER SET role = 'ROLE_ADMIN' WHERE email = 'youremail@gmail.com';
```

Admin panel is accessible at `/admin/dashboard` after login.

**Admin features:**
- View all registered users with stats (total, active, disabled, contacts)
- Enable / Disable user accounts
- Change user roles (USER ↔ ADMIN)
- Delete users and all their contacts
- Live search across users

---

## 🔐 Security

- Passwords hashed with **BCrypt** (CPU thread pool)
- OAuth2 users get a **random UUID encoded password** — cannot be used for form login
- OAuth2 admin login — SecurityContext manually updated with DB role
- Role-based access: `ROLE_ADMIN > ROLE_USER` (Spring Security Role Hierarchy)
- Email verification required before first login
- OTP expires in **10 minutes**
- Email tokens expire in **24 hours**
- 403 Access Denied → custom error page

---

## 📸 Key Pages

| Route | Description |
|---|---|
| `/` | Home page |
| `/signup` | Registration with email verification |
| `/signin` | Login (form + Google) |
| `/forgot` | Forgot password (OTP flow) |
| `/user/index` | User dashboard |
| `/user/show-contacts/0` | Paginated contacts (Redis cached) |
| `/user/add-contact` | Add new contact (Cloudinary upload) |
| `/user/profile` | Profile page |
| `/user/settings` | Change password |
| `/admin/dashboard` | Admin dashboard |
| `/admin/users` | Manage all users |

---

## 👨‍💻 Designed & Developed by

<div align="center">

### Pushkal Sharma
**Computer Science & Engineering**

📧 [devvratsharma0907@gmail.com](mailto:devvratsharma0907@gmail.com)

</div>

---

## 📄 License

This project is for educational purposes.
