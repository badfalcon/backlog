# Backlog PR Viewer for IntelliJ

![Build](https://github.com/badfalcon/backlog/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/PLUGIN_ID.svg)](https://plugins.jetbrains.com/plugin/PLUGIN_ID)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Adjust the [pluginGroup](./gradle.properties), [plugin ID](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [x] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `PLUGIN_ID` in the above README badges.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
[日本語](#概要) | [English](#overview)

### 概要
このプラグインは、IntelliJ IDEA上でBacklogのプルリクエスト（PR）を直接確認できるようにするものです。   
IDEを離れることなく、効率的にコードレビューを行うことができます。

### 主要機能
- IntelliJ IDEA内でBacklogのPRリストの表示
- PRの詳細情報の閲覧
- コミットごとの変更点の確認
- ファイルごとの変更点の確認

### 今後追加予定の機能
- コメントの表示と追加

### インストール方法 (マーケットプレイス)
1. IntelliJ IDEAを開きます。
1. 「設定/環境設定」を開きます。
1. 「プラグイン」セクションに移動します。
1. 「マーケットプレイス」タブで "Backlog PR Viewer" を検索します。
1. 「インストール」ボタンをクリックします。
1. IDEを再起動して、プラグインを有効にします。

### インストール方法 (手動)
1. [最新のリリース](https://github.com/badfalcon/backlog/releases/latest)をダウンロードします。
2. IntelliJ IDEAを開きます。
3. <kbd>設定/環境設定</kbd> > <kbd>プラグイン</kbd> > <kbd>⚙️</kbd> > <kbd>ディスクからプラグインをインストール...</kbd> の順に選択します。
4. ダウンロードしたプラグインファイルを選択してインストールします。
5. IDEを再起動して、プラグインを有効にします。

### 初期設定
1. IDEの「設定/環境設定」を開きます。
1. 「ツール」セクションに移動し、「Backlog」サブセクションを選択します。
1. Backlogの認証情報（サーバーURL、APIキー、プロジェクトキーなど）を入力します。
1. 「適用」をクリックし、設定を保存します。

### 使用方法
1. IDEのツールバーまたはメニューから「Backlog PR Viewer」を開きます。
1. PRをクリックして詳細を確認します。
1. 「コミット」タブを選択し、各コミットの変更点を確認します。
1. 「ファイル」タブを選択し、ファイルごとの変更点を確認します。

### 設定
- BacklogのワークスペースのURL
- APIキー
- プロジェクト名

### サポート
問題や提案がある場合は、GitHubのIssueを作成してください。

### ライセンス
このプラグインは[Apache License 2.0]の下で公開されています。詳細は`LICENSE`ファイルを参照してください。

---

### Overview
This plugin allows you to view Backlog pull requests (PRs) directly within IntelliJ IDEA. It enables efficient code review without leaving your IDE.

### Key Features
- Display Backlog PR list within IntelliJ IDEA
- View PR details
- Check changes for each commit
- Review file-by-file changes

### Planned Features
- View and add comments

### Installation (from Marketplace)
1. Open IntelliJ IDEA.
2. Go to Settings/Preferences.
3. Navigate to the "Plugins" section.
4. Search for "Backlog PR Viewer" in the Marketplace tab.
5. Click "Install".
6. Restart the IDE to activate the plugin.

### Installation (Manually)
1. Download the [latest release](https://github.com/badfalcon/backlog/releases/latest).
2. Open IntelliJ IDEA.
3. Go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
4. Select the downloaded plugin file to install it.
5. Restart the IDE to activate the plugin.

### Initial Setup
1. Open the IDE Settings/Preferences.
2. Navigate to the "Tools" section and select the "Backlog" subsection.
3. Enter your Backlog authentication details (server URL, API key, project key, etc.).
4. Click "Apply" to save the settings.

### Usage
1. Open "Backlog PR Viewer" from the IDE toolbar or menu.
2. Select a project to view the PR list.
3. Click on a PR to view its details.
4. Select the "Commits" tab to review changes for each commit.
5. Select the "Files" tab to review file-by-file changes.

### Configuration
- Workspace Url
- API key
- Project name

[//]: # (todo あっているか確認)

### Support
If you encounter any problems or have suggestions, please create an Issue on GitHub.

### License
This plugin is released under the [Apache License 2.0]. See the `LICENSE` file for details.

<!-- Plugin description end -->

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
[Apache License 2.0]: https://www.apache.org/licenses/LICENSE-2.0