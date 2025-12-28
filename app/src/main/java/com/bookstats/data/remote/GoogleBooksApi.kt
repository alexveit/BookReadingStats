package com.bookstats.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client for the Google Books API.
 * Used to fetch book metadata including cover images, authors, page count, and descriptions.
 * 
 * This API is free and doesn't require an API key for basic searches.
 */
@Singleton
class GoogleBooksApi @Inject constructor() {
    
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        // Don't follow redirects for HEAD requests so we can check actual availability
        followRedirects = true
    }
    
    /**
     * Search for books by title.
     * 
     * @param title The book title to search for
     * @param maxResults Maximum number of results to return (1-40)
     * @return List of book cover results, or empty list on error
     */
    suspend fun searchByTitle(title: String, maxResults: Int = 5): List<BookCoverResult> {
        return try {
            // Ktor automatically URL-encodes parameters, handling special characters properly
            val response: GoogleBooksResponse = client.get(
                "https://www.googleapis.com/books/v1/volumes"
            ) {
                parameter("q", "intitle:$title")
                parameter("maxResults", maxResults.coerceIn(1, 40))
                parameter("printType", "books")
            }.body()
            
            response.items?.mapNotNull { item ->
                val volumeInfo = item.volumeInfo ?: return@mapNotNull null
                val imageLinks = volumeInfo.imageLinks ?: return@mapNotNull null
                
                // Get the best available cover URL
                val coverUrl = getBestCoverUrl(imageLinks)
                    ?: return@mapNotNull null
                
                BookCoverResult(
                    title = volumeInfo.title ?: "Unknown",
                    authors = volumeInfo.authors ?: emptyList(),
                    pageCount = volumeInfo.pageCount,
                    description = volumeInfo.description,
                    categories = volumeInfo.categories ?: emptyList(),
                    thumbnailUrl = imageLinks.smallThumbnail?.toHttps(),
                    coverUrl = coverUrl
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Find the best matching cover for a book title.
     * Returns a single result or null if no match found.
     */
    suspend fun findBestMatch(title: String): BookCoverResult? {
        return searchByTitle(title, maxResults = 3).firstOrNull()
    }
    
    /**
     * Check if a URL returns a valid image (not a placeholder/error).
     * Makes a HEAD request to check availability without downloading the full image.
     */
    suspend fun isUrlValid(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        
        return try {
            val response: HttpResponse = client.head(url)
            // Check for successful status and that it's actually an image
            response.status.isSuccess() && 
                response.contentType()?.match(ContentType.Image.Any) == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get the best working cover URL for a BookCoverResult.
     * Tries the high-res coverUrl first, falls back to thumbnailUrl if it fails.
     * 
     * @param cover The book cover result to get URL for
     * @return The best available URL, or null if none work
     */
    suspend fun getBestWorkingUrl(cover: BookCoverResult): String? {
        // Try high-res URL first
        if (isUrlValid(cover.coverUrl)) {
            return cover.coverUrl
        }
        
        // Fall back to thumbnail
        if (isUrlValid(cover.thumbnailUrl)) {
            return cover.thumbnailUrl
        }
        
        // Last resort: return thumbnail even without validation 
        // (the search results displayed it, so it probably works)
        return cover.thumbnailUrl ?: cover.coverUrl
    }
    
    /**
     * Get the highest quality cover URL available.
     */
    private fun getBestCoverUrl(imageLinks: ImageLinks): String? {
        // Prefer larger images, fall back to smaller ones
        val url = imageLinks.extraLarge
            ?: imageLinks.large
            ?: imageLinks.medium
            ?: imageLinks.thumbnail
            ?: imageLinks.smallThumbnail
            ?: return null
        
        return url.toHttps().removeEdgeCurl().increaseZoom()
    }
    
    /**
     * Convert HTTP to HTTPS for security.
     */
    private fun String.toHttps(): String {
        return if (startsWith("http://")) {
            replace("http://", "https://")
        } else this
    }
    
    /**
     * Remove the edge=curl parameter that adds a page curl effect.
     */
    private fun String.removeEdgeCurl(): String {
        return replace("&edge=curl", "").replace("edge=curl&", "")
    }
    
    /**
     * Increase zoom level for better quality images.
     */
    private fun String.increaseZoom(): String {
        return if (contains("zoom=")) {
            replace(Regex("zoom=\\d"), "zoom=2")
        } else if (contains("?")) {
            "$this&zoom=2"
        } else {
            "$this?zoom=2"
        }
    }
}

/**
 * Result from a book cover search.
 * Contains metadata useful for auto-filling book information.
 */
data class BookCoverResult(
    val title: String,
    val authors: List<String>,
    val pageCount: Int?,
    val description: String?,
    val categories: List<String>,
    val thumbnailUrl: String?,
    val coverUrl: String
) {
    /**
     * Authors formatted as comma-separated string.
     */
    val authorsFormatted: String
        get() = authors.joinToString(", ")
    
    /**
     * First category, or empty string if none.
     */
    val primaryCategory: String
        get() = categories.firstOrNull()?.let { category ->
            // Google Books categories can be paths like "Fiction / Fantasy / General"
            // Extract just the most specific part
            category.split("/").lastOrNull()?.trim() ?: category
        } ?: ""
    
    /**
     * All categories as comma-separated string.
     */
    val categoriesFormatted: String
        get() = categories.joinToString(", ")
}

// ============================================
// Google Books API Response DTOs
// ============================================

@Serializable
internal data class GoogleBooksResponse(
    val kind: String? = null,
    val totalItems: Int? = null,
    val items: List<VolumeItem>? = null
)

@Serializable
internal data class VolumeItem(
    val id: String? = null,
    val volumeInfo: VolumeInfo? = null
)

@Serializable
internal data class VolumeInfo(
    val title: String? = null,
    val authors: List<String>? = null,
    val pageCount: Int? = null,
    val imageLinks: ImageLinks? = null,
    val description: String? = null,
    val publishedDate: String? = null,
    val categories: List<String>? = null
)

@Serializable
internal data class ImageLinks(
    val smallThumbnail: String? = null,
    val thumbnail: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val large: String? = null,
    val extraLarge: String? = null
)
