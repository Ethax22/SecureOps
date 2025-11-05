package com.secureops.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.secureops.app.data.remote.api.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    // Gson
    single {
        GsonBuilder()
            .setLenient()
            .create()
    }

    // OkHttpClient
    single {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.secureops.app.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // GitHub Retrofit
    single(named("github")) {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    // GitLab Retrofit
    single(named("gitlab")) {
        Retrofit.Builder()
            .baseUrl("https://gitlab.com/api/v4/")
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    // Jenkins Retrofit
    single(named("jenkins")) {
        Retrofit.Builder()
            .baseUrl("http://localhost:8080/") // Default, will be overridden by account baseUrl
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    // CircleCI Retrofit
    single(named("circleci")) {
        Retrofit.Builder()
            .baseUrl("https://circleci.com/")
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    // Azure DevOps Retrofit
    single(named("azure")) {
        Retrofit.Builder()
            .baseUrl("https://dev.azure.com/")
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(get()))
            .build()
    }

    // API Services
    single { get<Retrofit>(named("github")).create(GitHubService::class.java) }
    single { get<Retrofit>(named("gitlab")).create(GitLabService::class.java) }
    single { get<Retrofit>(named("jenkins")).create(JenkinsService::class.java) }
    single { get<Retrofit>(named("circleci")).create(CircleCIService::class.java) }
    single { get<Retrofit>(named("azure")).create(AzureDevOpsService::class.java) }
}
