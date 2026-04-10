# Data Source Configuration

## Default (H2 In-Memory)

The starter works out of the box with H2 in-memory:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:fluffydb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Hibernate 6 (bundled with Spring Boot 3.4) auto-detects the database dialect, so there is no need to specify `database-platform`.

## Production Database

Override with any JPA-compatible database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate
```

The `job_execution` table is auto-created by Hibernate when `ddl-auto=create` or `create-drop`.
