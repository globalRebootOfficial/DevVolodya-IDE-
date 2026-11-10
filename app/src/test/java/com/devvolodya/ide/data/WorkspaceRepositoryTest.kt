package com.devvolodya.ide.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
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
}
