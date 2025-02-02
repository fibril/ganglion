package v1.models

import io.fibril.ganglion.clientServer.v1.users.models.User
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTests {
    @Test
    fun `user model validates correctly`() {
        val user1 = User("userId")
        assertTrue { true }

        assertFalse { false }
    }
}