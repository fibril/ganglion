interface Repository<T> {
    suspend fun find(id: String): T?
    suspend fun findAll(): List<T>
}