package com.example.llama

import android.content.Context
import android.os.Environment
import com.example.model.GGUFModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.regex.Pattern

class GGUFScanner(private val context: Context) {

    private val quantPattern = Pattern.compile("(Q\\d+_[KSML]+|q\\d+_\\d|Q\\d+_\\d|IQ\\d+_[A-Z]+|FP16|BF16|F16|F32)", Pattern.CASE_INSENSITIVE)
    private val paramPattern = Pattern.compile("(\\d+\\.?\\d*B|\\d+k)", Pattern.CASE_INSENSITIVE)
    private val archKeywords = listOf("llama", "qwen", "mistral", "gemma", "phi", "deepseek", "starcoder", "command-r", "vicuna")

    suspend fun scanAllLocations(customDir: String? = null): List<GGUFModelInfo> = withContext(Dispatchers.IO) {
        val candidateDirs = mutableListOf<File>()

        customDir?.let {
            val dir = File(it)
            if (dir.exists() && dir.isDirectory) {
                candidateDirs.add(dir)
            }
        }

        // Standard Android storage directories
        val primaryStorage = Environment.getExternalStorageDirectory()
        if (primaryStorage != null && primaryStorage.exists()) {
            candidateDirs.add(File(primaryStorage, "Download"))
            candidateDirs.add(File(primaryStorage, "Documents"))
            candidateDirs.add(File(primaryStorage, "Models"))
            candidateDirs.add(File(primaryStorage, "llama.cpp"))
            candidateDirs.add(File(primaryStorage, "ollama"))
            candidateDirs.add(primaryStorage)
        }

        // App-specific external storage
        context.getExternalFilesDir(null)?.let { candidateDirs.add(it) }
        context.getExternalFilesDir("models")?.let { candidateDirs.add(it) }

        // Common Termux storage symlink if accessible
        val termuxStorage = File("/sdcard/termux")
        if (termuxStorage.exists()) {
            candidateDirs.add(termuxStorage)
        }

        val foundModels = mutableMapOf<String, GGUFModelInfo>()

        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                scanDirectory(dir, foundModels, depth = 0, maxDepth = 2)
            }
        }

        foundModels.values.toList().sortedByDescending { it.lastModified }
    }

    private fun scanDirectory(
        directory: File,
        results: MutableMap<String, GGUFModelInfo>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory && !file.name.startsWith(".")) {
                scanDirectory(file, results, depth + 1, maxDepth)
            } else if (file.isFile && file.name.endsWith(".gguf", ignoreCase = true)) {
                if (!results.containsKey(file.absolutePath)) {
                    val info = parseGGUFFile(file)
                    results[file.absolutePath] = info
                }
            }
        }
    }

    private fun parseGGUFFile(file: File): GGUFModelInfo {
        val fileName = file.name
        val sizeBytes = file.length()
        val sizeFormatted = formatFileSize(sizeBytes)

        val quantMatcher = quantPattern.matcher(fileName)
        val quantization = if (quantMatcher.find()) quantMatcher.group(1)?.uppercase(Locale.ROOT) ?: "Q4_K_M" else "Q4_K_M"

        val paramMatcher = paramPattern.matcher(fileName)
        val parameters = if (paramMatcher.find()) paramMatcher.group(1)?.uppercase(Locale.ROOT) ?: "7B" else "7B"

        val lowerName = fileName.lowercase(Locale.ROOT)
        var architecture = "Llama"
        for (arch in archKeywords) {
            if (lowerName.contains(arch)) {
                architecture = arch.replaceFirstChar { it.uppercase() }
                break
            }
        }

        // Try reading binary GGUF header
        var contextLength = 4096
        try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() >= 24) {
                    val headerBytes = ByteArray(4)
                    raf.readFully(headerBytes)
                    val magic = String(headerBytes, Charsets.US_ASCII)
                    if (magic == "GGUF") {
                        val buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
                        raf.channel.read(buffer)
                        buffer.flip()
                        val version = buffer.int
                        if (version in 1..3) {
                            // Valid GGUF header
                            contextLength = if (parameters.contains("8B") || parameters.contains("7B")) 8192 else 4096
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to name heuristic
        }

        return GGUFModelInfo(
            filename = fileName,
            path = file.absolutePath,
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            parameters = parameters,
            quantization = quantization,
            architecture = architecture,
            contextLength = contextLength,
            lastModified = file.lastModified()
        )
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            val gb = mb / 1024.0
            String.format(Locale.US, "%.2f GB", gb)
        } else {
            String.format(Locale.US, "%.1f MB", mb)
        }
    }
}
