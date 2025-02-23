package io.fibril.ganglion.clientServer.utils

object QueryUtils {
    fun prepareQueryFromMap(query: String, params: Map<String, String>): String {
        val preparedQuery = StringBuilder(query).apply {
            var i = 0
            while (i < length - 1) {
                if (this[i] == '$' && (i == 0 || this[i - 1] == ' ')) {
                    var keyEndIndex = i
                    while (keyEndIndex < length - 1) {
                        if (this[keyEndIndex] == ' ' || this[keyEndIndex] == ';') break;
                        keyEndIndex++
                    }
                    val key = substring(i + 1, keyEndIndex).trim()

                    val value = params.getOrDefault(key, null)

                    if (value != null) {
                        replace(i, keyEndIndex, value)
                        i += value.length
                    } else i = keyEndIndex
                } else {
                    i++
                }
            }
        }
        return preparedQuery.toString()
    }
}