# Fluffy

[![CI](https://github.com/iamankushpandit/fluffy/actions/workflows/ci.yml/badge.svg)](https://github.com/iamankushpandit/fluffy/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)

A lightweight Spring Boot starter for batch job processing. Fluffy provides a simple, annotation-driven API to define, launch, and monitor batch jobs with built-in concurrency management, retry support, and a REST API.

## Modules

| Module | Description |
|---|---|
| `fluffy-batch-starter` | Core library — Spring Boot auto-configuration, job engine, REST API |
| `fluffy-batch-example` | Example application demonstrating usage |

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+

### Build

```bash
mvn clean package
```

### Run Tests

```bash
mvn clean test
```

## Documentation

Detailed documentation is available in the [`docs/`](docs/) directory:

- [Architecture](docs/architecture.md)
- [Job Definition](docs/job-definition.md)
- [Concurrency](docs/concurrency.md)
- [Data Source](docs/datasource.md)
- [REST API](docs/rest-api.md)
- [Retry & Stop](docs/retry-stop.md)

## Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting a pull request.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
