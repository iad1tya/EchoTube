# Contributing to EchoTube

Thank you for contributing.

## Before You Start

- Read `README.md` and `SECURITY.md`.
- Search existing issues and pull requests before opening a new one.
- Keep changes focused and scoped.

## Development Setup

1. Install JDK 17 and Android SDK.
2. Open project in Android Studio.
3. Sync Gradle.
4. Run:

```bash
./gradlew :app:installGithubDebug
```

## Branching and Commits

- Create feature branches from `main`.
- Use clear commit messages.
- Prefer small, reviewable commits.

Suggested commit style:

```text
type(scope): short summary
```

Examples:

- `feat(settings): add support email row in contact section`
- `fix(player): handle null stream metadata safely`

## Code Style

- Use Kotlin idioms and keep functions focused.
- Follow existing project style and naming.
- Avoid broad formatting-only changes unrelated to your task.
- Keep UI and behavior changes consistent with existing architecture.

## Testing Expectations

Before opening a pull request:

- Build app successfully.
- Verify the changed flow manually on an emulator or device.
- Run relevant tests if added/available.

Minimum check:

```bash
./gradlew :app:assembleGithubDebug
```

## Pull Request Checklist

- Explain what changed and why.
- Include testing notes.
- Add screenshots for UI changes.
- Keep PR title and description clear.
- Ensure CI passes.

## Reporting Bugs

Use the bug report template and include:

- device and Android version
- app version/build type
- clear reproduction steps
- expected vs actual behavior
- logs or screenshots when possible

## Feature Requests

Use the feature request template and include:

- problem statement
- proposed behavior
- alternatives considered
- optional mockups or examples

## Security Issues

Do not open public issues for sensitive vulnerabilities.

See `SECURITY.md` for responsible disclosure instructions.