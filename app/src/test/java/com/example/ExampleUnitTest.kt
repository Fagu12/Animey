package com.example

import okhttp3.HttpUrl
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testTask4_HttpUrlBuilder() {
    val expected = "https://just4anime.online/api/advanced-search?page=1&perPage=20&query=Frieren&sort=%5B%22POPULARITY_DESC%22%5D"
    
    val builtFromEncoded = HttpUrl.Builder()
      .scheme("https")
      .host("just4anime.online")
      .addPathSegment("api")
      .addPathSegment("advanced-search")
      .addQueryParameter("page", "1")
      .addQueryParameter("perPage", "20")
      .addQueryParameter("query", "Frieren")
      .addEncodedQueryParameter("sort", "%5B%22POPULARITY_DESC%22%5D")
      .build()
      
    assertEquals(expected, builtFromEncoded.toString())
  }
}
