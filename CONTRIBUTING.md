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

## Cloud-Native Testing Mindset

Fluffy is designed to run in cloud-native environments (e.g., Kubernetes). Contributors are expected to adopt a cloud-native mindset when developing and testing changes.

### Testing Requirements

1. **Test locally first.** All changes must be validated locally before submitting a pull request:
   ```bash
   mvn clean test
   ```
2. **Test on a cloud environment when possible.** If you have access to a Kubernetes cluster or cloud platform, deploy and verify your changes there as well. The example application includes a Dockerfile and Kubernetes manifests under `fluffy-batch-starter/fluffy-batch-example/k8s/` to help with this:
   ```bash
   # Build the Docker image
   docker build -t fluffy-batch-example fluffy-batch-starter/fluffy-batch-example

   # Apply Kubernetes manifests
   kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/
   ```
3. **Contribute to the existing test setup.** Whenever possible, add your tests to the existing test suites and configurations. This keeps the project's test infrastructure consistent and maintainable.
4. **If new test configuration is required:**
   - Create the new configuration (e.g., new Kubernetes manifests, Docker Compose files, or Spring profiles) and include it in your pull request.
   - Provide clear evidence that your changes work as expected (e.g., screenshots, log output, or CI pipeline results).
   - Demonstrate that no existing functionality is broken by your changes (e.g., by showing passing existing tests alongside your new tests).
   - Document any new configuration in your pull request description so reviewers can reproduce the testing.

### What Counts as Evidence

When introducing new configurations or infrastructure changes, include at least one of the following in your pull request:

- Screenshots or screen recordings of the application running with your changes.
- Relevant log output showing successful execution.
- CI/CD pipeline results demonstrating passing tests.
- A brief write-up describing the manual verification steps you performed.

## Coding Standards

- **Java version**: 21
- **Framework**: Spring Boot 3.4
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
