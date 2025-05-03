package io.fibril.ganglion.clientServer.v1.roomEvents


object RoomEventNames {
    const val REACTION = "m.reaction"
    const val REDACTION = "m.redaction"


    const val MESSAGE = "m.room.message"

    object MessageEvents {
        const val TEXT = "m.text"
        const val EMOTE = "m.emote"
        const val NOTICE = "m.notice"
        const val IMAGE = "m.image"
        const val FILE = "m.file"
        const val AUDIO = "m.audio"
        const val VIDEO = "m.video"
    }

    object StateEvents {
        const val CREATE = "m.room.create"
        const val MEMBER = "m.room.member"
        const val TOPIC = "m.room.topic"
        const val NAME = "m.room.name"
        const val AVATAR = "m.room.avatar"
        const val JOIN_RULES = "m.room.join_rules"
        const val GUEST_ACCESS = "m.room.guest_access"
        const val HISTORY_VISIBILITY = "m.room.history_visibility"
        const val POWER_LEVELS = "m.room.power_levels"
        const val PINNED_EVENTS = "m.room.pinned_events"
        const val ALIASES = "m.room.aliases"
        const val CANONICAL_ALIAS = "m.room.canonical_alias"
        const val ENCRYPTION = "m.room.encryption"
        const val SERVER_ACL = "m.room.server_acl"
        const val THIRD_PARTY_INVITE = "m.room.third_party_invite"
    }

    object CallEvents {
        const val INVITE = "m.call.invite"
        const val CANDIDATES = "m.call.candidates"
        const val ANSWER = "m.call.answer"
        const val SELECT_ANSWER = "m.call.select_answer"
        const val HANGUP = "m.call.hangup"
        const val REJECT = "m.call.reject"
        const val NEGOTIATE = "m.call.negotiate"
        const val SDP_STREAM_METADATA_CHANGED = "m.call.sdp_stream_metadata_changed"
    }
}