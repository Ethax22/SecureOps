package com.secureops.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// Jenkins Jobs Response
data class JenkinsJobsResponse(
    @SerializedName("jobs")
    val jobs: List<JenkinsJob>
)

data class JenkinsJob(
    @SerializedName("name")
    val name: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("color")
    val color: String, // blue, red, yellow, etc.

    @SerializedName("lastBuild")
    val lastBuild: JenkinsLastBuild?
)

data class JenkinsLastBuild(
    @SerializedName("number")
    val number: Int,

    @SerializedName("result")
    val result: String?, // SUCCESS, FAILURE, UNSTABLE, ABORTED

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("duration")
    val duration: Long
)

// Jenkins Job Response
data class JenkinsJobResponse(
    @SerializedName("name")
    val name: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("builds")
    val builds: List<JenkinsBuildInfo>
)

data class JenkinsBuildInfo(
    @SerializedName("number")
    val number: Int,

    @SerializedName("result")
    val result: String?,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("duration")
    val duration: Long,

    @SerializedName("url")
    val url: String
)

// Jenkins Build Response
data class JenkinsBuildResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("number")
    val number: Int,

    @SerializedName("result")
    val result: String?,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("duration")
    val duration: Long,

    @SerializedName("url")
    val url: String,

    @SerializedName("building")
    val building: Boolean,

    @SerializedName("changeSet")
    val changeSet: JenkinsChangeSet?
)

data class JenkinsChangeSet(
    @SerializedName("items")
    val items: List<JenkinsChange>
)

data class JenkinsChange(
    @SerializedName("commitId")
    val commitId: String?,

    @SerializedName("author")
    val author: JenkinsAuthor?,

    @SerializedName("msg")
    val msg: String?
)

data class JenkinsAuthor(
    @SerializedName("fullName")
    val fullName: String
)
