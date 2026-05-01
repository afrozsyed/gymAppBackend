# Gym CRM — Backend API

Spring Boot REST API for the Gym CRM SaaS application. Handles authentication, member management, plans, dashboard analytics, payment recording, automated reminders, and Super Admin gym management.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Security | Spring Security 6 + JWT (JJWT 0.12.x) |
| ORM | Spring Data JPA (Hibernate) |
| Database | PostgreSQL 16 |
| Mapping | MapStruct 1.6 |
| Boilerplate | Lombok |
| Scheduler | Spring `@Scheduled` |
| Messaging | Twilio (WhatsApp primary, SMS fallback) |

---

## Project Structure

```
src/main/java/com/gymcrm/
├── GymCrmApplication.java
├── config/
│   ├── JwtConfig.java              # JWT secret + expiry from env
│   ├── SecurityConfig.java         # Filter chain, CORS, BCrypt, role-based rules
│   └── TwilioConfig.java           # Twilio credentials + @PostConstruct init
├── multitenancy/
│   ├── GymContext.java             # ThreadLocal<Long> gymId holder
│   └── GymContextFilter.java       # Sets/clears gymId per request (skipped for SUPER_ADMIN)
├── security/
│   ├── JwtUtil.java                # Token generate / validate / extract (gymId optional)
│   ├── JwtAuthFilter.java          # Validates Bearer token; sets gymId attr only if present
│   └── CustomUserDetailsService.java
├── domain/                         # JPA entities
│   ├── Gym.java                    # Status enum: ACTIVE | INACTIVE
│   ├── User.java                   # Role enum: ADMIN | STAFF | SUPER_ADMIN; phone field; gymId nullable
│   ├── Plan.java
│   ├── Member.java                 # PaymentStatus: PAID | PENDING
│   ├── Payment.java
│   └── ReminderLog.java
├── repository/                     # Spring Data JPA repos (all gym-scoped)
├── dto/
│   ├── request/                    # Validated inbound payloads
│   │   ├── LoginRequest.java
│   │   ├── CreateGymRequest.java   # Super Admin: create gym + owner
│   │   ├── MemberRequest.java
│   │   ├── PlanRequest.java
│   │   ├── PaymentRequest.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   └── ResetPasswordRequest.java
│   └── response/                   # Outbound shapes (no entity exposure)
│       ├── AuthResponse.java       # token, role, name, gymName (null for SUPER_ADMIN)
│       ├── GymDetailResponse.java  # Super Admin gym list item
│       ├── UserProfileResponse.java
│       ├── MemberResponse.java
│       ├── PlanResponse.java
│       ├── PaymentResponse.java
│       └── DashboardResponse.java
├── mapper/                         # MapStruct interfaces
├── service/
│   ├── AuthService.java            # login only (register removed)
│   ├── AdminService.java           # SUPER_ADMIN: create/list/activate/deactivate gyms, reset password
│   ├── UserProfileService.java     # Profile view/update + change password (all roles)
│   ├── MemberService.java
│   ├── PlanService.java
│   ├── PaymentService.java         # Record payment + renewal logic
│   ├── DashboardService.java
│   ├── ReminderService.java        # Manual reminder → Twilio WhatsApp/SMS
│   ├── SchedulerService.java       # Daily 08:00 auto-reminders
│   └── MessagingService.java       # Twilio: WhatsApp primary, SMS fallback
├── controller/
│   ├── AuthController.java         # POST /api/auth/login only
│   ├── AdminController.java        # /api/admin/gyms/** (SUPER_ADMIN only)
│   ├── UserProfileController.java  # /api/profile (all authenticated roles)
│   ├── MemberController.java
│   ├── PlanController.java
│   ├── PaymentController.java
│   ├── DashboardController.java
│   └── ReminderController.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    ├── DuplicateEmailException.java
    ├── GymInactiveException.java   # Thrown on login when gym status = INACTIVE
    └── GymContextException.java
```

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL 16 running on port 5432

### 1. Create the database

```sql
CREATE DATABASE gymcrm;
```

Then run the schema and migrations in order:

```bash
psql -U postgres -d gymcrm -f ../db/00_create_schema.sql
psql -U postgres -d gymcrm -f ../db/01_seed_data.sql        # optional demo data
psql -U postgres -d gymcrm -f ../db/02_add_payment_fields.sql
psql -U postgres -d gymcrm -f ../db/03_super_admin_and_gym_status.sql
```

Migration 03 seeds the Super Admin account:
- **Email:** `superadmin@gymcrm.com`
- **Initial password:** `password` — **change immediately** via Profile > Change Password

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env`:

```env
DB_URL=jdbc:postgresql://localhost:5432/gymcrm
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Generate: openssl rand -hex 32
JWT_SECRET=your_64_hex_char_secret_here
JWT_EXPIRATION_MS=86400000

SPRING_PROFILES_ACTIVE=dev

# Twilio — set TWILIO_ENABLED=true to send real WhatsApp/SMS messages
# Leave false (default) to run in mock/log-only mode
TWILIO_ENABLED=false
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
TWILIO_SMS_FROM=+14155238886
```

### 3. Start the server

```bash
mvn spring-boot:run
```

Server starts on **http://localhost:8080**

### Docker (Backend + Postgres together)

```bash
cp .env.example .env   # fill in values first
docker-compose up --build
```

The `postgres` service auto-applies `../db/00_create_schema.sql` on first run.

---

## Roles

| Role | Description |
|---|---|
| `SUPER_ADMIN` | Platform administrator. No gym assigned. Can create gyms, activate/deactivate them, and reset gym admin passwords. Accesses `/api/admin/**` only. |
| `ADMIN` | Gym owner/manager. Full access to their gym's data. Can edit their own name and gym name via profile. |
| `STAFF` | Gym staff. Same data access as ADMIN but cannot edit the gym name. |

---

## Multi-Tenancy Architecture

Every gym's data is isolated at the row level. The `gym_id` column exists on every table.

**How it works:**

1. On login, the JWT is issued containing `gymId` as a claim (omitted for `SUPER_ADMIN`).
2. `JwtAuthFilter` validates the token and stores `gymId` as a request attribute — only when present.
3. `GymContextFilter` reads that attribute and puts `gymId` into a `ThreadLocal`. If `gymId` is absent (SUPER_ADMIN), the filter skips silently.
4. Every gym-scoped service method calls `GymContext.get()` to scope all queries — it never accepts `gymId` as a parameter from the controller.
5. `GymContextFilter` always clears the `ThreadLocal` in a `finally` block to prevent leaks.
6. `AdminService` and `UserProfileService` do **not** call `GymContext.get()` — they are not gym-scoped.

**Rule:** `gymId` is NEVER accepted from the request body or query params. Any JSON field named `gymId` is silently ignored.

---

## Authentication

All endpoints except `POST /api/auth/login` require a `Bearer` token.

```
Authorization: Bearer <jwt_token>
```

**JWT payload for gym users (ADMIN / STAFF):**
```json
{
  "sub": "admin@fitzone.com",
  "gymId": 1,
  "role": "ADMIN",
  "iat": 1714300000,
  "exp": 1714386400
}
```

**JWT payload for Super Admin:**
```json
{
  "sub": "superadmin@gymcrm.com",
  "role": "SUPER_ADMIN",
  "iat": 1714300000,
  "exp": 1714386400
}
```

Note: `gymId` is absent from the SUPER_ADMIN token. `GymContextFilter` handles this gracefully.

---

## API Reference

### Base URL: `http://localhost:8080`

---

### Auth

#### POST `/api/auth/login`

Public endpoint — no token required.

**Request:**
```json
{
  "email": "rahul@fitzone.com",
  "password": "secret123"
}
```

**Response `200 OK` (gym user):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "ADMIN",
  "name": "Rahul Sharma",
  "gymName": "FitZone Gym"
}
```

**Response `200 OK` (Super Admin):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "SUPER_ADMIN",
  "name": "Super Admin",
  "gymName": null
}
```

**Error responses:**
| Status | Reason |
|---|---|
| `401` | Invalid email or password |
| `403` | Gym is deactivated — `{"message": "Your gym account is deactivated. Please contact admin."}` |

---

### Super Admin APIs

> Requires: `Authorization: Bearer <token>` with role `SUPER_ADMIN`

All endpoints under `/api/admin/**` are locked to `SUPER_ADMIN`. Any other role receives `403`.

#### POST `/api/admin/gyms`

Creates a new gym and its admin user atomically.

**Request:**
```json
{
  "gymName": "FitZone Gym",
  "ownerName": "Rahul Sharma",
  "email": "rahul@fitzone.com",
  "phone": "+91-9876543210",
  "password": "secret123"
}
```

**Response `201 Created`:**
```json
{
  "gymId": 3,
  "gymName": "FitZone Gym",
  "gymPhone": null,
  "status": "ACTIVE",
  "ownerName": "Rahul Sharma",
  "ownerEmail": "rahul@fitzone.com",
  "ownerPhone": "+91-9876543210",
  "createdAt": "2026-05-01T10:00:00"
}
```

**Error responses:**
- `400` — validation failure
- `409` — email already registered

---

#### GET `/api/admin/gyms`

Returns all gyms with their owner details and status.

**Response `200 OK`:** Array of gym detail objects (same shape as create response).

---

#### PUT `/api/admin/gyms/{id}/activate`

Sets gym status to `ACTIVE`. Returns updated gym detail object.

---

#### PUT `/api/admin/gyms/{id}/deactivate`

Sets gym status to `INACTIVE`. Members of this gym will be blocked from logging in with a `403` error. Returns updated gym detail object.

---

#### PUT `/api/admin/gyms/{id}/reset-password`

Resets the gym's ADMIN user password without requiring the old password.

**Request:**
```json
{
  "newPassword": "newSecret123"
}
```

**Response `204 No Content`**

---

### Profile APIs

> Requires: `Authorization: Bearer <token>` (any role)

#### GET `/api/profile`

Returns the current user's profile.

**Response `200 OK`:**
```json
{
  "id": 5,
  "name": "Rahul Sharma",
  "email": "rahul@fitzone.com",
  "phone": "+91-9876543210",
  "role": "ADMIN",
  "gymName": "FitZone Gym"
}
```

Note: `gymName` is `null` for `SUPER_ADMIN`.

---

#### PUT `/api/profile`

Updates name (all roles) and gym name (ADMIN role only).

**Request:**
```json
{
  "name": "Rahul Sharma",
  "gymName": "FitZone Premium Gym"
}
```

`gymName` is ignored for `STAFF` and `SUPER_ADMIN`.

**Response `200 OK`:** Updated profile object (same shape as GET).

---

#### PUT `/api/profile/password`

Changes the current user's password. Requires the correct old password.

**Request:**
```json
{
  "oldPassword": "secret123",
  "newPassword": "newSecret456",
  "confirmPassword": "newSecret456"
}
```

**Error responses:**
- `400` — old password incorrect / new ≠ confirm / new same as old

**Response `204 No Content`**

---

### Dashboard API

> Requires: `Authorization: Bearer <token>` (ADMIN / STAFF)

#### GET `/api/dashboard`

Returns an overview of the gym's current state.

**Response `200 OK`:**
```json
{
  "expiredCount": 3,
  "expiringToday": 1,
  "pendingPayments": 4500.00,
  "totalMembers": 42,
  "alertMembers": [
    {
      "id": 2,
      "name": "Priya Sharma",
      "phone": "+91-9234567890",
      "joinDate": "2026-03-25",
      "plan": { "id": 1, "name": "Monthly", "durationDays": 30, "price": 1500.00 },
      "expiryDate": "2026-04-24",
      "paymentStatus": "PAID",
      "status": "EXPIRED",
      "createdAt": "2026-03-25T09:15:00"
    }
  ]
}
```

**Member `status` values:** `ACTIVE` | `EXPIRING_TODAY` | `EXPIRED`

`alertMembers` shows up to 20 members who are expired, expiring today, or have pending payments.

---

### Member APIs

> Requires: `Authorization: Bearer <token>` (ADMIN / STAFF)

All member endpoints are automatically scoped to the authenticated gym.

#### GET `/api/members`

Paginated list of all members.

**Query params:** `page` (default 0), `size` (default 20), `sort` (default `createdAt,desc`)

**Response `200 OK`:** `{ content: [...], totalElements, totalPages, number, size }`

---

#### GET `/api/members/{id}`

Single member. Returns `404` if not found or belongs to a different gym.

---

#### POST `/api/members`

**Request:**
```json
{
  "name": "Arjun Singh",
  "phone": "+91-9345678901",
  "joinDate": "2026-04-29",
  "planId": 2,
  "paymentStatus": "PAID"
}
```

`expiryDate` is computed as `joinDate + plan.durationDays`. Never accepted from the client.

**Response `201 Created`:** Full member object.

---

#### PUT `/api/members/{id}`

Same body as POST. Recalculates `expiryDate` if plan or join date changes.

**Response `200 OK`:** Updated member object.

---

#### DELETE `/api/members/{id}`

**Response `204 No Content`**

---

### Plan APIs

> Requires: `Authorization: Bearer <token>` (ADMIN / STAFF)

#### GET `/api/plans`

All plans for the gym (no pagination).

#### POST `/api/plans`

```json
{ "name": "Quarterly", "durationDays": 90, "price": 4000.00 }
```

**Response `201 Created`:** Plan object with `id`.

#### DELETE `/api/plans/{id}`

**Response `204 No Content`**

---

### Payment APIs

> Requires: `Authorization: Bearer <token>` (ADMIN / STAFF)

#### POST `/api/members/{id}/payments`

Records a payment and **renews the membership**. Updates the member's plan (upgrade/downgrade/same), recalculates expiry as `today + new plan duration`, sets payment status to `PAID`.

**Request:**
```json
{
  "planId": 2,
  "amount": 4000.00,
  "paymentMode": "UPI",
  "notes": "Paid via Google Pay — receipt #1234"
}
```

`paymentMode` values: `CASH` | `CARD` | `UPI` | `BANK_TRANSFER` | `OTHER`

**Response `200 OK`:**
```json
{
  "id": 7,
  "amount": 4000.00,
  "paymentMode": "UPI",
  "notes": "Paid via Google Pay — receipt #1234",
  "paymentDate": "2026-05-01",
  "status": "COMPLETED",
  "planName": "Quarterly"
}
```

---

#### GET `/api/members/{id}/payments`

Full payment history for a member. Returns array of payment objects.

---

### Reminder API

> Requires: `Authorization: Bearer <token>` (ADMIN / STAFF)

#### POST `/api/reminders/send/{memberId}`

Manually triggers a WhatsApp or SMS reminder for a specific member and logs it.

- `TWILIO_ENABLED=true` — sends WhatsApp first, falls back to SMS on failure
- `TWILIO_ENABLED=false` (default) — logs only, safe for dev

**Response `200 OK`:** `{ "message": "Reminder sent successfully" }`

---

## Scheduler

A background job runs daily at **08:00 AM** and sends WhatsApp/SMS reminders (via Twilio) to all members expiring today or tomorrow across all gyms. Logs each to `reminder_logs`.

Cron expression: `0 0 8 * * *`

---

## Error Response Format

```json
{
  "timestamp": "2026-05-01T08:30:00",
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "must not be blank",
    "planId": "must not be null"
  }
}
```

`errors` is only present for validation failures. All other errors only include `timestamp`, `status`, and `message`.

**HTTP status codes:**

| Code | Meaning |
|---|---|
| `200` | OK |
| `201` | Created |
| `204` | No Content (DELETE / password change) |
| `400` | Validation error or bad request (e.g. wrong old password) |
| `401` | Invalid / missing token or wrong credentials |
| `403` | Forbidden — wrong role, or gym is deactivated |
| `404` | Resource not found |
| `409` | Conflict (duplicate email) |
| `500` | Unexpected server error |

---

## Database Schema (Summary)

```
gym           id, name, phone, status, created_at
users         id, gym_id (nullable), name, email, password, phone, role, created_at
plans         id, gym_id, name, duration_days, price, created_at
members       id, gym_id, name, phone, join_date, plan_id, expiry_date, payment_status, created_at
payments      id, gym_id, member_id, plan_id, amount, payment_date, status, payment_mode, notes, created_at
reminder_logs id, gym_id, member_id, message_type, sent_at
```

**Key schema notes:**
- `gym.status` — `ACTIVE` (default) or `INACTIVE`. Inactive gyms are blocked at login.
- `users.gym_id` — nullable. `SUPER_ADMIN` has no gym and `gym_id = NULL`.
- `users.phone` — optional, used in profile display.

**DB migrations — run in order:**

| File | Purpose |
|---|---|
| `db/00_create_schema.sql` | Initial schema — all tables |
| `db/01_seed_data.sql` | Demo data (optional) |
| `db/02_add_payment_fields.sql` | Adds `payment_mode`, `notes`, `plan_id` to `payments` |
| `db/03_super_admin_and_gym_status.sql` | Nullable `gym_id`, `phone` on users, `SUPER_ADMIN` role, `status` on gym, Super Admin seed |

```bash
psql -U postgres -d gymcrm -f ../db/00_create_schema.sql
psql -U postgres -d gymcrm -f ../db/01_seed_data.sql
psql -U postgres -d gymcrm -f ../db/02_add_payment_fields.sql
psql -U postgres -d gymcrm -f ../db/03_super_admin_and_gym_status.sql
```

---

## Security Notes

- Passwords are hashed with **BCrypt strength 12**.
- JWT secret must be at least 32 bytes (64 hex chars). Generate with: `openssl rand -hex 32`
- `gymId` is embedded in the JWT at login time and is read-only — it cannot be forged without the server secret.
- `SUPER_ADMIN` tokens contain no `gymId` claim. `GymContextFilter` handles this gracefully.
- Stack traces are never returned in API error responses.
- `ddl-auto` is set to `none` — Hibernate never modifies the schema.
