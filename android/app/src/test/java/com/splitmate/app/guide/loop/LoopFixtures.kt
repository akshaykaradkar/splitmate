package com.splitmate.app.guide.loop

/** Loads text fixtures from `src/test/resources/guide/loop/`. */
object LoopFixtures {
    fun text(name: String): String {
        val stream = requireNotNull(LoopFixtures::class.java.classLoader?.getResourceAsStream("guide/loop/$name")) {
            "Missing fixture guide/loop/$name"
        }
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
