package com.devvolodya.ide.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceRepositoryTest {
    @Test
    fun createsProjectAndReadsWrittenFile() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("sample-app")
            val source = repository.createFile(project, "src/Main.kt")

            repository.writeText(source, "fun main() = println(\"Hello\")")

            assertEquals("fun main() = println(\"Hello\")", repository.readText(source))
            assertTrue(repository.listProjects().contains(project))
            assertTrue(repository.listProjectFiles(project).contains(source))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun createsNestedDirectories() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("sample-app")

            val directory = repository.createDirectory(project, "src/main/kotlin")

            assertTrue(directory.isDirectory)
            assertTrue(File(directory, "placeholder.txt").parentFile == directory)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun renamesFileAndKeepsItsContents() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("sample-app")
            val source = repository.createFile(project, "src/OldName.kt")
            repository.writeText(source, "val answer = 42")

            val renamed = repository.renameEntry(project, source, "Main.kt")

            assertFalse(source.exists())
            assertTrue(renamed.isFile)
            assertEquals("val answer = 42", repository.readText(renamed))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun deletesDirectoryTreeWithoutLeavingFilesBehind() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("sample-app")
            val directory = repository.createDirectory(project, "src/nested")
            repository.createFile(project, "src/nested/Main.kt")

            repository.deleteEntry(project, directory)

            assertFalse(directory.exists())
            assertTrue(repository.listProjectFiles(project).none {
                it.relativeTo(project).invariantSeparatorsPath.startsWith("src/")
            })
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsProjectPathTraversal() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            WorkspaceRepository(root).createProject("../outside")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFilePathTraversal() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("safe")
            repository.createFile(project, "../outside.txt")
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotDeleteProjectRoot() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("safe")
            repository.deleteEntry(project, project)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotRenameProjectRoot() {
        val root = Files.createTempDirectory("devvolodya-workspace").toFile()
        try {
            val repository = WorkspaceRepository(root)
            val project = repository.createProject("safe")
            repository.renameEntry(project, project, "renamed")
        } finally {
            root.deleteRecursively()
        }
    }
}
