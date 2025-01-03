package v1.models

import DTO
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Test
import v1.users.models.User
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTests {
    @Test
    fun `user model validates correctly`() {
        val user1 = User("userId")
        assertTrue { user1.validate() }

        assertFalse { DTO.validate(JsonObject().put("fake_key", "fake_value"), user1.schema) }
    }
}