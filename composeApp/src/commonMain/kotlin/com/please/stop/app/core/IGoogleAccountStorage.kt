package com.please.stop.app.core

import com.please.stop.app.core.models.data.GoogleAccountLink

interface IGoogleAccountStorage {
    suspend fun write(link: GoogleAccountLink)
    suspend fun read(): GoogleAccountLink?
    suspend fun delete()
}
