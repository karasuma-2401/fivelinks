package com.karasuma.fivelinks.fivelinks_cmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform