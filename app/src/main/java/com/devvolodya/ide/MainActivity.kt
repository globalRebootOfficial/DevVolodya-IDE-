package com.devvolodya.ide

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devvolodya.ide.data.WorkspaceRepository
import java.io.File

private val Background = Color(0xFF0D1117)
private val Panel = Color(0xFF161B22)
private val Border = Color(0xFF30363D)
private val Accent = Color(0xFF58A6FF)
private val Muted = Color(0xFF8B949E)
private val MainText = Color(0xFFE6EDF3)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = WorkspaceRepository(File(filesDir, "workspace"))
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Background,
                    surface = Panel,
                    onBackground = MainText,
                    onSurface = MainText
                )
            ) {
                DevVolodyaApp(repository)
            }
        }
    }
}

private enum class Page(val label: String) {
    HOME("Home"),
    PROJECTS("Projects"),
    EDITOR("Editor"),
    GIT("Git"),
    BUILD("Build"),
    SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevVolodyaApp(repository: WorkspaceRepository) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(Page.HOME) }
    var projects by remember { mutableStateOf(repository.listProjects()) }
    var selectedProject by remember { mutableStateOf<File?>(null) }
    var projectFiles by remember { mutableStateOf(emptyList<File>()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var editorContent by remember { mutableStateOf("") }
    var editorDirty by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var newFilePath by remember { mutableStateOf("src/Main.kt") }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var fileClipboard by remember { mutableStateOf<File?>(null) }
    var cutClipboard by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var rootStatus by remember { mutableStateOf("Not checked") }

    fun refreshProjects() {
        projects = repository.listProjects()
    }

    fun openFile(file: File) {
        selectedFile = file
        val result = runCatching { repository.readText(file) }
        editorContent = result.getOrDefault("")
        editorDirty = false
        statusMessage = result.exceptionOrNull()?.message
    }

    fun openProject(project: File) {
        selectedProject = project
        val result = runCatching { repository.listProjectFiles(project) }
        projectFiles = result.getOrDefault(emptyList())
        statusMessage = result.exceptionOrNull()?.message
        val firstFile = projectFiles.firstOrNull()
        if (firstFile == null) {
            selectedFile = null
            editorContent = ""
            editorDirty = false
        } else {
            openFile(firstFile)
        }
        page = Page.EDITOR
    }

    fun saveCurrentFile() {
        val file = selectedFile ?: return
        val result = runCatching { repository.writeText(file, editorContent) }
        if (result.isSuccess) {
            editorDirty = false
            statusMessage = "Saved " + file.name
        } else {
            statusMessage = result.exceptionOrNull()?.message ?: "Save failed"
        }
    }

    fun createProject() {
        val result = runCatching { repository.createProject(projectName) }
        result.onSuccess { created ->
            projectName = ""
            refreshProjects()
            openProject(created)
            statusMessage = "Project " + created.name + " created"
        }.onFailure { error ->
            statusMessage = error.message ?: "Project creation failed"
        }
    }

    fun refreshProjectFiles() { selectedProject?.let { projectFiles = repository.listProjectFiles(it) } }

    fun createProjectFile() {
        val project = selectedProject ?: return
        val result = runCatching { repository.createFile(project, newFilePath) }
        result.onSuccess { created ->
            projectFiles = repository.listProjectFiles(project)
            openFile(created)
            newFilePath = "src/Main.kt"
            showCreateFileDialog = false
            statusMessage = "Created " + created.relativeTo(project).invariantSeparatorsPath
        }.onFailure { error ->
            statusMessage = error.message ?: "File creation failed"
        }
    }

    if (showDeleteDialog && selectedFile != null) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Delete file?") }, text = { Text("This permanently deletes ${selectedFile?.name}.") }, confirmButton = { TextButton(onClick = { val target = selectedFile; val project = selectedProject; if (target != null && project != null) runCatching { repository.deleteEntry(project, target) }.onSuccess { selectedFile = null; editorContent = ""; editorDirty = false; refreshProjectFiles(); statusMessage = "File deleted" }.onFailure { statusMessage = it.message }; showDeleteDialog = false }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } })
    }

    if (showCreateFileDialog && selectedProject != null) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false },
            title = { Text("Create file") },
            text = {
                OutlinedTextField(
                    value = newFilePath,
                    onValueChange = { newFilePath = it },
                    singleLine = true,
                    label = { Text("Relative path") },
                    supportingText = { Text("Example: src/Main.kt") }
                )
            },
            confirmButton = {
                TextButton(onClick = { createProjectFile() }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false }) { Text("Cancel") }
            }
        )
    }

    val navigationItems = listOf(
        Page.HOME to Icons.Default.Home,
        Page.PROJECTS to Icons.Default.Folder,
        Page.EDITOR to Icons.Default.Code,
        Page.GIT to Icons.Default.AccountTree,
        Page.BUILD to Icons.Default.Build,
        Page.SETTINGS to Icons.Default.Settings
    )

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DevVolodya IDE", fontSize = 18.sp)
                        Text(
                            selectedProject?.name ?: "Native Android development workspace",
                            color = Muted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    if (page == Page.EDITOR && selectedProject != null) {
                        IconButton(onClick = { showCreateFileDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Create file")
                        }
                        IconButton(
                            onClick = { saveCurrentFile() },
                            enabled = editorDirty
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save file")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Panel,
                    titleContentColor = MainText
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                navigationItems.forEach { (item, icon) ->
                    NavigationBarItem(
                        selected = page == item,
                        onClick = { page = item },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            statusMessage?.let { message ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        message,
                        color = if (
                            message.contains("failed", ignoreCase = true) ||
                            message.contains("cannot", ignoreCase = true) ||
                            message.contains("invalid", ignoreCase = true)
                        ) Color(0xFFFF7B72) else Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            when (page) {
                Page.HOME -> HomePage(projects.size) { page = Page.PROJECTS }
                Page.PROJECTS -> ProjectsPage(
                    projects = projects,
                    projectName = projectName,
                    onProjectNameChange = { projectName = it },
                    onCreate = { createProject() },
                    onOpen = { openProject(it) }
                )
                Page.EDITOR -> EditorPage(
                    project = selectedProject, files = projectFiles, selectedFile = selectedFile,
                    content = editorContent, dirty = editorDirty,
                    onSelectFile = { openFile(it) }, onContentChange = { editorContent = it; editorDirty = true },
                    onSave = { saveCurrentFile() }, onCreateFile = { showCreateFileDialog = true },
                    onOpenProjects = { page = Page.PROJECTS },
                    onCopyFile = { selectedFile?.let { fileClipboard = it; cutClipboard = false; statusMessage = "File copied" } },
                    onCutFile = { selectedFile?.let { fileClipboard = it; cutClipboard = true; statusMessage = "File ready to move" } },
                    onPasteFile = { val src = fileClipboard; if (src != null) runCatching {
                        val suffix = if (src.extension.isBlank()) "" else ".${src.extension}"
                        val dst = File(src.parentFile, src.nameWithoutExtension + "_copy" + suffix)
                        src.copyTo(dst, overwrite = false); if (cutClipboard) src.delete()
                        fileClipboard = null; cutClipboard = false; refreshProjectFiles(); openFile(dst); statusMessage = "File pasted"
                    }.onFailure { statusMessage = it.message ?: "Paste failed" } },
                    onDeleteFile = { showDeleteDialog = true },
                    onCopyCode = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("code", editorContent)); statusMessage = "Code copied" },
                    onCutCode = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.setPrimaryClip(ClipData.newPlainText("code", editorContent)); editorContent = ""; editorDirty = true; statusMessage = "Code cut — save to apply" },
                    onPasteCode = { val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; val clip = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty(); if (clip.isNotEmpty()) { editorContent += clip; editorDirty = true; statusMessage = "Code pasted" } }
                )
                Page.GIT -> GitPage(selectedProject)
                Page.BUILD -> BuildPage(selectedProject,
                    onExportZip = { exportZip(selectedProject, context.filesDir)?.let { statusMessage = "ZIP exported: ${it.absolutePath}" } ?: run { statusMessage = "ZIP export failed" } },
                    onExportShell = { exportShell(selectedProject)?.let { statusMessage = "SH created: ${it.absolutePath}" } ?: run { statusMessage = "SH export failed" } },
                    onBuildApk = { statusMessage = "APK build requires an Android SDK/Gradle build runner. No build was started on this device." })
                Page.SETTINGS -> SettingsPage(rootStatus) {
                    Thread {
                        val result = runCatching {
                            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
                            val output = process.inputStream.bufferedReader().readText()
                            val code = process.waitFor()
                            if (code == 0 && output.contains("uid=0")) "Root granted: $output" else "Root unavailable or denied: $output"
                        }.getOrElse { "Root unavailable: ${it.message}" }
                        android.os.Handler(android.os.Looper.getMainLooper()).post { rootStatus = result }
                    }.start()
                }
            }
        }
    }
}

@Composable
private fun HomePage(projectCount: Int, openProjects: () -> Unit) {
    Text("Your mobile development workspace", color = Muted)
    Spacer(Modifier.height(16.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Welcome to DevVolodya IDE", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("$projectCount local projects", color = Muted)
            Spacer(Modifier.height(14.dp))
            Button(onClick = openProjects) { Text("Open projects") }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("WORKSPACE", color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    InfoRow("Native shell", "Kotlin · Jetpack Compose")
    InfoRow("Local storage", "App-private workspace")
    InfoRow("Current milestone", "Project manager · file editor · safe file access")
}

@Composable
private fun ProjectsPage(
    projects: List<File>,
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onOpen: (File) -> Unit
) {
    Text("Projects", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = projectName,
            onValueChange = onProjectNameChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("New project name") }
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onCreate, enabled = projectName.isNotBlank()) {
            Text("Create")
        }
    }
    Spacer(Modifier.height(12.dp))
    if (projects.isEmpty()) {
        Text("No projects yet. Create your first project above.", color = Muted)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(projects) { project ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(project) },
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Accent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(project.name)
                        Text(
                            project.walkTopDown().count { it.isFile }.toString() + " files",
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Text("OPEN  ›", color = Accent, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EditorPage(
    project: File?,
    files: List<File>,
    selectedFile: File?,
    content: String,
    dirty: Boolean,
    onSelectFile: (File) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCreateFile: () -> Unit,
    onOpenProjects: () -> Unit,
    onCopyFile: () -> Unit, onCutFile: () -> Unit, onPasteFile: () -> Unit, onDeleteFile: () -> Unit,
    onCopyCode: () -> Unit, onCutCode: () -> Unit, onPasteCode: () -> Unit
) {
    if (project == null) {
        Text("Open a project from Projects to start editing.", color = Muted)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onOpenProjects) { Text("Open projects") }
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(project.name, color = Accent, style = MaterialTheme.typography.titleMedium)
            Text(files.size.toString() + " files", color = Muted, fontSize = 11.sp)
        }
        OutlinedButton(onClick = onCreateFile) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("New file")
        }
    }
    Spacer(Modifier.height(8.dp))

    if (files.isEmpty()) {
        Text("This project has no files yet. Create one to begin.", color = Muted)
        return
    }

    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.width(132.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(files) { file ->
                val selected = file == selectedFile
                Text(
                    file.relativeTo(project).invariantSeparatorsPath,
                    color = if (selected) Accent else Muted,
                    fontSize = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) Color(0xFF1F2937) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectFile(file) }
                        .padding(7.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selectedFile?.name ?: "No file selected",
                    color = MainText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dirty) Text("Modified", color = Accent, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onSave, enabled = dirty) {
                    Icon(Icons.Default.Save, contentDescription = "Save file")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onCopyFile, enabled = selectedFile != null) { Text("Copy file") }
                TextButton(onClick = onCutFile, enabled = selectedFile != null) { Text("Cut") }
                TextButton(onClick = onPasteFile, enabled = selectedFile != null) { Text("Paste") }
                TextButton(onClick = onDeleteFile, enabled = selectedFile != null) { Text("Delete") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onCopyCode) { Text("Copy code") }
                TextButton(onClick = onCutCode) { Text("Cut code") }
                TextButton(onClick = onPasteCode) { Text("Paste code") }
            }
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MainText
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF0B0F14),
                    unfocusedContainerColor = Color(0xFF0B0F14),
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border
                )
            )
        }
    }
}

@Composable
private fun GitPage(project: File?) {
    Text("Git integration", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(
        if (project == null) "Select a project first." else "Selected project: " + project.name,
        color = Muted
    )
    Spacer(Modifier.height(12.dp))
    InfoRow("Repository status", "Git engine not connected yet")
    InfoRow("Planned operations", "Init · Status · Add · Commit · Branch · Log · Diff · Remote")
    Text(
        "No Git operation is shown as successful until a real Git backend is connected.",
        color = Muted,
        fontSize = 12.sp
    )
}

@Composable
private fun BuildPage(project: File?, onExportZip: () -> Unit, onExportShell: () -> Unit, onBuildApk: () -> Unit) {
    Text("Build Center", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(10.dp))
    if (project == null) Text("Open a project first.", color = Muted) else {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onBuildApk) { Text("Build APK") }
            OutlinedButton(onClick = onExportZip) { Text("Export ZIP") }
            OutlinedButton(onClick = onExportShell) { Text("Export SH") }
        }
    }
    Spacer(Modifier.height(10.dp))
    listOf(
        "Android APK / AAB" to "Requires Android SDK, Gradle and Android build runner.",
        "C / C++" to "Requires Clang / NDK toolchain.",
        "Java / Kotlin" to "Requires JDK, Gradle and D8/R8.",
        "Python / JavaScript" to "Requires a runtime or packaging tool for the selected target.",
        "ZIP" to "Local project archive export is available.",
        "SH" to "A shell helper script can be exported.",
        "Windows EXE" to "Requires a Windows-compatible compiler or remote Windows runner."
    ).forEach { InfoRow(it.first, it.second) }
    Text("A target is marked built only after a real compiler produces and verifies the output.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun SettingsPage(rootStatus: String, onCheckRoot: () -> Unit) {
    Text("Settings", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    InfoRow("Package", "com.devvolodya.ide")
    InfoRow("Platform", "Native Android · Kotlin · Jetpack Compose")
    InfoRow("Workspace", "Projects are stored in app-private storage.")
    InfoRow("Root status", rootStatus)
    OutlinedButton(onClick = onCheckRoot) { Text("Check KernelSU / Magisk root") }
    Spacer(Modifier.height(12.dp))
    Text("COMPLETE USER GUIDE", color = Accent, style = MaterialTheme.typography.titleMedium)
    InfoRow("1. Projects", "Open Projects, enter a name and tap Create. Open a project to browse its files.")
    InfoRow("2. Files", "Use New file to create a file. Select it from the file list. Copy file, Cut, Paste and Delete act on the selected file. Delete asks for confirmation.")
    InfoRow("3. Code editor", "Edit code and tap Save. Copy code, Cut code and Paste code use Android clipboard and operate on the whole editor buffer.")
    InfoRow("4. Build Center", "ZIP and SH export are available. APK requires an Android SDK/Gradle build runner. Other targets need their own compiler or remote build environment.")
    InfoRow("5. Git", "Git operations are not connected in this build yet.")
    InfoRow("6. Root", "The phone must already be rooted. Tap the root check and approve the KernelSU/Magisk prompt. Root is never silently granted.")
    InfoRow("7. Backups", "Export ZIP before major edits. Uninstalling the app may remove its private workspace.")
    InfoRow("8. Formats", "APK/AAB use Android Gradle; EXE uses a Windows toolchain; ZIP is an archive; SH is a shell script. Python, JavaScript, JVM and native targets need matching runtimes and compilers.")
}

@Composable
private fun InfoRow(title: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
    }
}


private fun exportZip(project: File?, filesDir: File): File? {
    if (project == null || !project.isDirectory) return null
    return runCatching {
        val outDir = File(filesDir, "exports").apply { mkdirs() }
        val out = File(outDir, "${project.name}.zip")
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            project.walkTopDown().filter { it.isFile }.forEach { file ->
                zip.putNextEntry(ZipEntry(project.name + "/" + file.relativeTo(project).invariantSeparatorsPath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        out
    }.getOrNull()
}

private fun exportShell(project: File?): File? {
    if (project == null || !project.isDirectory) return null
    return runCatching {
        val out = File(project, "build-project.sh")
        out.writeText("#!/system/bin/sh\nset -eu\necho \"Project: ${project.name}\"\necho \"Add the required compiler command for this project.\"\n", Charsets.UTF_8)
        out.setExecutable(true)
        out
    }.getOrNull()
}
