# DevVolodya IDE

Native Android IDE project using Kotlin and Jetpack Compose.

## Current milestone (0.2.x)

- Dark native Compose interface with Home, Projects, Editor, Git, Build and Settings sections.
- App-private workspace under the application's files directory.
- Create local projects with a starter README.
- Create nested project files and directories.
- Browse project files and edit/save UTF-8 text files up to 1 MiB.
- Rename and delete project entries without allowing operations to escape the selected project.
- Canonical-path checks reject traversal and symbolic-link project roots; recursive deletion does not follow symbolic links.
- Unit tests cover project creation, file round-tripping, nested folders, rename/delete and path-traversal rejection.

## Build

The repository includes the Gradle Wrapper. The Bitrise `build_apk` workflow runs lint,
unit tests and the Android debug APK build. The package is `com.devvolodya.ide`,
with minSdk 26 and targetSdk 35.

## Not implemented yet

The file operations are currently exposed by the workspace repository; the Compose file list
still needs visible folder navigation and rename/delete controls. Git operations, an interactive
PTY terminal, syntax highlighting/LSP, SAF access to arbitrary external folders, compilers/toolchains,
project APK packaging, ZIP/SH exporters, EXE cross-compilation, Dev Hub networking, and Root
adapters for su / KernelSU / Magisk / APatch are not implemented. No Windows EXE is run natively
on Android. Root operations will require explicit confirmation and least-privilege execution.
No APK is considered a complete IDE release until all planned modules are implemented and verified.
