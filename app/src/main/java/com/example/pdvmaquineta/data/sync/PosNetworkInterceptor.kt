package com.example.pdvmaquineta.data.sync

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// Monta a URL final combinando a URL base configurada (que PODE ter um prefixo
// de caminho, ex.: https://host/api) com o caminho relativo da rota
// (/v1/pos/...). Também adiciona o token do terminal. Assim a URL base pode ser
// dinâmica e conter prefixo, sem recriar o Retrofit.
class PosNetworkInterceptor @Inject constructor(
    private val settings: SyncSettings
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val base = settings.baseUrl.trim().trimEnd('/')
        if (base.isNotEmpty()) {
            val full = buildString {
                append(base)                       // ex.: https://host/api
                append(request.url.encodedPath)    // ex.: /v1/pos/activate
                request.url.encodedQuery?.let { append("?").append(it) }
            }
            full.toHttpUrlOrNull()?.let { newUrl ->
                request = request.newBuilder().url(newUrl).build()
            }
        }

        val token = settings.token
        if (token.isNotBlank()) {
            request = request.newBuilder().header("Authorization", "Bearer $token").build()
        }

        return chain.proceed(request)
    }
}
