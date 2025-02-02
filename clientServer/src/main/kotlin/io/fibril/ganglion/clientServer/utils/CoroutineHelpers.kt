package io.fibril.ganglion.clientServer.utils

import kotlinx.coroutines.*

object CoroutineHelpers {
    private fun startScope(dispatcher: CoroutineDispatcher, block: suspend () -> Unit): Job {
        return CoroutineScope(dispatcher).launch {
            block()
        }
    }

    fun usingCoroutineScopeWithIODispatcher(block: suspend () -> Unit): Job {
        return startScope(Dispatchers.IO, block)
    }
}