package dev.kosmx.scannerMod

import me.replydev.qubo.InputData
import me.replydev.utils.IpList

class IpListList(private val list: List<IpList>): IpList("0.0.0.0", "0.0.0.0") {

    companion object {
        fun of(str: String) : IpList {
            return (if (str[0] == ';') str.drop(1) else str).split(";").stream().mapMulti { it, c ->
                try {
                    c.accept(InputData.tryGetIp(it))
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }.toList().let { IpListList(it) }
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
}