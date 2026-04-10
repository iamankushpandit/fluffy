# Contributing to Fluffy

Thank you for your interest in contributing to Fluffy! This document provides guidelines and rules for contributing to this project.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for everyone.

## How to Contribute

### Reporting Issues

- Use the [GitHub Issues](https://github.com/iamankushpandit/fluffy/issues) tab to report bugs or request features.
- Before opening a new issue, search existing issues to avoid duplicates.
- Provide a clear and descriptive title and include as much relevant information as possible.

### Submitting Changes

1. **Fork the repository** and create your branch from `main`.
2. **Create a feature branch** with a descriptive name:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the coding standards below.
4. **Write or update tests** for any new or changed functionality.
5. **Ensure all tests pass** before submitting:
   ```bash
   mvn clean test
   ```
6. **Commit your changes** with clear, descriptive commit messages.
7. **Push your branch** and open a Pull Request against `main`.

### Pull Request Rules

- Each pull request should address a single concern (bug fix, feature, refactor, etc.).
- Include a clear description of the changes and the motivation behind them.
- Reference any related issues using `Closes #<issue-number>` or `Fixes #<issue-number>`.
- All CI checks must pass before a pull request can be merged.
- At least one maintainer approval is required before merging.
- Keep pull requests small and focused to make reviews easier.

## Coding Standards

- **Java version**: 17
- **Framework**: Spring Boot 3.2
- **Build tool**: Maven
- Follow standard Java naming conventions and formatting.
- Write meaningful Javadoc for public APIs.
- Keep methods focused and concise.
- Prefer composition over inheritance.

## Project Structure

```
fluffy/
├── fluffy-batch-starter/   # Core library / Spring Boot starter
├── fluffy-batch-example/   # Example application demonstrating usage
├── docs/                   # Documentation
└── pom.xml                 # Parent POM
```

## Building the Project

```bash
# Build the project
mvn clean package

# Run tests only
mvn clean test
```

## License

By contributing to Fluffy, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
