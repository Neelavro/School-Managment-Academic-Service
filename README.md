# Academic Service

Spring Boot backend for the School Management System.

## Tech Stack

| | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Database | MySQL |

## Dependencies

| Dependency | Version |
|---|---|
| spring-boot-starter-web (webmvc) | managed by Boot 4.0.5 |
| spring-boot-starter-data-jpa | managed by Boot 4.0.5 |
| spring-boot-starter-security | managed by Boot 4.0.5 |
| spring-boot-starter-validation | managed by Boot 4.0.5 |
| spring-boot-starter-cache | managed by Boot 4.0.5 |
| spring-boot-starter-actuator | managed by Boot 4.0.5 |
| mysql-connector-j | managed by Boot 4.0.5 |
| lombok | managed by Boot 4.0.5 |
| jackson-databind | managed by Boot 4.0.5 |
| caffeine | managed by Boot 4.0.5 |
| jjwt-api / jjwt-impl / jjwt-jackson | 0.13.0 |
| playwright | 1.50.0 |
| metadata-extractor | 2.18.0 |

## Running Locally

```bash
./mvnw spring-boot:run
```

The service starts on port **8084**.

## Running with Docker

```bash
docker compose up
```
