interface Service<T> {

    /**
     * Used to identify this Service in a map of services
     */
    val identifier: String
    suspend fun create(dto: DTO): T
    suspend fun findOne(id: String): T?
    suspend fun findAll(): List<T>
    suspend fun update(id: String, dto: DTO): T
    suspend fun remove(id: String)

}