package io.fibril.ganglion.client.utils

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