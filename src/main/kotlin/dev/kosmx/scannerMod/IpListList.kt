package dev.kosmx.scannerMod

import me.replydev.qubo.InputData
import me.replydev.utils.IpList
import java.io.BufferedReader

class IpListList(private val list: List<IpList>): IpList("0.0.0.0", "0.0.0.0") {

    companion object {
        fun of(str: String): IpListList {
            return (if (str[0] == ';') str.drop(1) else str).split(";", "\n").stream().mapMulti { it, c ->
                try {
                    c.accept(InputData.tryGetIp(it.replace("\t", "-")))
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }.toList().let { IpListList(it) }
        }

        fun of(reader: BufferedReader): IpListList {
            val str = reader.lines().collect({ StringBuilder() },
                { r, t ->
                    r.append(";")
                    r.append(t)
                },
                { s1, s2 ->
                    s1.append(";")
                    s1.append(s2)
                }).toString()
            return of(str)
        }
    }

    private val seq = sequence<String> {
        for (ipList in list) {
            while (ipList.hasNext()) {
                yield(ipList.next)
            }
        }
    }.iterator()


    override fun getCount(): Long {
        return list.fold(0) {acc, ipList -> acc + ipList.count }
    }

    override fun getNext(): String {
        return seq.next()
    }

    override fun hasNext(): Boolean {
        return seq.hasNext()
    }

    override fun toString(): String {
        return "IpRange{length:${list.size}, first:${list[0]}}"
    }
}