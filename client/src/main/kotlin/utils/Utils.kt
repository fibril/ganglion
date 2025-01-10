package utils

import java.util.*

object Utils {
    fun idGenerator() = UUID.randomUUID().toString().replace("-", "")
}