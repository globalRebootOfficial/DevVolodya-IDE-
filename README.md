# DevVolodya IDE

Native Android IDE project using Kotlin and Jetpack Compose.

## Current milestone (0.2.0)

- Dark native Compose interface with Home, Projects, Editor, Git, Build and Settings sections.
- App-private workspace under the application's files directory.
- Create local projects with a starter README.
- Create nested project files such as src/Main.kt.
- Browse project files and edit/save UTF-8 text files up to 1 MiB.
- Canonical-path checks prevent project/file operations from escaping the workspace.
- Unit tests cover project creation, file round-tripping and path-traversal rejection.

## Build

The repository includes the Gradle Wrapper. The Bitrise build_apk workflow runs lint,
unit tests and the Android debug APK build. The package is com.devvolodya.ide,
with minSdk 26 and targetSdk 35.

## Not implemented yet

Git operations, an interactive PTY terminal, syntax highlighting/LSP, SAF access to arbitrary
external folders, compilers/toolchains, project APK packaging, ZIP/SH exporters, EXE
cross-compilation, Dev Hub networking, and Root adapters for su / KernelSU / Magisk / APatch.
No Windows EXE is run natively on Android. Root operations will require explicit confirmation
and least-privilege execution. No APK is considered ready until CI produces and verifies it.
