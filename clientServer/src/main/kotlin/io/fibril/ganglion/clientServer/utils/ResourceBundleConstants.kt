package io.fibril.ganglion.clientServer.utils

import java.util.*

object ResourceBundleConstants {
    val domain = ResourceBundle.getBundle("application").getString("domain")
    val matrixFields = ResourceBundle.getBundle("matrixFields")
    val userQueries = ResourceBundle.getBundle("queries.userQueries")
    val userProfileQueries = ResourceBundle.getBundle("queries.userProfileQueries")
    val mediaQueries = ResourceBundle.getBundle("queries.mediaQueries")
    val authQueries = ResourceBundle.getBundle("queries.authQueries")
    val deviceQueries = ResourceBundle.getBundle("queries.deviceQueries")
    val roomQueries = ResourceBundle.getBundle("queries.roomQueries")
    val roomEventQueries = ResourceBundle.getBundle("queries.roomEventQueries")
}