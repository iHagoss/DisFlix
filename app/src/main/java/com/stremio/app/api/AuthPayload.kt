package com.stremio.app.api

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.stremio.app.data.models.GDPRConsent
import com.stremio.app.data.models.User
import java.text.SimpleDateFormat
import java.util.*

sealed class AuthPayload {
    data class Login(
        val email: String,
        val password: String,
        val facebook: Boolean = false
    ) : AuthPayload()
    
    data class Register(
        val email: String,
        val password: String,
        val gdprConsent: GDPRConsent
    ) : AuthPayload()
    
    data class Logout(
        val authKey: String
    ) : AuthPayload()
}

class AuthPayloadTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!AuthPayload::class.java.isAssignableFrom(rawType)) {
            return null
        }
        
        @Suppress("UNCHECKED_CAST")
        return AuthPayloadTypeAdapter(gson) as TypeAdapter<T>
    }
}

class AuthPayloadTypeAdapter(private val gson: Gson) : TypeAdapter<AuthPayload>() {
    
    override fun write(out: JsonWriter, value: AuthPayload?) {
        if (value == null) {
            out.nullValue()
            return
        }
        
        out.beginObject()
        out.name("type").value("Auth")
        
        when (value) {
            is AuthPayload.Login -> {
                out.name("type").value("Login")
                out.name("email").value(value.email)
                out.name("password").value(value.password)
                out.name("facebook").value(value.facebook)
            }
            is AuthPayload.Register -> {
                out.name("type").value("Register")
                out.name("email").value(value.email)
                out.name("password").value(value.password)
                out.name("gdprConsent")
                gson.toJson(value.gdprConsent, GDPRConsent::class.java, out)
            }
            is AuthPayload.Logout -> {
                out.name("type").value("Logout")
                out.name("authKey").value(value.authKey)
            }
        }
        
        out.endObject()
    }
    
    override fun read(reader: JsonReader): AuthPayload? {
        return null
    }
}

data class SaveUserPayload(
    val authKey: String,
    val user: User
)

class SaveUserPayloadTypeAdapterFactory : TypeAdapterFactory {
    override fun <T : Any?> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType != SaveUserPayload::class.java) {
            return null
        }
        
        @Suppress("UNCHECKED_CAST")
        return SaveUserPayloadTypeAdapter(gson) as TypeAdapter<T>
    }
}

class SaveUserPayloadTypeAdapter(private val gson: Gson) : TypeAdapter<SaveUserPayload>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    override fun write(out: JsonWriter, value: SaveUserPayload?) {
        if (value == null) {
            out.nullValue()
            return
        }
        
        out.beginObject()
        out.name("type").value("SaveUser")
        out.name("authKey").value(value.authKey)
        
        val userJson = gson.toJsonTree(value.user).asJsonObject
        for ((key, jsonValue) in userJson.entrySet()) {
            out.name(key)
            gson.toJson(jsonValue, out)
        }
        
        out.endObject()
    }
    
    override fun read(reader: JsonReader): SaveUserPayload? {
        return null
    }
}
