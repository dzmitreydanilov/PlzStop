package com.please.stop.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform