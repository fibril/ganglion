package io.fibril.ganglion.clientServer.utils

import java.util.*

object ResourceBundleConstants {
    private val ENVIRONMENT = System.getProperty("ganglion.environment")
    val applicationBundle = ResourceBundle.getBundle("${ENVIRONMENT}.application")
    val databaseBundle = ResourceBundle.getBundle("${ENVIRONMENT}.database")
    val domain = applicationBundle.getString("domain")
    val matrixFields = ResourceBundle.getBundle("matrixFields")
    val userQueries = ResourceBundle.getBundle("queries.userQueries")
    val userProfileQueries = ResourceBundle.getBundle("queries.userProfileQueries")
    val mediaQueries = ResourceBundle.getBundle("queries.mediaQueries")
    val authQueries = ResourceBundle.getBundle("queries.authQueries")
    val deviceQueries = ResourceBundle.getBundle("queries.deviceQueries")
    val roomQueries = ResourceBundle.getBundle("queries.roomQueries")
    val roomEventQueries = ResourceBundle.getBundle("queries.roomEventQueries")
}