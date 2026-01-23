package com.celdy.groufr.di

import com.celdy.groufr.BuildConfig
import com.celdy.groufr.data.network.ApiConstants
import com.celdy.groufr.data.network.ApiService
import com.celdy.groufr.data.network.AuthApiService
import com.celdy.groufr.data.network.AuthAuthenticator
import com.celdy.groufr.data.network.AuthInterceptor
import com.celdy.groufr.data.storage.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return interceptor
    }

    /**
     * OkHttpClient for auth-only operations (token refresh).
     * Does NOT include the Authenticator to avoid circular dependency.
     */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Retrofit for auth-only operations.
     */
    @Provides
    @Singleton
    @AuthClient
    fun provideAuthRetrofit(
        @AuthClient okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * API service for auth operations (token refresh).
     * Used by AuthAuthenticator.
     */
    @Provides
    @Singleton
    fun provideAuthApiService(
        @AuthClient retrofit: Retrofit
    ): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    /**
     * AuthAuthenticator for automatic token refresh on 401 responses.
     */
    @Provides
    @Singleton
    fun provideAuthAuthenticator(
        tokenStore: TokenStore,
        authApiService: AuthApiService
    ): AuthAuthenticator {
        return AuthAuthenticator(tokenStore, authApiService)
    }

    /**
     * Main OkHttpClient with AuthInterceptor and AuthAuthenticator.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authAuthenticator: AuthAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authAuthenticator)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
