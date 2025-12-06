package com.stremio.app.data.models

import com.google.gson.annotations.SerializedName

data class CatalogResponse(
    val metas: List<MetaItemPreview> = emptyList(),
    @SerializedName("hasMore")
    val hasMore: Boolean = false,
    @SerializedName("cacheMaxAge")
    val cacheMaxAge: Int? = null
)

data class MetaResponse(
    val meta: MetaItem? = null,
    @SerializedName("cacheMaxAge")
    val cacheMaxAge: Int? = null
)

data class StreamsResponse(
    val streams: List<Stream> = emptyList(),
    @SerializedName("cacheMaxAge")
    val cacheMaxAge: Int? = null
)

data class SubtitlesResponse(
    val subtitles: List<Subtitles> = emptyList(),
    @SerializedName("cacheMaxAge")
    val cacheMaxAge: Int? = null
)

data class CatalogPage(
    val request: ResourceRequest,
    val content: CatalogResponse,
    val addon: Addon
)

data class CatalogWithFilters(
    val catalog: ManifestCatalog,
    val addon: Addon,
    val selectedExtra: List<ExtraValue> = emptyList()
) {
    val selectable: List<SelectableExtra>
        get() = catalog.extra.map { prop ->
            SelectableExtra(
                name = prop.name,
                options = prop.options,
                selected = selectedExtra.find { it.name == prop.name }?.value,
                isRequired = prop.isRequired
            )
        }
    
    fun buildRequest(): ResourceRequest {
        return ResourceRequest(
            base = addon.transportUrl,
            path = ResourcePath(
                resource = "catalog",
                type = catalog.type,
                id = catalog.id,
                extra = selectedExtra
            )
        )
    }
}

data class SelectableExtra(
    val name: String,
    val options: List<String>,
    val selected: String?,
    val isRequired: Boolean
)

data class DiscoverRow(
    val title: String,
    val items: List<MetaItemPreview>,
    val catalog: ManifestCatalog,
    val addon: Addon,
    val deepLink: String
)

data class SearchResult(
    val query: String,
    val items: List<MetaItemPreview>,
    val addon: Addon
)
