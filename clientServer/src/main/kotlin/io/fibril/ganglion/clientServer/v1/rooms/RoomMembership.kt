package io.fibril.ganglion.clientServer.v1.rooms

import io.fibril.ganglion.clientServer.utils.DirectedGraph

enum class RoomMembershipState {
    JOIN,
    INVITE,
    KNOCK,
    LEAVE,
    BAN
}

object RoomMembership {
    val transitionGraph = DirectedGraph<RoomMembershipState>()
        .addVertex(RoomMembershipState.JOIN)
        .addVertex(RoomMembershipState.INVITE)
        .addVertex(RoomMembershipState.KNOCK)
        .addVertex(RoomMembershipState.LEAVE)
        .addVertex(RoomMembershipState.BAN)
        // EDGES

        // INVITE
        .addEdge(RoomMembershipState.INVITE, RoomMembershipState.JOIN)
        .addEdge(RoomMembershipState.INVITE, RoomMembershipState.LEAVE)
        .addEdge(RoomMembershipState.INVITE, RoomMembershipState.BAN)
        .addEdge(RoomMembershipState.INVITE, RoomMembershipState.KNOCK)

        //JOIN
        .addEdge(RoomMembershipState.JOIN, RoomMembershipState.LEAVE)
        .addEdge(RoomMembershipState.JOIN, RoomMembershipState.BAN)

        // KNOCK
        .addEdge(RoomMembershipState.KNOCK, RoomMembershipState.INVITE)
        .addEdge(RoomMembershipState.KNOCK, RoomMembershipState.JOIN)
        .addEdge(RoomMembershipState.KNOCK, RoomMembershipState.LEAVE)
        .addEdge(RoomMembershipState.KNOCK, RoomMembershipState.BAN)

        //LEAVE
        .addEdge(RoomMembershipState.LEAVE, RoomMembershipState.INVITE)
        .addEdge(RoomMembershipState.LEAVE, RoomMembershipState.JOIN)
        .addEdge(RoomMembershipState.LEAVE, RoomMembershipState.KNOCK)
        .addEdge(RoomMembershipState.LEAVE, RoomMembershipState.BAN)

        //BAN
        .addEdge(RoomMembershipState.BAN, RoomMembershipState.LEAVE)


    // Output = {join = RoomMembershipState.JOIN .... }
    val mapNameToMembershipState =
        RoomMembershipState.entries.groupBy { it.name.lowercase() }
            .mapValues { it.value.first() }

}

