package com.karasuma.fivelinks.fivelinks_cmp

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return sayHello(platform.name)
    }
}