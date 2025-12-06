package com.stremio.app.api

import com.stremio.app.data.models.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface AddonApi {
    
    @GET("manifest.json")
    suspend fun getManifest(): Response<AddonManifest>
    
    @GET
    suspend fun getManifestFromUrl(@Url url: String): Response<AddonManifest>
    
    @GET("catalog/{type}/{id}.json")
    suspend fun getCatalog(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<CatalogResponse>
    
    @GET("catalog/{type}/{id}/{extra}.json")
    suspend fun getCatalogWithExtra(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("extra") extra: String
    ): Response<CatalogResponse>
    
    @GET("catalog/{type}/{id}/skip={skip}.json")
    suspend fun getCatalogWithSkip(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("skip") skip: Int
    ): Response<CatalogResponse>
    
    @GET("meta/{type}/{id}.json")
    suspend fun getMeta(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<MetaResponse>
    
    @GET("stream/{type}/{id}.json")
    suspend fun getStreams(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<StreamsResponse>
    
    @GET("stream/{type}/{id}/{videoId}.json")
    suspend fun getStreamsForVideo(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("videoId") videoId: String
    ): Response<StreamsResponse>
    
    @GET("subtitles/{type}/{id}.json")
    suspend fun getSubtitles(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<SubtitlesResponse>
    
    @GET("subtitles/{type}/{id}/{extra}.json")
    suspend fun getSubtitlesWithExtra(
        @Path("type") type: String,
        @Path("id") id: String,
        @Path("extra") extra: String
    ): Response<SubtitlesResponse>
}

interface CinemetaApi {
    
    @GET("catalog/{type}/top.json")
    suspend fun getTopCatalog(@Path("type") type: String): Response<CatalogResponse>
    
    @GET("catalog/{type}/top/genre={genre}.json")
    suspend fun getCatalogByGenre(
        @Path("type") type: String,
        @Path("genre") genre: String
    ): Response<CatalogResponse>
    
    @GET("catalog/{type}/top/search={query}.json")
    suspend fun search(
        @Path("type") type: String,
        @Path("query") query: String
    ): Response<CatalogResponse>
    
    @GET("meta/{type}/{id}.json")
    suspend fun getMeta(
        @Path("type") type: String,
        @Path("id") id: String
    ): Response<MetaResponse>
}
