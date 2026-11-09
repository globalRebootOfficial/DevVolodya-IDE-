package com.devvolodya.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EditorBg = Color(0xFF17191F)
private val PanelBg = Color(0xFF20232B)
private val Accent = Color(0xFF8AB4F8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DevVolodyaApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevVolodyaApp() {
    var selectedTab by remember { mutableStateOf("Редактор") }
    var code by remember {
        mutableStateOf(
            "// Добро пожаловать в DevVolodya IDE\n\n" +
            "fun main() {\n    println(\"Привет, мир!\")\n}"
        )
    }
    val tabs = listOf("Проекты", "Редактор", "Терминал", "Git")

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = EditorBg,
            surface = PanelBg
        )
    ) {
        Scaffold(
            containerColor = EditorBg,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("DevVolodya IDE", fontSize = 18.sp)
                            Text(
                                "Мобильная среда разработки",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Запуск")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = PanelBg,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = PanelBg) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        "Проекты" -> Icons.Default.Folder
                                        "Редактор" -> Icons.Default.Code
                                        "Терминал" -> Icons.Default.Terminal
                                        else -> Icons.Default.AccountTree
                                    },
                                    contentDescription = tab
                                )
                            },
                            label = { Text(tab, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(12.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PanelBg, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Мой первый проект", color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Text("Kotlin", color = Accent, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                when (selectedTab) {
                    "Проекты" -> ProjectPanel()
                    "Редактор" -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Main.kt", color = Color.White, modifier = Modifier.weight(1f))
                            Text("Черновик", color = Color.Gray, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFE6E6E6)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF111318),
                                unfocusedContainerColor = Color(0xFF111318),
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Color.DarkGray
                            )
                        )
                    }
                    "Терминал" -> TerminalPanel()
                    else -> GitPanel()
                }
            }
        }
    }
}

@Composable
private fun ProjectPanel() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Файлы проекта", color = Color.White)
        listOf(
            "app/",
            "  src/",
            "    MainActivity.kt",
            "build.gradle.kts",
            "settings.gradle.kts"
        ).forEach { file ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(PanelBg, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    if (file.endsWith("/")) Icons.Default.Folder
                    else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = Accent
                )
                Spacer(Modifier.width(10.dp))
                Text(file, color = Color.White)
            }
        }
        Text(
            "Проводник проекта будет расширен на следующем этапе.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun TerminalPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.Black, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text("DevVolodya Terminal", color = Accent, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(12.dp))
        Text(
            "$ Ожидание подключения локального shell…",
            color = Color(0xFFB8E6B8),
            fontFamily = FontFamily.Monospace
        )
        Text(
            "Терминал пока является интерфейсной заготовкой.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun GitPanel() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PanelBg, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text("Контроль версий Git", color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("Ветка: main", color = Accent)
        Text(
            "Репозиторий: globalRebootOfficial/DevVolodya-IDE-",
            color = Color.LightGray,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Git-клиент, коммиты и синхронизация будут подключены отдельным модулем.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
