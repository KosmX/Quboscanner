package dev.kosmx.scannerMod

import kotlinx.serialization.Serializable
import me.replydev.mcping.data.FinalResponse

//data class so kotlin should pretty-print it by default

@Serializable
data class ResponseData (val motd: String, val address: String, val version: String, var icon: String?, val players: Players) {


    companion object {
        fun ofFinalResponse(response: Pair<Server, FinalResponse>): ResponseData {
            val description: String = response.second.description ?: ""
            val address = response.first.address.toString()
            val icon: String? = response.second.favIcon?.replace("\n", "")
            val players = Players(response.second.players)
            return ResponseData(
                motd = description,
                address = address,
                icon = icon,
                players = players,
                version = response.second.version.name ?: ""
            )
        }
    }

    override fun toString(): String {
        return """
            $address
            $version
            MOTD: ${motd.replace("\n", "\\n")}
            icon data: $icon
            players: $players
        """.trimIndent()
    }
}

@Serializable
data class Player(val name: String?, val id: String?) {
    constructor(player: me.replydev.mcping.rawData.Player) : this(player.name, player.id)
}

@Serializable
data class Players(val max: Int, val online: Int, val sample: List<Player>?) {
    constructor(players: me.replydev.mcping.rawData.Players) : this(players.max, players.online, players.sample.map { Player(it) })
}
