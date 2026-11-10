package com.devvolodya.ide.data

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

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
        val canonicalProject = project.canonicalFile
        return canonicalProject.walkTopDown()
            .onEnter { directory -> directory == canonicalProject || !directory.name.startsWith(".") }
            .filter { file ->
                file.isFile &&
                    !file.name.startsWith(".") &&
                    isInside(canonicalProject, file)
            }
            .sortedBy { it.relativeTo(canonicalProject).invariantSeparatorsPath.lowercase() }
            .toList()
    }

    /** Creates a UTF-8 file below the selected project. Nested paths are supported. */
    fun createFile(project: File, relativePath: String): File {
        val target = resolveRelativePath(project, relativePath)
        if (Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)) {
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

    /** Creates a directory below the selected project, including missing parent folders. */
    fun createDirectory(project: File, relativePath: String): File {
        val target = resolveRelativePath(project, relativePath)
        if (Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            throw IOException("Directory already exists")
        }

        val parent = target.parentFile ?: throw IOException("Invalid directory path")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Cannot create parent directory")
        }
        if (!target.mkdir()) {
            throw IOException("Cannot create directory")
        }
        return target
    }

    /** Renames a file or directory without allowing it to leave the project. */
    fun renameEntry(project: File, entry: File, rawNewName: String): File {
        validateProject(project)
        val source = resolveExistingEntry(project, entry)
        val newName = validateSegment(rawNewName, "new name")
        if (source.name == newName) return source

        val parent = source.parentFile ?: throw IOException("Cannot rename project root")
        val target = File(parent, newName).canonicalFile
        require(isInside(project, target)) { "Renamed entry must stay inside the project" }
        if (Files.exists(target.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            throw IOException("An entry with that name already exists")
        }

        Files.move(source.toPath(), target.toPath())
        return target
    }

    /** Deletes a file or directory tree without following symbolic links. */
    fun deleteEntry(project: File, entry: File) {
        validateProject(project)
        val target = resolveExistingEntry(project, entry)

        Files.walkFileTree(target.toPath(), object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, error: IOException?): FileVisitResult {
                if (error != null) throw error
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    fun readText(file: File): String {
        val canonicalFile = file.canonicalFile
        require(isInside(workspaceRoot, canonicalFile)) { "File is outside the workspace" }
        if (!canonicalFile.isFile) throw IOException("Selected path is not a file")
        if (canonicalFile.length() > MAX_EDITABLE_BYTES) {
            throw IOException("File is larger than the 1 MiB editor limit")
        }
        return canonicalFile.readText(Charsets.UTF_8)
    }

    fun writeText(file: File, content: String) {
        val canonicalFile = file.canonicalFile
        require(isInside(workspaceRoot, canonicalFile)) { "File is outside the workspace" }
        if (!canonicalFile.isFile) throw IOException("Selected path is not a file")
        if (content.toByteArray(Charsets.UTF_8).size > MAX_EDITABLE_BYTES) {
            throw IOException("File is larger than the 1 MiB editor limit")
        }
        canonicalFile.writeText(content, Charsets.UTF_8)
    }

    private fun resolveRelativePath(project: File, relativePath: String): File {
        validateProject(project)
        require(relativePath.isNotBlank()) { "Path cannot be empty" }
        require(!relativePath.startsWith("/") && '\\' !in relativePath) {
            "Use a relative path with forward slashes"
        }

        val segments = relativePath.split("/")
        require(segments.all { it.isNotBlank() }) { "Path contains an empty segment" }
        val safeSegments = segments.map { validateSegment(it, "path segment") }
        val canonicalProject = project.canonicalFile
        val target = File(canonicalProject, safeSegments.joinToString(File.separator)).canonicalFile
        require(isInside(canonicalProject, target)) { "Path must stay inside the selected project" }
        return target
    }

    private fun resolveExistingEntry(project: File, entry: File): File {
        require(!Files.isSymbolicLink(entry.toPath())) { "Symbolic links cannot be modified" }
        val canonicalProject = project.canonicalFile
        val target = entry.canonicalFile
        require(isInside(canonicalProject, target)) { "Entry is outside the selected project" }
        if (!target.exists()) throw IOException("Selected entry does not exist")
        return target
    }

    private fun validateProject(project: File) {
        require(!Files.isSymbolicLink(project.toPath())) { "Symbolic-link projects are not supported" }
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
