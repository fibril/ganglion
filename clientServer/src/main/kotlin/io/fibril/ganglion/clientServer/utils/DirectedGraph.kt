package io.fibril.ganglion.clientServer.utils

class DirectedGraph<T> {
    private val adjacencyList: MutableMap<T, MutableList<T>> = mutableMapOf()

    fun addVertex(vertex: T): DirectedGraph<T> {
        if (!adjacencyList.containsKey(vertex)) {
            adjacencyList[vertex] = mutableListOf()
        }
        return this
    }

    fun addEdge(source: T, destination: T): DirectedGraph<T> {
        if (!adjacencyList.containsKey(source)) {
            addVertex(source)
        }
        if (!adjacencyList.containsKey(destination)) {
            addVertex(destination)
        }
        adjacencyList[source]?.add(destination)
        return this
    }

    infix fun hasPathFrom(vertex: T): VertexToVertexPathChecker<T> = VertexToVertexPathChecker(this, vertex)

    fun printGraph() {
        for ((vertex, edges) in adjacencyList) {
            println("$vertex -> ${edges.joinToString(", ")}")
        }
    }

    class VertexToVertexPathChecker<P>(private val graph: DirectedGraph<P>, private val source: P) {
        infix fun to(destination: P): Boolean {
            return graph.adjacencyList[source]?.contains(destination) ?: false
        }
    }
}