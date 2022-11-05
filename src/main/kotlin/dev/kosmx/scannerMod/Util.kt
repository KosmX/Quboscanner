package dev.kosmx.scannerMod

import java.nio.file.Files
import java.util.*
import java.util.concurrent.Executors
import kotlin.io.path.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.isDirectory

/**
 * Map a sequence parallel, using available - 1 threads by default
 * The process has no timeout or exception handling, please provide your own if needed.
 *
 */
fun <T, R> Sequence<T>.mapParallel(numThreads: Int = Runtime.getRuntime().availableProcessors() - 1, action: (value: T) -> R?): Sequence<R?> {
    return chunked(numThreads)
        .map { chunk ->
            val threadPool = Executors.newFixedThreadPool(numThreads)
            try {
                return@map chunk.map {
                    val callable: () -> R? = { action(it) }
                    threadPool.submit(callable)
                }
            } finally {
                threadPool.shutdown()
            }
        }.flatten()
        .map { future -> future.get() }
}

fun <T> Sequence<T?>.dropNull(): Sequence<T> {
    return mapNotNull { it }
}


private val iconDir = Path("icons")


fun writeIcon(str: String): String? {
    if (!iconDir.isDirectory()) {
        iconDir.createDirectory()
    }

    val data: ByteArray
    try {
        data = Base64.getDecoder().decode(str.substring(str.indexOf(',') + 1)) ?: return null
    } catch (e: IllegalArgumentException) {
        println(str)
        println(e.message)
        e.printStackTrace()
        return null
    }

    val hash = data.contentHashCode().toUInt()
    var extra: Int? = null

    while (true) {
        val filename = extra?.let { "$hash-$extra.png" } ?: "$hash.png"
        if(!Files.isRegularFile(iconDir.resolve(filename))) break
        if (iconDir.resolve(filename).toFile().readBytes().contentEquals(data)) {
            return filename
        } else {
            if (extra == null) extra = 1
            else extra++
        }
    }

    val filename = extra?.let { "$hash-$extra.png" } ?: "$hash.png"

    iconDir.resolve(filename).toFile().writeBytes(data)
    return filename
}
