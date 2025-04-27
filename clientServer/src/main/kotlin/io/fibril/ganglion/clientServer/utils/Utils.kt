package io.fibril.ganglion.clientServer.utils

import java.security.SecureRandom
import java.util.*

object Utils {
    fun idGenerator() = UUID.randomUUID().toString().replace('-', Character.MIN_VALUE)

    fun shortIdGenerator(length: Int? = 18): String {
        val random = SecureRandom()
        val zeroToZ = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray()
        val sb = StringBuilder(length!!)
        for (i in 0..<length) sb.append(zeroToZ[random.nextInt(zeroToZ.size)])
        return sb.toString()
    }

    val ROOM_ID_PREFIX = "!"
    val ROOM_EVENT_ID_PREFIX = "$"

    fun generateRoomId(): String = "${ROOM_ID_PREFIX}${shortIdGenerator()}:${ResourceBundleConstants.domain}"

    fun generateRoomEventId(): String = "${ROOM_EVENT_ID_PREFIX}${shortIdGenerator()}:${ResourceBundleConstants.domain}"

    fun clamp(number: Int, lower: Int, upper: Int): Int {
        var res = if (number < upper) number else upper
        res = if (number > lower) number else lower

        return res
    }
}