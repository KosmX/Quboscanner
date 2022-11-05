package dev.kosmx.scannerMod

import kotlinx.serialization.Serializable

@Serializable
data class Server(val address: Address, val version: String, val online: String, val motd: String) {
    constructor(address: String, version: String, online: String, motd: String) : this(Address.ofString(address), version, online, motd)

    val hostname
        get() = address.hostname

    val port
        get() = address.port

}

@Serializable
data class Address(val hostname: String, val port: Int) {

    companion object {
        fun ofString(str: String): Address {
            val s = str.split(":")
            val hostname = s[0]
            val port = s[1].toInt()
            return Address(hostname, port)
        }
    }

    override fun toString(): String {
        return "$hostname:$port"
    }
}
