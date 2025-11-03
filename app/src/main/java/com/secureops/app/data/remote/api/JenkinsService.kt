package com.secureops.app.data.remote.api

import com.secureops.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface JenkinsService {

    @GET("api/json")
    suspend fun getJobs(
        @Query("tree") tree: String = "jobs[name,url,color,lastBuild[number,result,timestamp,duration]]"
    ): Response<JenkinsJobsResponse>

    @GET("job/{jobName}/api/json")
    suspend fun getJob(
        @Path("jobName") jobName: String,
        @Query("tree") tree: String = "builds[number,result,timestamp,duration,url]"
    ): Response<JenkinsJobResponse>

    @GET("job/{jobName}/{buildNumber}/api/json")
    suspend fun getBuild(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<JenkinsBuildResponse>

    @GET("job/{jobName}/{buildNumber}/consoleText")
    suspend fun getBuildLog(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int
    ): Response<String>

    @POST("job/{jobName}/build")
    suspend fun triggerBuild(
        @Path("jobName") jobName: String,
        @Header("Jenkins-Crumb") crumb: String? = null
    ): Response<Unit>

    @POST("job/{jobName}/{buildNumber}/stop")
    suspend fun stopBuild(
        @Path("jobName") jobName: String,
        @Path("buildNumber") buildNumber: Int,
        @Header("Jenkins-Crumb") crumb: String? = null
    ): Response<Unit>
}
