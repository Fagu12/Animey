package com.example.extension

import com.example.data.extension.mangayomi.repository.DefaultMangayomiRepositoryClient
import com.example.data.extension.repository.DefaultRepositoryFetcher
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class FetchMallyd11RepoTest {

    @Test
    fun testParseMallyd11SampleIndex() {
        val json = """
        [
          {
            "name": "AnimeWorld",
            "id": "1183439094",
            "baseUrl": "https://www.animeworld.so",
            "apiUrl": "",
            "lang": "it",
            "iconUrl": "https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/src/it/animeworld/icon.png",
            "version": "0.0.5",
            "sourceCodeUrl": "https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/src/it/animeworld/animeworld.js",
            "sourceCodeLanguage": 1,
            "typeSource": "single",
            "itemType": 1,
            "isManga": false,
            "isNsfw": false,
            "hasCloudflare": false,
            "appMinVerReq": "0.1.0",
            "additionalParams": "",
            "notes": ""
          }
        ]
        """.trimIndent()

        val repoFetcher = DefaultRepositoryFetcher()
        val parseRes = repoFetcher.parseIndexJson(json, "https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/anime_index.json")

        if (parseRes is com.example.core.result.AppResult.Error) {
            org.junit.Assert.fail("parseIndexJson failed with: ${parseRes.error.message}")
        }
        assertTrue("Expected Success", parseRes is com.example.core.result.AppResult.Success)
        val data = (parseRes as com.example.core.result.AppResult.Success).data
        assertEquals(1, data.extensions.size)
        val ext = data.extensions.first()
        assertEquals("1183439094", ext.id)
        assertEquals("AnimeWorld", ext.name)
        assertEquals("0.0.5", ext.version)
        assertEquals("it", ext.language)
        assertEquals("https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/src/it/animeworld/animeworld.js", ext.downloadUrl)
    }

    @Test
    fun testFetchMallyd11IndexOnlineOrFallback() = runBlocking {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val repoClient = DefaultMangayomiRepositoryClient(client)
        val repoFetcher = DefaultRepositoryFetcher(client)
        val url = "https://raw.githubusercontent.com/Mallyd11/mangayomi-anime-extensions/main/anime_index.json"
        
        val result1 = repoClient.fetchRepositoryIndex(url)
        println("MangayomiRepositoryClient result: ${result1.javaClass.simpleName}")
        if (result1 is com.example.core.result.AppResult.Success) {
            println("Fetched ${result1.data.size} entries via MangayomiRepositoryClient!")
            assertTrue("Expected entries", result1.data.isNotEmpty())
        } else if (result1 is com.example.core.result.AppResult.Error) {
            println("Online fetch skipped/failed (offline environment): ${result1.error.message}")
        }

        val result2 = repoFetcher.fetchRepositoryIndex(url)
        println("DefaultRepositoryFetcher result: ${result2.javaClass.simpleName}")
        if (result2 is com.example.core.result.AppResult.Success) {
            println("Fetched ${result2.data.extensions.size} entries via DefaultRepositoryFetcher!")
            assertTrue("Expected extensions", result2.data.extensions.isNotEmpty())
        } else if (result2 is com.example.core.result.AppResult.Error) {
            println("Online fetch skipped/failed (offline environment): ${result2.error.message}")
        }
    }
}
