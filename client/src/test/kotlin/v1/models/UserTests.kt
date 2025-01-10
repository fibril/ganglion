package v1.models

import io.fibril.ganglion.client.DTO
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Test
import io.fibril.ganglion.client.v1.users.models.User
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTests {
    @Test
    fun `user model validates correctly`() {
        val user1 = User("userId")
        assertTrue { user1.validate() }

        assertFalse { DTO.validate(JsonObject().put("fakeKey", "fakeValue"), user1.schema) }
    }
}