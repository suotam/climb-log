package com.example.climblog.domain.model

data class WishlistEntry(
    val id: Long = 0,
    val routeId: Long,
    val route: Route? = null,
    val note: String = "",
    val priority: Int = 2, // 1 = nízká, 2 = střední, 3 = vysoká
    val addedAt: Long = System.currentTimeMillis()
)
