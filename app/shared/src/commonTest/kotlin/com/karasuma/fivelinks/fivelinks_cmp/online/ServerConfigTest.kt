package com.karasuma.fivelinks.fivelinks_cmp.online

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerConfigTest {
    @Test
    fun websocketUrls_mustUseTheGameSocketPath() {
        assertNull(ServerConfig.validationError("ws://localhost:8080/ws"))
        assertNull(ServerConfig.validationError("wss://play.example.com/ws"))
        assertEquals(
            "Bản này chưa được cấu hình máy chủ online.",
            ServerConfig.validationError(" "),
        )
        assertEquals(
            "Địa chỉ máy chủ phải bắt đầu bằng ws:// hoặc wss://.",
            ServerConfig.validationError("https://play.example.com/ws"),
        )
        assertEquals(
            "Địa chỉ máy chủ phải kết thúc bằng /ws.",
            ServerConfig.validationError("wss://play.example.com"),
        )
        assertEquals(
            "Bản phát hành chỉ chấp nhận máy chủ bảo mật wss://.",
            ServerConfig.validationError("ws://localhost:8080/ws", requireSecure = true),
        )
    }
}
