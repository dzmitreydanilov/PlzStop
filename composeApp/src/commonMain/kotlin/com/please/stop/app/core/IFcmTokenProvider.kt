package com.please.stop.app.core

interface IFcmTokenProvider {
    suspend fun getToken(): String?
}
