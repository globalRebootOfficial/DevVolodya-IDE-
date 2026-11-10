package com.devvolodya.ide.data

import java.io.File
import java.io.IOException

/**
 * Safe access to the app-private project workspace.
 *
 * All accepted paths are canonicalized before use so a caller cannot escape
 * the workspace with ".." segments or a symlink.
 */
class WorkspaceRepository(private val workspaceRoot: File) {
    init {
        if (!workspaceRoot.exists() && !workspaceRoot.mkdirs()) {
            throw IOException("Cannot create workspace directory")
        }
        if (!workspaceRoot.isDirectory) {
            throw IOException("Workspace path is not a directory")
        }
    }

    fun listProjects(): List<File> =
        workspaceRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory && !it.name.startsWith(".") && isInside(workspaceRoot, it) }
            .sortedBy { it.name.lowercase() }

    fun createProject(rawName: String): File {
        val name = validateSegment(rawName, "project name")
        val project = File(workspaceRoot, name)
        require(isInside(workspaceRoot, project)) { "Invalid project path" }
        if (!project.mkdir()) {
            throw IOException("Project already exists or cannot be created")
        }

        try {
            File(project, "README.md").writeText(
                "# $name\n\nCreated with DevVolodya IDE.\n",
                Charsets.UTF_8
            )
        } catch (error: Exception) {
            project.deleteRecursively()
            throw error
        }
        return project
    }

    fun listProjectFiles(project: File): List<File> {
        validateProject(project)
        return project.walkTopDown()
            .onEnter { directory -> directory == project || !directory.name.startsWith(".") }
            .filter { file ->
                file.isFile &&
                    !file.name.startsWith(".") &&
                    isInside(project, file)
            }
            .sortedBy { it.relativeTo(project).invariantSeparatorsPath.lowercase() }
            .toList()
    }

    /**
     * Creates a file below the selected project. Nested paths such as
     * "src/Main.kt" are supported; absolute paths and traversal are rejected.
     */
    fun createFile(project: File, relativePath: String): File {
        validateProject(project)
        require(relativePath.isNotBlank()) { "File path cannot be empty" }
        require(!relativePath.startsWith("/") && !relativePath.contains('\\')) {
            "Use a relative path with forward slashes"
        }

        val segments = relativePath.split("/")
        require(segments.all { it.isNotBlank() && it != "." && it != ".." }) {
            "Path traversal and empty path segments are not allowed"
        }
        segments.forEach { validateSegment(it, "path segment") }

        val target = File(project, relativePath).canonicalFile
        require(isInside(project, target)) { "File must stay inside the selected project" }
        if (target.exists()) {
            throw IOException("File already exists")
        }

        val parent = target.parentFile ?: throw IOException("Invalid file path")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Cannot create parent directory")
        }
        if (!target.createNewFile()) {
            throw IOException("Cannot create file")
        }
        return target
    }

    fun readText(file: File): String {
        require(isInside(workspaceRoot, file)) { "File is outside the workspace" }
        if (!file.isFile) throw IOException("Selected path is not a file")
        if (file.length() > MAX_EDITABLE_BYTES) {
            throw IOException("File is larger than the 1 MiB editor limit")
        }
        return file.readText(Charsets.UTF_8)
    }

    fun writeText(file: File, content: String) {
        require(isInside(workspaceRoot, file)) { "File is outside the workspace" }
        if (!file.isFile) throw IOException("Selected path is not a file")
        if (content.toByteArray(Charsets.UTF_8).size > MAX_EDITABLE_BYTES) {
            throw IOException("File is larger than the 1 MiB editor limit")
        }
        file.writeText(content, Charsets.UTF_8)
    }

    private fun validateProject(project: File) {
        val canonicalProject = project.canonicalFile
        require(isInside(workspaceRoot, canonicalProject)) { "Project is outside the workspace" }
        require(canonicalProject.parentFile == workspaceRoot.canonicalFile) {
            "Only workspace project folders are supported"
        }
        require(canonicalProject.isDirectory) { "Project directory does not exist" }
    }

    private fun validateSegment(value: String, description: String): String {
        val segment = value.trim()
        require(segment.isNotEmpty()) { "$description cannot be empty" }
        require(segment != "." && segment != "..") { "Invalid $description" }
        require(segment.length <= MAX_SEGMENT_LENGTH) { "$description is too long" }
        require('/' !in segment && '\\' !in segment) { "$description cannot contain path separators" }
        require(segment.none { it.isISOControl() }) { "$description contains a control character" }
        return segment
    }

    private fun isInside(parent: File, candidate: File): Boolean {
        val parentPath = parent.canonicalFile.toPath()
        val candidatePath = candidate.canonicalFile.toPath()
        return candidatePath.startsWith(parentPath) && candidatePath != parentPath
    }

    companion object {
        private const val MAX_EDITABLE_BYTES = 1024L * 1024L
        private const val MAX_SEGMENT_LENGTH = 128
    }
}
