package io.fibril.ganglion.clientServer.v1.authentication


enum class RoleType {
    ADMIN,
    USER,
    TRUSTED_APPLICATION,
    BOT,
    GUEST
}

val roleHierarchy = mapOf<RoleType, Int>(
    RoleType.GUEST to 1,
    RoleType.BOT to 2,
    RoleType.TRUSTED_APPLICATION to 3,
    RoleType.USER to 4,
    RoleType.ADMIN to 5
)
