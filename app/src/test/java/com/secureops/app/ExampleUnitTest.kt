package com.secureops.app

import com.secureops.app.ml.VoiceCommandProcessor
import com.secureops.app.domain.model.CommandIntent
import org.junit.Test
import org.junit.Assert.*

class VoiceCommandProcessorTest {

    private val processor = VoiceCommandProcessor()

    @Test
    fun `test build status query intent detection`() {
        val command = processor.processVoiceInput("What's the status of my builds?")
        assertEquals(CommandIntent.QUERY_BUILD_STATUS, command.intent)
    }

    @Test
    fun `test failure explanation intent detection`() {
        val command = processor.processVoiceInput("Why did build #123 fail?")
        assertEquals(CommandIntent.EXPLAIN_FAILURE, command.intent)
        assertEquals("123", command.parameters["buildNumber"])
    }

    @Test
    fun `test risky deployments intent detection`() {
        val command = processor.processVoiceInput("Any risky deployments today?")
        assertEquals(CommandIntent.CHECK_RISKY_DEPLOYMENTS, command.intent)
        assertEquals("today", command.parameters["timeRange"])
    }

    @Test
    fun `test rerun build intent detection`() {
        val command = processor.processVoiceInput("Rerun the last failed build")
        assertEquals(CommandIntent.RERUN_BUILD, command.intent)
        assertEquals("last_failed", command.parameters["target"])
    }

    @Test
    fun `test rollback intent detection`() {
        val command = processor.processVoiceInput("Rollback the deployment")
        assertEquals(CommandIntent.ROLLBACK_DEPLOYMENT, command.intent)
    }

    @Test
    fun `test response generation for build status`() {
        val data = mapOf(
            "totalBuilds" to 10,
            "failedBuilds" to 2,
            "runningBuilds" to 1
        )
        val response = processor.generateResponse(
            CommandIntent.QUERY_BUILD_STATUS,
            data,
            true
        )
        assertTrue(response.contains("2 failed build"))
        assertTrue(response.contains("10 total"))
    }
}
