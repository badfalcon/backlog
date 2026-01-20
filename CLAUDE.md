# CLAUDE.md - AI Assistant Guide for Backlog PR Viewer

This document provides comprehensive guidance for AI assistants working with the Backlog PR Viewer codebase.

## Project Overview

**Backlog PR Viewer** is an IntelliJ Platform Plugin that enables viewing [Backlog](https://nulab.com/backlog/) pull requests directly within JetBrains IDEs. It allows developers to review PRs, view commit history, and examine file changes without leaving the IDE.

- **Plugin ID**: `com.github.badfalcon.backlog`
- **Current Version**: 0.1.5
- **Repository**: https://github.com/badfalcon/backlog
- **JetBrains Marketplace**: https://plugins.jetbrains.com/plugin/25137
- **License**: Apache License 2.0

## Tech Stack

- **Language**: Kotlin
- **JVM Target**: 17
- **Build System**: Gradle (Kotlin DSL) with Version Catalog
- **Platform**: IntelliJ Platform SDK (2023.3+)
- **Key Dependencies**:
  - `backlog4j:2.6.0` - Backlog API client library
  - `Git4Idea` - Bundled IntelliJ Git plugin (required dependency)

## Project Structure

```
backlog/
├── src/
│   ├── main/
│   │   ├── kotlin/com/github/badfalcon/backlog/
│   │   │   ├── BacklogBundle.kt           # i18n message bundle
│   │   │   ├── config/                    # Settings/configuration
│   │   │   │   ├── BacklogSettingState.kt
│   │   │   │   ├── BacklogSettingsConfigurable.kt
│   │   │   │   └── BacklogSettingsComponent.kt
│   │   │   ├── icons/                     # Custom icons
│   │   │   │   └── BacklogIcons.kt
│   │   │   ├── notifier/                  # Event system
│   │   │   │   └── ToolWindowNotifier.kt
│   │   │   ├── service/                   # Core services
│   │   │   │   ├── BacklogService.kt
│   │   │   │   ├── GitService.kt
│   │   │   │   ├── PullRequestService.kt
│   │   │   │   └── ToolWindowService.kt
│   │   │   ├── tabs/                      # UI tabs
│   │   │   │   ├── BacklogHomeTab.kt
│   │   │   │   └── BacklogPRDetailTab.kt
│   │   │   ├── toolWindow/                # Tool window factory
│   │   │   │   └── BacklogToolWindowFactory.kt
│   │   │   └── util/                      # Utilities
│   │   │       └── BacklogMarkdownConverter.kt
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   ├── plugin.xml             # Plugin descriptor
│   │       │   └── pluginIcon*.svg        # Plugin icons
│   │       ├── icons/                     # Resource icons
│   │       └── messages/
│   │           └── BacklogBundle.properties  # i18n strings
│   └── test/
│       ├── kotlin/                        # Test sources
│       └── testData/                      # Test fixtures
├── gradle/
│   ├── libs.versions.toml                 # Version catalog
│   └── wrapper/
├── .github/
│   ├── workflows/
│   │   ├── build.yml                      # CI/CD pipeline
│   │   ├── release.yml                    # Release pipeline
│   │   └── run-ui-tests.yml               # UI tests
│   └── dependabot.yml                     # Dependency updates
├── build.gradle.kts                       # Build configuration
├── gradle.properties                      # Gradle properties
└── settings.gradle.kts                    # Settings
```

## Architecture

### Service Layer (Project-Level Services)

All services are `@Service(Service.Level.PROJECT)` - one instance per open project.

1. **BacklogService** (`service/BacklogService.kt`)
   - Manages Backlog API client initialization
   - Validates API credentials
   - Fetches pull requests and attachments from Backlog
   - Supports both `.backlog.com` and `.backlog.jp` domains

2. **GitService** (`service/GitService.kt`)
   - Interfaces with Git4Idea plugin
   - Monitors repository changes via `GitRepositoryChangeListener`
   - Fetches remotes and computes diffs between branches
   - Retrieves commit history

3. **PullRequestService** (`service/PullRequestService.kt`)
   - Orchestrates between BacklogService and GitService
   - Provides unified PR data (changes, commits, attachments)

4. **ToolWindowService** (`service/ToolWindowService.kt`)
   - Manages the IDE tool window
   - Creates and manages PR detail tabs
   - Handles diff viewing via IntelliJ's DiffManager
   - Uses coroutines for async operations

### Event System

Uses IntelliJ's MessageBus with a custom `UPDATE_TOPIC`:
- `ToolWindowNotifier` interface for publish/subscribe pattern
- Notifies when Backlog client or Git repository becomes ready
- Triggers PR list refresh

### Configuration

- **MyPluginSettingsState**: Persistent state stored in `BacklogSettings.xml`
  - `workspaceName`: Backlog workspace name
  - `apiKey`: Backlog API key
  - `projectName`: Target project
  - `topLevelDomain`: COM or JP

- Settings UI accessible via: Settings > Tools > Backlog

### UI Components

- **BacklogHomeTab**: Shows list of open PRs in a table
- **BacklogPRDetailTab**: Shows PR details with three sub-tabs:
  - Pull Request Details (description with markdown rendering)
  - File Changes (diff viewer)
  - Commits (commit history)

## Development Commands

### Build & Run

```bash
# Build the plugin
./gradlew buildPlugin

# Run plugin in sandbox IDE
./gradlew runIde

# Run tests
./gradlew test

# Run all checks
./gradlew check

# Verify plugin compatibility
./gradlew verifyPlugin

# Run code quality inspection (Qodana)
./gradlew qodanaScan
```

### Code Coverage

```bash
# Generate Kover coverage report
./gradlew koverXmlReport
# Report at: build/reports/kover/report.xml
```

### Gradle Tasks Reference

| Task | Description |
|------|-------------|
| `buildPlugin` | Build plugin distribution |
| `runIde` | Run IDE with plugin installed |
| `runIdeForUiTests` | Run IDE for UI testing |
| `test` | Run unit tests |
| `check` | Run all verification tasks |
| `verifyPlugin` | Verify plugin compatibility |
| `publishPlugin` | Publish to JetBrains Marketplace |
| `patchChangelog` | Update CHANGELOG.md |

## Testing

- Test framework: IntelliJ Platform Test Framework
- Test base class: `BasePlatformTestCase`
- Test data location: `src/test/testData/`

Run tests:
```bash
./gradlew test
```

## CI/CD

### Build Workflow (`.github/workflows/build.yml`)
Triggered on: push to `main`, all pull requests

Jobs:
1. **build** - Build plugin, prepare artifacts
2. **test** - Run tests, upload coverage to Codecov
3. **inspectCode** - Qodana static analysis
4. **verify** - IntelliJ Plugin Verifier
5. **releaseDraft** - Create GitHub draft release (main branch only)

### Release Workflow (`.github/workflows/release.yml`)
Triggered on: GitHub release (prereleased/released)

- Publishes plugin to JetBrains Marketplace
- Uploads release assets to GitHub
- Creates changelog PR

### Required Secrets
- `PUBLISH_TOKEN` - JetBrains Marketplace token
- `CERTIFICATE_CHAIN` - Plugin signing certificate
- `PRIVATE_KEY` - Plugin signing private key
- `PRIVATE_KEY_PASSWORD` - Private key password

## Code Conventions

### Kotlin Style
- Follow Kotlin coding conventions
- Use IntelliJ Platform API patterns
- Services should be project-level with `@Service(Service.Level.PROJECT)`

### Logging
- Use `thisLogger().warn("[backlog] " + message)` for debugging
- Prefix log messages with `[backlog]` for filtering

### Package Structure
```
com.github.badfalcon.backlog
├── config      # Settings and configuration
├── icons       # Icon constants
├── notifier    # Event/message system
├── service     # Business logic services
├── tabs        # UI tab components
├── toolWindow  # Tool window factory
└── util        # Utility classes
```

### Plugin Configuration
- Plugin descriptor: `src/main/resources/META-INF/plugin.xml`
- Resource bundle: `messages/BacklogBundle.properties`
- Persistent settings: `BacklogSettings.xml`

## Key APIs & Patterns

### Backlog API (backlog4j)
```kotlin
val client: BacklogClient = BacklogClientFactory(configure).newClient()
val pullRequests = client.getPullRequests(projectKey, repoId, params)
```

### Git4Idea Integration
```kotlin
val manager = GitUtil.getRepositoryManager(project)
val repository = manager.getRepositoryForRoot(rootDirectory)
GitHistoryUtils.history(project, root, "base..target")
GitChangeUtils.getDiff(project, root, revBase, revTarget, null)
```

### IntelliJ Platform Services
```kotlin
// Get service instance
val service = project.service<BacklogService>()

// Message bus pattern
val publisher = project.messageBus.syncPublisher(UPDATE_TOPIC)
publisher.update("message")
```

### Coroutines
```kotlin
cs.launch {
    withContext(Dispatchers.IO) {
        // Background work
    }
}
```

## Versioning

- Follows [SemVer](https://semver.org/)
- Version defined in `gradle.properties`: `pluginVersion`
- Platform compatibility: `pluginSinceBuild = 233` (IntelliJ 2023.3+)

## Common Tasks for AI Assistants

### Adding a New Feature
1. Identify which service layer the feature belongs to
2. Follow existing patterns for service/UI integration
3. Update `plugin.xml` if adding new extensions
4. Add i18n strings to `BacklogBundle.properties`
5. Write tests in `src/test/kotlin`

### Fixing Bugs
1. Reproduce the issue understanding the data flow
2. Check service initialization order (BacklogService > GitService > ToolWindowService)
3. Verify async operations use proper coroutine contexts
4. Test with both `.com` and `.jp` Backlog domains

### Updating Dependencies
1. Check `gradle/libs.versions.toml` for version catalog
2. Run `./gradlew verifyPlugin` after updates
3. Ensure compatibility with target IntelliJ versions

## Important Notes

- The plugin requires Git4Idea bundled plugin as a dependency
- Settings are stored per-project, not globally
- PR data is fetched by matching remote URLs to Backlog repositories
- Markdown rendering uses a custom converter for Backlog's markdown syntax
- Image attachments are embedded as Base64 in HTML
