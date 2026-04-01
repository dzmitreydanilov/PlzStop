package com.please.stop.app.core.models.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val dateOfBirth: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val isPremium: Boolean = false
) {

    fun getDisplayName(): String? =
        listOfNotNull(firstName, lastName)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" ")
}
