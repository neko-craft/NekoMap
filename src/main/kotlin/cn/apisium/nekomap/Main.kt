package cn.apisium.nekomap

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import org.json.simple.JSONObject
import redis.clients.jedis.Jedis

private fun getBytes(data: Long) = byteArrayOf(
    (data and 0xff).toByte(),
    (data shr 8 and 0xff).toByte(),
    (data shr 16 and 0xff).toByte(),
    (data shr 24 and 0xff).toByte(),
    (data shr 32 and 0xff).toByte(),
    (data shr 40 and 0xff).toByte(),
    (data shr 48 and 0xff).toByte(),
    (data shr 56 and 0xff).toByte()
)

fun ByteBuf.writeVarInt(value: Int): ByteBuf {
    do {
        var temp = value and 0b01111111 ushr 7
        if (value != 0) temp = temp or 0b10000000
        writeByte(temp)
    } while (value != 0)
    return this
}

@Suppress("UNUSED", "DEPRECATION")
class Main: JavaPlugin(), Listener {
    private val idField = Material::class.java.getDeclaredField("id").apply { isAccessible = true }
    private lateinit var redis: Jedis

    override fun onEnable() {
        logger.info("Loading...")
        val world = server.getWorld("world")
        val chunk = world!!.spawnLocation.chunk

        val redis = Jedis("127.0.0.1", 6397)

        val json = JSONObject()
        Material.values()
            .filter { !it.name.startsWith("LEGACY_") }
            .forEach { json[idField.getInt(it)] = it.name.toLowerCase() }
        redis["${server.name}_${world.name}_blocks"] = json.toJSONString()
        server.pluginManager.registerEvents(this, this)
    }

    override fun onDisable() {
        redis.disconnect()
    }

    @EventHandler
    private fun onChunkLoad(chunk: ChunkLoadEvent) {
        getChunk(chunk.chunk)
    }

    private fun getChunk(chunk: Chunk) {
        val buf = ByteBufAllocator.DEFAULT.heapBuffer()
        val world = chunk.world
        val cz = chunk.z shl 4
        val cx = chunk.x shl 4
        buf.writeVarInt(chunk.x).writeVarInt(chunk.z)
        for (z in 0..15) for (x in 0..15) {
            var block = world.getHighestBlockAt(cx or x, cz or z)
            var i = 0
            while (block.type.isTransparent && i++ < 10) block = block.getRelative(0, -1, 0)
            buf.writeMedium(idField.getInt(block.type)).writeByte(block.y)
            if (block.type == Material.GRASS_BLOCK) buf.writeDouble(block.temperature).writeDouble(block.humidity)
        }
        redis.hset("${server.name}_${world.name}_chunks".toByteArray(), getBytes(chunk.chunkSnapshot), buf.array())
    }
}