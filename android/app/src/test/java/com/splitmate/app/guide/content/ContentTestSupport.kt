package com.splitmate.app.guide.content

import com.splitmate.app.data.guide.content.HttpFetcher
import com.splitmate.app.data.guide.content.HttpResponse
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

/** Loads recorded/synthetic fixtures from src/test/resources/guide/content/. */
object Fixtures {
    fun text(name: String): String {
        val stream = Fixtures::class.java.getResourceAsStream("/guide/content/$name")
            ?: error("Missing fixture: $name")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    /** Extracts a nested payload (e.g. "search_response") from a combined fixture file. */
    fun part(name: String, key: String): String = JSONObject(text(name)).getJSONObject(key).toString()

    /** Wraps a raw .wikitext fixture in an `action=parse` formatversion=2 response envelope. */
    fun parseResponse(title: String, revid: Long, wikitextFile: String): String =
        JSONObject()
            .put(
                "parse",
                JSONObject()
                    .put("title", title)
                    .put("pageid", 1)
                    .put("revid", revid)
                    .put("wikitext", text(wikitextFile))
            ).toString()

    fun ok(body: String) = HttpResponse(200, body, mapOf("Content-Type" to "application/json; charset=utf-8"))
}

/**
 * Deterministic fake [HttpFetcher]: routes by URL predicate; each route serves its responses in
 * order and repeats the last one. Unmatched URLs get a 404. Records every request.
 */
class FakeHttpFetcher : HttpFetcher {
    data class Request(val url: String, val headers: Map<String, String>)

    private class Route(val predicate: (String) -> Boolean, val responses: ArrayDeque<HttpResponse>, val failWith: IOException?)

    val requests = CopyOnWriteArrayList<Request>()
    private val routes = ArrayList<Route>()

    /** Serves [responses] for URLs containing ALL [fragments]. */
    fun on(fragments: List<String>, vararg responses: HttpResponse): FakeHttpFetcher {
        require(responses.isNotEmpty())
        synchronized(routes) {
            routes += Route({ url -> fragments.all { url.contains(it) } }, ArrayDeque(responses.toList()), null)
        }
        return this
    }

    fun on(fragment: String, vararg responses: HttpResponse): FakeHttpFetcher = on(listOf(fragment), *responses)

    fun failOn(fragment: String, error: IOException = IOException("simulated")): FakeHttpFetcher {
        synchronized(routes) { routes += Route({ it.contains(fragment) }, ArrayDeque(), error) }
        return this
    }

    override fun get(url: String, headers: Map<String, String>): HttpResponse {
        requests += Request(url, headers)
        val route = synchronized(routes) { routes.firstOrNull { it.predicate(url) } }
            ?: return HttpResponse(404, "{}")
        route.failWith?.let { throw it }
        synchronized(route) {
            return if (route.responses.size > 1) route.responses.removeFirst() else route.responses.first()
        }
    }

    fun urls(): List<String> = requests.map { it.url }
}
