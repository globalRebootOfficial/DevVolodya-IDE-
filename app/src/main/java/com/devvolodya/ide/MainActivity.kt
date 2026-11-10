package com.devvolodya.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EditorBg = Color(0xFF111318)
private val PanelBg = Color(0xFF20232B)
private val Accent = Color(0xFF8AB4F8)
private val Muted = Color(0xFF9AA4B2)

private class EditorFile(val path: String, initial: String) {
    var content by mutableStateOf(initial)
    val name get() = path.substringAfterLast("/")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DevStudioApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevStudioApp() {
    val files = remember {
        mutableStateListOf(
            EditorFile("app/src/main/java/MainActivity.kt", "package com.example.app\n\nfun main() {\n    println(\"Hello, DevStudio!\")\n}"),
            EditorFile("app/build.gradle.kts", "plugins {\n    id(\"com.android.application\")\n    id(\"org.jetbrains.kotlin.android\")\n}"),
            EditorFile("app/src/main/AndroidManifest.xml", "<manifest>\n    <application android:label=\"My App\" />\n</manifest>")
        )
    }
    var selectedPath by remember { mutableStateOf(files.first().path) }
    var section by remember { mutableStateOf("Редактор") }
    var explorerVisible by remember { mutableStateOf(true) }
    val current = files.first { it.path == selectedPath }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = EditorBg, surface = PanelBg)) {
        Scaffold(
            containerColor = EditorBg,
            topBar = {
                TopAppBar(
                    title = { Column { Text("DevStudio", fontSize = 19.sp); Text("Android development environment", fontSize = 10.sp, color = Muted) } },
                    actions = {
                        IconButton(onClick = { explorerVisible = !explorerVisible }) { Icon(Icons.Default.FolderOpen, "Проводник") }
                        IconButton(onClick = { section = "Git" }) { Icon(Icons.Default.AccountTree, "Git") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = PanelBg, titleContentColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = PanelBg) {
                    listOf("Проекты", "Редактор", "Сборка", "Git").forEach { item ->
                        NavigationBarItem(
                            selected = section == item,
                            onClick = { section = item },
                            icon = { Icon(when(item) {
                                "Проекты" -> Icons.Default.Folder
                                "Редактор" -> Icons.Default.Code
                                "Сборка" -> Icons.Default.Build
                                else -> Icons.Default.AccountTree
                            }, item) },
                            label = { Text(item, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(10.dp)) {
                Row(Modifier.fillMaxWidth().background(PanelBg, RoundedCornerShape(9.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Android, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text("DevStudio Project", color = Color.White, modifier = Modifier.weight(1f))
                    Text("Kotlin · Gradle", color = Accent, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                when(section) {
                    "Проекты" -> Explorer(files, selectedPath) { selectedPath = it; section = "Редактор" }
                    "Редактор" -> {
                        if (explorerVisible) { Explorer(files, selectedPath) { selectedPath = it }; Spacer(Modifier.height(7.dp)) }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(PanelBg, RoundedCornerShape(8.dp))) {
                            files.forEach { file ->
                                TextButton(onClick = { selectedPath = file.path }, colors = ButtonDefaults.textButtonColors(contentColor = if(file.path == selectedPath) Accent else Muted)) {
                                    Text(file.name, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        CodeEditor(current)
                    }
                    "Сборка" -> BuildPanel()
                    else -> GitPanel()
                }
            }
        }
    }
}

@Composable
private fun Explorer(files: List<EditorFile>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().background(PanelBg, RoundedCornerShape(9.dp)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("ФАЙЛЫ ПРОЕКТА", color = Muted, fontSize = 10.sp)
        files.forEach { file ->
            Row(Modifier.fillMaxWidth().background(if(file.path == selected) Color(0xFF303846) else Color.Transparent, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if(file.name.endsWith(".kt")) Icons.Default.Code else Icons.Default.InsertDriveFile, null, tint = Accent, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(file.path, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { onSelect(file.path) }, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Открыть", fontSize = 10.sp) }
            }
        }
        Text("Пока показана демонстрационная структура. Работа с реальными файлами устройства добавляется отдельно.", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun CodeEditor(file: EditorFile) {
    var value by remember(file.path) { mutableStateOf(TextFieldValue(file.content)) }
    val keywords = listOf("fun","val","var","class","object","if","else","when","for","while","return","import","package","private","public","override","suspend","true","false","null")
    val word = value.text.take(value.selection.start).takeLastWhile { it.isLetterOrDigit() || it == '_' }
    val matches = if(word.length >= 2) keywords.filter { it.startsWith(word) && it != word }.take(4) else emptyList()

    Column(Modifier.fillMaxWidth().weight(1f).background(EditorBg, RoundedCornerShape(9.dp)).padding(9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(file.path, color = Accent, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text("Kotlin", color = Muted, fontSize = 10.sp)
        }
        HorizontalDivider(color = PanelBg, modifier = Modifier.padding(vertical = 7.dp))
        BasicTextField(
            value = value,
            onValueChange = { value = it; file.content = it.text },
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFE6E6E6), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp),
            visualTransformation = KotlinHighlight,
            decorationBox = { inner -> Box { if(value.text.isEmpty()) Text("Начни писать код…", color = Muted); inner() } }
        )
        if(matches.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                matches.forEach { suggestion ->
                    AssistChip(onClick = {
                        val before = value.text.substring(0, value.selection.start - word.length)
                        val after = value.text.substring(value.selection.start)
                        val updated = before + suggestion + after
                        value = TextFieldValue(updated, TextRange(before.length + suggestion.length))
                        file.content = updated
                    }, label = { Text(suggestion, fontSize = 10.sp) })
                }
            }
        }
    }
}

private object KotlinHighlight : VisualTransformation {
    private val pattern = Regex("//[^\\n]*|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\\b(fun|val|var|class|object|if|else|when|for|while|return|import|package|private|public|override|suspend|true|false|null)\\b|\\b\\d+\\b")
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        val styled = buildAnnotatedString {
            var end = 0
            pattern.findAll(text.text).forEach { m ->
                append(text.text.substring(end, m.range.first))
                val token = m.value
                val color = when {
                    token.startsWith("//") -> Color(0xFF7F8C98)
                    token.startsWith("\"") || token.startsWith("'") || token.first().isDigit() -> Color(0xFFB5CEA8)
                    else -> Color(0xFFC792EA)
                }
                withStyle(SpanStyle(color = color)) { append(token) }
                end = m.range.last + 1
            }
            append(text.text.substring(end))
        }
        return TransformedText(styled, OffsetMapping.Identity)
    }
}

@Composable
private fun BuildPanel() {
    Column(Modifier.fillMaxWidth().background(PanelBg, RoundedCornerShape(9.dp)).padding(14.dp)) {
        Text("Автоматическая сборка APK", color = Color.White)
        Spacer(Modifier.height(7.dp))
        Text("Каждый push в main автоматически запускает GitHub Actions и Gradle. После успешной сборки APK появится в Artifacts запуска.", color = Accent, fontSize = 12.sp)
        Spacer(Modifier.height(7.dp))
        Text("Статус сборки и скачивание APK прямо из DevStudio — следующий этап интеграции. Сборка не запускается вручную на планшете.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun GitPanel() {
    Column(Modifier.fillMaxWidth().background(PanelBg, RoundedCornerShape(9.dp)).padding(14.dp)) {
        Text("Git", color = Color.White)
        Spacer(Modifier.height(7.dp))
        Text("globalRebootOfficial/DevVolodya-IDE-", color = Accent, fontSize = 12.sp)
        Text("Ветка: main", color = Color.LightGray, fontSize = 12.sp)
        Spacer(Modifier.height(7.dp))
        Text("Commit, push, pull и история коммитов будут подключены через GitHub API на следующем этапе.", color = Muted, fontSize = 11.sp)
    }
}
