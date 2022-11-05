@file:JvmName("ServerPingTool")
package dev.kosmx.scannerMod

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import me.replydev.mcping.MCPing
import me.replydev.mcping.PingOptions
import me.replydev.mcping.data.FinalResponse
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.nio.file.Files
import kotlin.io.path.Path

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val parser = ArgParser("Server ping tool")

    val input by parser.option(
        ArgType.String,
        shortName = "l",
        fullName = "ipListFile",
        description = "The file containing the IPs, you may use stdin"
    ).required()

    val threadCount by parser.option(ArgType.Int, shortName = "t", fullName = "threads", description = "Thread count")
        .required()

    val output by parser.option(
        ArgType.String,
        shortName = "o",
        fullName = "output",
        description = "you may use stdout"
    ).required()

    val versionFilter by parser.option(ArgType.String, fullName = "version", description = "MC version filter (regex)")

    val timeoutArg by parser.option(
        ArgType.Int,
        shortName = "ti",
        fullName = "timeout",
        description = "server ping timeout"
    ).default(1000)

    val outputToJson by parser.option(ArgType.String, fullName = "json")

    val saveIcon by parser.option(ArgType.Boolean, fullName = "saveIcon", shortName = "i").default(false)

    parser.parse(args)

    val inputReader: BufferedReader = if (input == "stdin") {
        BufferedReader(InputStreamReader(System.`in`))
    } else {
        Files.newBufferedReader(Path(input))
    }

    val outputWriter: BufferedWriter = if (output == "stdout") {
        BufferedWriter(OutputStreamWriter(System.out))
    } else {
        Files.newBufferedWriter(Path(output))
    }


    val matcher: (version: String) -> Boolean = versionFilter?.let { str ->
        Regex(str)
    }?.let { regex ->
        { version ->
            regex.matches(version)
        }
    } ?: { true }

    val results = if (outputToJson != null) mutableListOf<ResponseData>() else null

    inputReader.lineSequence().iterator()
        //Split lines into servers
        .let { lines ->
            sequence {
                while (lines.hasNext()) {
                    val line = lines.next()
                    if (line == "-----------------------") continue
                    try {
                        val version = (lines.next().takeIf { it != "-----------------------" }
                            ?: error("misaligned list")).substring(9)
                        val online = (lines.next().takeIf { it != "-----------------------" }
                            ?: error("misaligned list")).substring(8)
                        val motd = (lines.next().takeIf { it != "-----------------------" }
                            ?: error("misaligned list")).substring(6)
                        (lines.next().takeIf { it != "-----------------------" } ?: error("misaligned list")) //ping time
                        /*val icon = */(lines.next().takeIf { it != "-----------------------" } ?: error("misaligned list")) //optional icon
                        yield(Server(line, version, online, motd))
                    } catch (_: IllegalStateException) {
                        //Just misaligned text, don't worry about it
                    } catch (_: NoSuchElementException) {
                        //No more lines, don't write error messages
                    } catch (e: Exception) {
                        println(e.message)
                        e.printStackTrace()
                    }
                }
            }
        }

        //Version filtering
        .filter { matcher(it.version) }

        //Process server sequence parallel
        .mapParallel(threadCount) { server ->
            try {
                return@mapParallel Pair(
                    server, MCPing().getPing(
                        PingOptions().apply {
                            timeout = timeoutArg
                            hostname = server.hostname
                            port = server.port
                        })
                )
            } catch (e: ConnectException) {
                println("${e.message} on ${server.address}")
            } catch (e: SocketTimeoutException) {
                println("${e.message} on ${server.address}")
            } catch (t: Throwable) {
                println("${t.message} on ${server.address}")
                t.printStackTrace()
            }
            return@mapParallel null
        }

        //Write responses to json/text
        .dropNull() //drop null elements
        .forEach { response: Pair<Server, FinalResponse> ->
            val responseData = ResponseData.ofFinalResponse(response)
            if (saveIcon) responseData.icon = responseData.icon?.let{ writeIcon(it) }
            outputWriter.append(responseData.toString())
            outputWriter.newLine()
            outputWriter.append("-----------------------")
            outputWriter.newLine()

            results?.add(responseData)
        }

    val format = Json {
        prettyPrint = true
    }
    results?.let {
        Files.newOutputStream(Path(outputToJson!!)).use { jsonWriter ->
            format.encodeToStream(it, jsonWriter)
        }
    }

    inputReader.close()
    outputWriter.close()
}
