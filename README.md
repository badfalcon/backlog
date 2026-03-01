# Backlog PR Viewer for IntelliJ

![Build](https://github.com/badfalcon/backlog/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/25137.svg)](https://plugins.jetbrains.com/plugin/25137)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/25137.svg)](https://plugins.jetbrains.com/plugin/25137)

[English](#overview) | [日本語](#概要)

<!-- Plugin description -->
### Overview
This plugin allows you to view [Backlog](https://nulab.com/backlog/) pull requests (PRs) directly within a JetBrains IDE. It enables efficient code review without leaving your IDE.

### Key Features
- Display Backlog PR list within the JetBrains IDE
- View PR details
- Check changes for each commit
- Review file-by-file changes

### Planned Features
- View and add comments
- Revamp the Tab Views
- OAuth2.0 Support
- I18n Support

### Installation (from Marketplace)
1. Open a JetBrains IDE.
2. Go to **Settings/Preferences**.
3. Navigate to the **Plugins** section.
4. Search for "Backlog PR Viewer" in the Marketplace tab.
5. Click **Install**.
6. Restart the IDE to activate the plugin.

### Installation (Manually)
1. Download the [latest release](https://github.com/badfalcon/backlog/releases/latest).
2. Open a JetBrains IDE.
3. Go to <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>
4. Select the downloaded plugin file to install it.
5. Restart the IDE to activate the plugin.

### Initial Setup
1. Open the IDE **Settings/Preferences**.
2. Navigate to **Tools** > **Backlog**.
3. Enter your Backlog authentication details:
   - **Workspace Name**: Your Backlog workspace name
   - **API Key**: Your Backlog API key
   - **Project Name**: The target Backlog project key
   - **Top Level Domain**: Select `.com` or `.jp`
4. Click **Apply** to save the settings.

### Usage
1. Open the **Backlog PR Viewer** tool window from the IDE toolbar or menu.
2. The PR list for your configured project will be displayed automatically.
3. Click on a PR to view its details.
4. Use the **Commits** tab to review changes per commit.
5. Use the **Files** tab to review file-by-file diffs.

### Support
If you encounter any problems or have suggestions, please [create an Issue on GitHub](https://github.com/badfalcon/backlog/issues).

### License
This plugin is released under the [Apache License 2.0]. See the `LICENSE` file for details.

---

### 概要
このプラグインは、JetBrains IDE上で[Backlog](https://backlog.com/ja/)のプルリクエスト（PR）を直接確認できるようにするものです。IDEを離れることなく、効率的にコードレビューを行うことができます。

### 主要機能
- JetBrains IDE内でBacklogのPRリストの表示
- PRの詳細情報の閲覧
- コミットごとの変更点の確認
- ファイルごとの変更点の確認

### 今後追加予定の機能
- コメントの表示と追加
- タブビューの見直し
- OAuth2.0のサポート
- 多言語対応

### インストール方法 (マーケットプレイス)
1. JetBrains IDEを開きます。
2. **設定/環境設定** を開きます。
3. **プラグイン** セクションに移動します。
4. **マーケットプレイス** タブで "Backlog PR Viewer" を検索します。
5. **インストール** ボタンをクリックします。
6. IDEを再起動して、プラグインを有効にします。

### インストール方法 (手動)
1. [最新のリリース](https://github.com/badfalcon/backlog/releases/latest)をダウンロードします。
2. JetBrains IDEを開きます。
3. <kbd>設定/環境設定</kbd> > <kbd>プラグイン</kbd> > <kbd>⚙️</kbd> > <kbd>ディスクからプラグインをインストール...</kbd> の順に選択します。
4. ダウンロードしたプラグインファイルを選択してインストールします。
5. IDEを再起動して、プラグインを有効にします。

### 初期設定
1. IDEの **設定/環境設定** を開きます。
2. **ツール** > **Backlog** を選択します。
3. Backlogの認証情報を入力します。
   - **ワークスペース名**: Backlogのワークスペース名
   - **APIキー**: BacklogのAPIキー
   - **プロジェクト名**: 対象プロジェクトのキー
   - **トップレベルドメイン**: `.com` または `.jp` を選択
4. **適用** をクリックして設定を保存します。

### 使用方法
1. IDEのツールバーまたはメニューから **Backlog PR Viewer** ツールウィンドウを開きます。
2. 設定したプロジェクトのPRリストが自動的に表示されます。
3. PRをクリックして詳細を確認します。
4. **コミット** タブを選択し、各コミットの変更点を確認します。
5. **ファイル** タブを選択し、ファイルごとの変更点を確認します。

### サポート
問題や提案がある場合は、[GitHubのIssue](https://github.com/badfalcon/backlog/issues)を作成してください。

### ライセンス
このプラグインは[Apache License 2.0]の下で公開されています。詳細は`LICENSE`ファイルを参照してください。

<!-- Plugin description end -->

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[Apache License 2.0]: https://www.apache.org/licenses/LICENSE-2.0
