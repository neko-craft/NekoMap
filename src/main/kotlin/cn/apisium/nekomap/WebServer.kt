package cn.apisium.nekomap

import io.ktor.application.*
import io.ktor.http.cio.websocket.send
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import org.redisson.Redisson
import org.redisson.config.Config

fun main() {
    val redis = Redisson.create(Config().apply { useSingleServer()
        .setAddress("redis://127.0.0.1:6397")
    })
    val chunks = redis.getMap<Long, ByteArray>("Paper_world_chunks")
    val server = embeddedServer(Netty, port = 8089) {
        install(WebSockets)
        routing {
            webSocket("/ws") {
                chunks.values(1).forEach { send(it) }
            }
        }
    }
    server.start(wait = true)
}