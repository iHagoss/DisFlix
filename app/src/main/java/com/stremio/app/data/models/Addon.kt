package com.stremio.app.data.models

import com.google.gson.annotations.SerializedName

data class Addon(
    val manifest: AddonManifest,
    @SerializedName("transportUrl")
    val transportUrl: String,
    val flags: AddonFlags = AddonFlags()
)

data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    @SerializedName("contactEmail")
    val contactEmail: String? = null,
    val description: String? = null,
    val logo: String? = null,
    val background: String? = null,
    val types: List<String> = emptyList(),
    val resources: List<ManifestResource> = emptyList(),
    @SerializedName("idPrefixes")
    val idPrefixes: List<String>? = null,
    val catalogs: List<ManifestCatalog> = emptyList(),
    @SerializedName("addonCatalogs")
    val addonCatalogs: List<ManifestCatalog> = emptyList(),
    @SerializedName("behaviorHints")
    val behaviorHints: AddonBehaviorHints = AddonBehaviorHints()
) {
    fun isResourceSupported(resource: String, type: String, id: String): Boolean {
        val manifestResource = resources.find { it.name == resource } ?: return false
        
        val typesSupported = manifestResource.types?.contains(type) ?: types.contains(type)
        val idSupported = manifestResource.idPrefixes?.any { id.startsWith(it) }
            ?: idPrefixes?.any { id.startsWith(it) }
            ?: true
        
        return typesSupported && idSupported
    }
    
    fun getCatalog(type: String, id: String): ManifestCatalog? {
        return catalogs.find { it.type == type && it.id == id }
    }
}

data class ManifestResource(
    val name: String,
    val types: List<String>? = null,
    @SerializedName("idPrefixes")
    val idPrefixes: List<String>? = null
)

data class ManifestCatalog(
    val id: String,
    val type: String,
    val name: String? = null,
    val extra: List<ExtraProp> = emptyList(),
    @SerializedName("extraSupported")
    val extraSupported: List<String> = emptyList(),
    @SerializedName("extraRequired")
    val extraRequired: List<String> = emptyList()
) {
    fun isExtraSupported(extraName: String): Boolean {
        return extra.any { it.name == extraName } || extraSupported.contains(extraName)
    }
    
    fun getDefaultExtra(): List<ExtraValue> {
        return extra.filter { it.isRequired && it.options.isNotEmpty() }
            .map { ExtraValue(it.name, it.options.first()) }
    }
}

data class ExtraProp(
    val name: String,
    @SerializedName("isRequired")
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
    @SerializedName("optionsLimit")
    val optionsLimit: Int = 1
)

data class ExtraValue(
    val name: String,
    val value: String
)

data class AddonBehaviorHints(
    val adult: Boolean = false,
    val p2p: Boolean = false,
    val configurable: Boolean = false,
    @SerializedName("configurationRequired")
    val configurationRequired: Boolean = false
)

data class AddonFlags(
    val official: Boolean = false,
    val protected: Boolean = false
)

data class ResourcePath(
    val resource: String,
    val type: String,
    val id: String,
    val extra: List<ExtraValue> = emptyList()
) {
    fun toPath(): String {
        val basePath = "$resource/$type/$id"
        if (extra.isEmpty()) return basePath
        
        val extraStr = extra.joinToString("/") { "${it.name}=${it.value}" }
        return "$basePath/$extraStr"
    }
}

data class ResourceRequest(
    val base: String,
    val path: ResourcePath
) {
    fun toUrl(): String = "$base/${path.toPath()}.json"
}
