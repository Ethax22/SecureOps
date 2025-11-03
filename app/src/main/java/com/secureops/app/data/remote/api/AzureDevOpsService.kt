package com.secureops.app.data.remote.api

import com.secureops.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AzureDevOpsService {

    @GET("{organization}/{project}/_apis/build/builds")
    suspend fun getBuilds(
        @Path("organization") organization: String,
        @Path("project") project: String,
        @Query("api-version") apiVersion: String = "7.0",
        @Query("\$top") top: Int = 50
    ): Response<AzureBuildsResponse>

    @GET("{organization}/{project}/_apis/build/builds/{buildId}")
    suspend fun getBuild(
        @Path("organization") organization: String,
        @Path("project") project: String,
        @Path("buildId") buildId: Int,
        @Query("api-version") apiVersion: String = "7.0"
    ): Response<AzureBuild>

    @GET("{organization}/{project}/_apis/build/builds/{buildId}/logs")
    suspend fun getBuildLogs(
        @Path("organization") organization: String,
        @Path("project") project: String,
        @Path("buildId") buildId: Int,
        @Query("api-version") apiVersion: String = "7.0"
    ): Response<AzureLogsResponse>

    @POST("{organization}/{project}/_apis/build/builds/{buildId}/retry")
    suspend fun retryBuild(
        @Path("organization") organization: String,
        @Path("project") project: String,
        @Path("buildId") buildId: Int,
        @Query("api-version") apiVersion: String = "7.0"
    ): Response<AzureBuild>

    @PATCH("{organization}/{project}/_apis/build/builds/{buildId}")
    suspend fun cancelBuild(
        @Path("organization") organization: String,
        @Path("project") project: String,
        @Path("buildId") buildId: Int,
        @Query("api-version") apiVersion: String = "7.0",
        @Body body: AzureCancelRequest
    ): Response<AzureBuild>
}
