# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Backlog PR Viewer** — IntelliJ Platform Plugin for viewing [Backlog](https://nulab.com/backlog/) pull requests within JetBrains IDEs. Review PRs, view commits, and examine file diffs without leaving the IDE.

- **Plugin ID**: `com.github.badfalcon.backlog`
- **Language**: Kotlin (JVM 17)
- **Platform**: IntelliJ 2023.3+ (`pluginSinceBuild = 233`, no upper bound)
- **Key dependency**: `backlog4j:2.6.0` (Backlog API client), `Git4Idea` (bundled IntelliJ plugin)

## Development Commands

```bash
./gradlew buildPlugin       # Build the plugin
./gradlew runIde            # Run plugin in sandbox IDE
./gradlew test              # Run tests
./gradlew check             # Run all checks (includes tests + Kover coverage)
./gradlew verifyPlugin      # Verify plugin compatibility with target IDEs
./gradlew koverXmlReport    # Generate coverage report (build/reports/kover/report.xml)
```

Single test class: `./gradlew test --tests "com.github.badfalcon.backlog.MyPluginTest"`

## Architecture

### Service Layer

All services are project-level (`@Service(Service.Level.PROJECT)`) — one instance per open project. Service access: `project.getService(BacklogService::class.java)`.

- **BacklogService** — Manages Backlog API client lifecycle, credential validation, fetching PRs and attachments. Supports `.backlog.com` and `.backlog.jp` domains.
- **GitService** — Wraps Git4Idea APIs. Monitors repo changes via `GitRepositoryChangeListener`, computes diffs, retrieves commit history.
- **PullRequestService** — Orchestrator bridging BacklogService and GitService. Provides unified PR data (changes, commits, attachments).
- **ToolWindowService** — Manages the IDE tool window and PR detail tabs. Uses coroutines (`Dispatchers.IO`) for async operations and IntelliJ's `DiffManager` for file diffs.

### Data Flow

1. Settings configured (workspace, API key, project, TLD) → BacklogService initializes API client
2. GitService detects remotes → matches remote URLs to Backlog repositories
3. `ToolWindowNotifier` publishes events via IntelliJ's MessageBus (`UPDATE_TOPIC`) when client or git repo becomes ready
4. BacklogHomeTab fetches and displays open PRs → user clicks → BacklogPRDetailTab shows details/diffs/commits

### Event System

Uses IntelliJ's MessageBus with `ToolWindowNotifier.UPDATE_TOPIC` for publish/subscribe. Events trigger when Backlog client initializes, git repository changes, or PR list needs refresh.

### Configuration

Persistent state in `BacklogSettings.xml` via `BacklogSettingState`: `workspaceName`, `apiKey`, `projectName`, `topLevelDomain` (COM/JP). Settings UI: **Settings > Tools > Backlog**.

## Key Conventions

- Log messages: `thisLogger().warn("[backlog] " + message)` — always prefix with `[backlog]`
- i18n strings go in `src/main/resources/messages/BacklogBundle.properties`
- New extensions must be registered in `src/main/resources/META-INF/plugin.xml`
- Dependency versions managed in `gradle/libs.versions.toml`
- Image attachments in PR descriptions are embedded as Base64 in HTML via `BacklogMarkdownConverter`
- Test base class: `BasePlatformTestCase` (IntelliJ Platform Test Framework)
