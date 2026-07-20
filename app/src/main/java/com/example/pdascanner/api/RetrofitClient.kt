package com.example.pdascanner.api

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Cliente Retrofit configurado para HTTPS con mTLS.
 *
 * Soporta:
 * - HTTPS seguro con validación de certificado servidor (ca.pem)
 * - Certificado de cliente para autenticación mTLS (client.p12)
 * - Timeouts configurables
 */
object RetrofitClient {

    // ============================================================================
    // CONFIGURACIÓN
    // ============================================================================

    private const val BASE_URL = "https://192.168.29.13:444"
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    private const val CLIENT_CERT_PASSWORD = "rodavigo2026"

    // CORREGIDO: @Volatile asegura que el cambio en la variable sea visible instantáneamente para todos los hilos
    @Volatile
    private var apiService: ApiService? = null


    // ============================================================================
    // API PÚBLICA
    // ============================================================================

    fun getInstance(context: Context): ApiService {
        // CORREGIDO: Patrón de doble comprobación de bloqueo (Double-Checked Locking) para evitar condiciones de carrera
        return apiService ?: synchronized(this) {
            apiService ?: createApiService(context).also { apiService = it }
        }
    }


    // ============================================================================
    // CONSTRUCCIÓN DEL CLIENTE
    // ============================================================================

    private fun createApiService(context: Context): ApiService {
        val httpClient = buildOkHttpClient(context)

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun buildOkHttpClient(context: Context): OkHttpClient {
        val trustManager = getTrustManager(context)
        val sslContext = createSSLContext(context, trustManager)

        return OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            // OPCIONAL / RECOMENDADO: Si el certificado ca.pem está emitido para un dominio (ej. rodavigo.local)
            // pero estás atacando por IP directa (192.168.29.13), el validador de nombres SSL fallará.
            // Con esto permitimos la conexión HTTPS sin que chille por discrepancia de IP/Hostname.
            .hostnameVerifier(HostnameVerifier { hostname, _ ->
                hostname == "192.168.29.13" || hostname == "pc-paco.rodavigo.local"
            })
            .build()
    }


    // ============================================================================
    // CERTIFICADO DE SERVIDOR — TrustManager
    // ============================================================================

    private fun getTrustManager(context: Context): X509TrustManager {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val caInput: InputStream = context.resources.openRawResource(com.example.pdascanner.R.raw.ca)
            val ca = certificateFactory.generateCertificate(caInput)
            caInput.close()

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("ca", ca)

            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(keyStore)

            trustManagerFactory.trustManagers[0] as X509TrustManager
        } catch (e: Exception) {
            throw RuntimeException("Error cargando certificado CA del servidor", e)
        }
    }


    // ============================================================================
    // CERTIFICADO DE CLIENTE — KeyManager (mTLS)
    // ============================================================================

    private fun getKeyManagerFactory(context: Context): KeyManagerFactory {
        return try {
            val p12Input: InputStream = context.resources.openRawResource(com.example.pdascanner.R.raw.client)
            val clientKeyStore = KeyStore.getInstance("PKCS12")
            clientKeyStore.load(p12Input, CLIENT_CERT_PASSWORD.toCharArray())
            p12Input.close()

            val keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
            )
            keyManagerFactory.init(clientKeyStore, CLIENT_CERT_PASSWORD.toCharArray())
            keyManagerFactory
        } catch (e: Exception) {
            Log.e("MTLS_ERROR", "Error cargando p12: ${e.javaClass.simpleName} | ${e.message}")
            Log.e("MTLS_ERROR", "Causa raíz: ${e.cause?.javaClass?.simpleName} | ${e.cause?.message}")
            throw RuntimeException("Error cargando certificado de cliente (client.p12)", e)
        }
    }

    // ============================================================================
    // SSL CONTEXT — combina servidor + cliente
    // ============================================================================

    private fun createSSLContext(context: Context, trustManager: X509TrustManager): SSLContext {
        return try {
            val keyManagerFactory = getKeyManagerFactory(context)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
                keyManagerFactory.keyManagers,  // Certificado cliente → nginx
                arrayOf(trustManager),           // Certificado servidor → validación CA
                java.security.SecureRandom()
            )
            sslContext
        } catch (e: Exception) {
            throw RuntimeException("Error creando SSL context con mTLS", e)
        }
    }
}