# Academic Service — Server Setup Guide

End-to-end instructions for standing up a fresh school server (one tenant =
one server running `academic_service`). Covers prerequisites, secret
generation, `.env` creation, database bootstrap, and verification.

Audience: ops / sysadmin spinning up a new school instance.

---

## 1. Prerequisites

Install the following on the host (Ubuntu 22.04 / Debian 12 recommended;
macOS works for dev only).

| Tool          | Version       | Why                                          |
|---------------|---------------|----------------------------------------------|
| Java JDK      | 21 (Temurin)  | Runtime — Spring Boot 4.0.5 needs 21+        |
| Maven         | 3.9+          | Only if building from source on host         |
| MySQL Server  | 8.0+          | Primary datastore                            |
| Docker Engine | 24+           | If running via `docker compose` (preferred)  |
| Docker Compose| v2 plugin     | Bundled with Docker Desktop / `docker-compose-plugin` |
| Git           | any           | Pulling source                               |
| openssl       | any           | Generating secrets                           |
| curl          | any           | Smoke-testing endpoints                      |

### Quick install (Ubuntu)

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven mysql-server git curl openssl
# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER  # log out / back in
```

Verify:

```bash
java -version          # openjdk version "21..."
mvn -v                 # Apache Maven 3.9+
mysql --version        # 8.0+
docker --version
docker compose version
```

---

## 2. Clone the repo

```bash
git clone https://github.com/Neelavro/School-Managment-Academic-Service.git
cd School-Managment-Academic-Service
git checkout test-full-system
```

---

## 3. MySQL database & user

Log in as `root` (or any user with `CREATE USER`/`GRANT` privileges):

```bash
sudo mysql -u root -p
```

Create the database and a dedicated user. Replace `STRONG_DB_PASSWORD`
with the password you'll put into `.env` later.

```sql
CREATE DATABASE academic_service
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'academic_user'@'%' IDENTIFIED BY 'STRONG_DB_PASSWORD';
GRANT ALL PRIVILEGES ON academic_service.* TO 'academic_user'@'%';
FLUSH PRIVILEGES;
EXIT;
```

> If MySQL is on the same host as the app *and* you are NOT using Docker,
> bind `'academic_user'@'localhost'` instead of `'%'`.
>
> If the app runs in Docker and MySQL on the host, leave `'%'` and ensure
> MySQL listens on `0.0.0.0` (`bind-address` in `/etc/mysql/mysql.conf.d/mysqld.cnf`).

---

## 4. Generate secrets

The app needs two random secrets. Both go into `.env`. Run these commands
and save the output — you'll paste them in step 5.

### 4.1 JWT signing secret

Used to sign auth tokens. Must be ≥ 32 characters. Anything weaker is
rejected at startup.

```bash
openssl rand -base64 48
```

Example output (yours will be different):

```
gv4Z9o+Wq2k0e7L8Yb1m3X5Cn6P9R0T2sUiVjWkXlAmBnCpDqErFsGtHuIvJwK
```

### 4.2 Platform metrics shared secret

Used by `platform_admin_service` to authenticate the nightly
`/api/platform/metrics` poll. Same value goes into the platform admin's
per-school record.

```bash
openssl rand -hex 32
```

Example output:

```
4f1c2a3e8b9d0e1f2a3b4c5d6e7f8091a2b3c4d5e6f7081928374659708a1b2c
```

### 4.3 SSLCommerz credentials (production only)

You don't generate these — they come from your SSLCommerz merchant
dashboard. For dev/testing, the sandbox defaults (`testbox` / `qwerty`)
work out of the box.

---

## 5. Create the `.env` file

Copy the template and edit. The file lives next to `pom.xml` (it's
auto-loaded via `spring.config.import=optional:file:.env`).

```bash
cp .env.example .env
nano .env   # or vim / your editor of choice
```

Fill in:

```ini
# ── Database ──────────────────────────────────────────────────────────
DB_URL=jdbc:mysql://localhost:3306/academic_service
DB_USERNAME=academic_user
DB_PASSWORD=STRONG_DB_PASSWORD              # from step 3

# ── Server ────────────────────────────────────────────────────────────
SERVER_PORT=8084

# ── JWT ───────────────────────────────────────────────────────────────
JWT_SECRET=gv4Z9o+Wq2k0e7L8Yb1m3X5Cn6P9R0T2sUiVjWkXlAmBnCpDqErFsGtHuIvJwK
# ^^^ paste output of `openssl rand -base64 48` here

# ── SSLCommerz ────────────────────────────────────────────────────────
# Sandbox defaults — flip to your live credentials for production
SSLCOMMERZ_STORE_ID=testbox
SSLCOMMERZ_STORE_PASSWORD=qwerty
SSLCOMMERZ_SANDBOX=true

# ── Public URLs ───────────────────────────────────────────────────────
# Where the parent's browser lands after paying (frontend)
PUBLIC_BASE_URL_FRONTEND=https://school.example.com
# Where SSLCommerz POSTs the IPN — must be publicly reachable HTTPS
PUBLIC_BASE_URL_BACKEND=https://api.school.example.com

# ── Platform metrics shared secret ────────────────────────────────────
PLATFORM_METRICS_SECRET=4f1c2a3e8b9d0e1f2a3b4c5d6e7f8091a2b3c4d5e6f7081928374659708a1b2c
# ^^^ paste output of `openssl rand -hex 32` here
```

> **Important:** add the same `PLATFORM_METRICS_SECRET` value into the
> platform-admin side (`schools.metrics_secret` column for this school)
> so the nightly poller can authenticate.

> **Production:** set `SSLCOMMERZ_SANDBOX=false` and use live store
> credentials from your SSLCommerz dashboard.

### Permissions

```bash
chmod 600 .env
```

---

## 6. Run the SQL schema files

The schema is split into per-sidebar-section files under `sql/`. They
**must** be run in dependency order. `bootstrap.sql` first (foundation
tables); `accounts_schema.sql` last (depends on enrollments).

Every file is idempotent (`CREATE TABLE IF NOT EXISTS`) — safe to re-run.

### Option A: one shot from the shell

```bash
cd sql

for f in \
  bootstrap.sql \
  my_institute.sql \
  academic_structure.sql \
  students.sql \
  exams.sql \
  marking.sql \
  results.sql \
  downloads.sql \
  user_management.sql \
  hr_management.sql \
  attendance.sql \
  accounts_schema.sql
do
  echo "▶ running $f"
  mysql -u academic_user -p academic_service < "$f" || { echo "✗ $f failed"; exit 1; }
done
echo "✓ schema ready"
```

### Option B: interactive

```bash
mysql -u academic_user -p academic_service
```

then inside the prompt:

```sql
SOURCE sql/bootstrap.sql;
SOURCE sql/my_institute.sql;
SOURCE sql/academic_structure.sql;
SOURCE sql/students.sql;
SOURCE sql/exams.sql;
SOURCE sql/marking.sql;
SOURCE sql/results.sql;
SOURCE sql/downloads.sql;
SOURCE sql/user_management.sql;
SOURCE sql/hr_management.sql;
SOURCE sql/attendance.sql;
SOURCE sql/accounts_schema.sql;
```

### Verify

```bash
mysql -u academic_user -p academic_service -e "SHOW TABLES;" | wc -l
```

Expect ~70+ tables. If you get FK errors, you ran a file before its
prerequisite — re-run from the top (all files are idempotent).

---

## 7. Run the application

### Option A: Docker Compose (recommended for production)

The included `docker-compose.yml` reads `.env`, builds the image, and
expects MySQL to be reachable at `172.17.0.1:3306` (the default Docker
bridge host gateway on Linux). Adjust `SPRING_DATASOURCE_URL` inside the
compose file if your MySQL is elsewhere.

```bash
docker compose up -d --build
docker compose logs -f academic-service
```

Stop with `docker compose down`.

### Option B: Maven directly

```bash
./mvnw clean package -DskipTests
java -jar target/academic_service-0.0.1-SNAPSHOT.jar
```

Or for dev with hot reload:

```bash
./mvnw spring-boot:run
```

---

## 8. Verify the server is healthy

```bash
# Server is up
curl -i http://localhost:8084/actuator/health 2>/dev/null | head -1
# Expect: HTTP/1.1 200 OK   (or 404 if actuator not exposed — try below)

# Auth endpoint reachable (expect 400/401, NOT connection-refused)
curl -i -X POST http://localhost:8084/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{}'

# Platform metrics endpoint — should 401 without secret
curl -i http://localhost:8084/api/platform/metrics

# Platform metrics endpoint — should 200 with the secret you set
curl -i http://localhost:8084/api/platform/metrics \
     -H "X-Platform-Secret: $PLATFORM_METRICS_SECRET"
```

If `/api/platform/metrics` returns `503 Service Unavailable`, you forgot
to set `PLATFORM_METRICS_SECRET` in `.env` — fix and restart.

---

## 9. Production checklist

Before going live:

- [ ] `SSLCOMMERZ_SANDBOX=false` and live SSLCommerz credentials in `.env`
- [ ] `PUBLIC_BASE_URL_BACKEND` is a public HTTPS URL (SSLCommerz IPN
      cannot reach `localhost`)
- [ ] `JWT_SECRET` is unique per server (do NOT reuse across schools)
- [ ] `PLATFORM_METRICS_SECRET` matches the value stored on the
      platform-admin side for this school
- [ ] `.env` has `chmod 600` and is NOT committed to git
- [ ] MySQL `academic_user` has password auth, not socket auth
- [ ] Reverse proxy (nginx / Caddy) terminates TLS in front of port 8084
- [ ] Daily `mysqldump academic_service` backup configured
- [ ] Docker container restart policy is `unless-stopped` (already set
      in `docker-compose.yml`)
- [ ] Image-upload volume `/var/www/student-service-images` exists and
      has a backup policy

---

## 10. Quick reference — secrets cheat sheet

| Variable                  | How to generate                  | Length         |
|---------------------------|----------------------------------|----------------|
| `DB_PASSWORD`             | `openssl rand -base64 24`        | 32 char        |
| `JWT_SECRET`              | `openssl rand -base64 48`        | 64 char (≥32)  |
| `PLATFORM_METRICS_SECRET` | `openssl rand -hex 32`           | 64 char        |
| `SSLCOMMERZ_STORE_ID`     | from SSLCommerz dashboard        | merchant-given |
| `SSLCOMMERZ_STORE_PASSWORD` | from SSLCommerz dashboard      | merchant-given |

---

## 11. Troubleshooting

**App fails at startup with `Could not resolve placeholder 'JWT_SECRET'`**
The `.env` file is missing or not in the working directory. It must sit
next to `pom.xml` (or wherever you launch the jar from).

**`Access denied for user 'academic_user'@'...'`**
MySQL user host pattern doesn't match. Re-run the `CREATE USER` from
step 3 with the right host (`'%'` for Docker, `'localhost'` for host-only).

**`Table 'academic_service.X' doesn't exist` at runtime**
A SQL file was skipped. Re-run all 12 files from step 6 — they're
idempotent.

**SSLCommerz IPN never fires**
`PUBLIC_BASE_URL_BACKEND` is not publicly reachable. SSLCommerz cannot
hit `localhost` or a private IP. Use a real domain + HTTPS, or ngrok
during testing.

**Platform metrics returns 503**
`PLATFORM_METRICS_SECRET` is blank in `.env`. Set it and restart.

**Platform metrics returns 401**
The poller is sending the wrong secret in `X-Platform-Secret`. Make sure
the platform-admin side has the same value as this server's `.env`.
