# Contributing to KofeinoTracker

Dziękujemy za zainteresowanie rozwojem KofeinoTracker! :)
Contributions are welcome in both Polish and English.

## Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/your-username/KofeinoTracker.git
   ```
3. Set up the project — see [setup guide](docs/setup-guide.md) for Wear OS emulator configuration
4. Create a feature branch: `git checkout -b feature/amazing-feature`

## Development

### Architecture

```
wear/src/main/java/pl/dekrate/kofeino/
├── data/       # Room DAOs, Repositories, API services
├── domain/     # Domain models (POJOs/data classes)
├── di/         # Hilt modules
└── presentation/  # ViewModels, Screens (Compose), Navigation, Theme
```

### Coding conventions

- **Kotlin** with Jetpack Compose for UI
- **MVVM** pattern with `StateFlow` + `ViewModel`
- **Hilt** dependency injection
- Write **unit tests** with MockK + Turbine + Robolectric
- Write **UI tests** with Compose UI Test + Espresso
- Naming: `CamelCase` for classes, `camelCase` for functions/variables
- Format code with `ktfmt` or IntelliJ default Kotlin formatter

### Testing

```bash
# Unit tests
./gradlew :wear:testDebugUnitTest

# Instrumented tests (requires emulator)
./gradlew :wear:connectedDebugAndroidTest
```

## Pull Request Process

1. Ensure all tests pass
2. Update README.md / docs if adding a feature
3. Add or update tests for new functionality
4. Make sure the PR description clearly describes the change
5. PRs will be reviewed by maintainers

## Bug Reports

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md) — include:
- Steps to reproduce
- Expected vs actual behavior
- Environment (Wear OS version, device)
- Screenshots if applicable

## Feature Requests

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md).

## Code of Conduct

Be respectful, constructive, and welcoming to all contributors.
