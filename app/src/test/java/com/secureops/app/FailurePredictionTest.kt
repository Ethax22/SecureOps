package com.secureops.app

import android.content.Context
import com.secureops.app.ml.FailurePredictionModel
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.kotlin.mock

class FailurePredictionTest {

    private lateinit var model: FailurePredictionModel
    private val mockContext: Context = mock()

    @Before
    fun setup() {
        model = FailurePredictionModel(mockContext)
    }

    @Test
    fun `test failure prediction with high risk factors`() {
        val commitDiff = "A".repeat(1000) // Large commit
        val testHistory = List(10) { it % 3 != 0 } // 70% failure rate
        val logs = "ERROR ERROR ERROR WARNING WARNING"

        val (riskPercentage, confidence) = model.predictFailure(
            commitDiff,
            testHistory,
            logs
        )

        assertTrue(riskPercentage > 50f)
        assertTrue(confidence > 0f && confidence <= 1f)
    }

    @Test
    fun `test causal factors identification for large commit`() {
        val commitDiff = "line\n".repeat(600)
        val testHistory = emptyList<Boolean>()
        val logs = ""

        val factors = model.identifyCausalFactors(commitDiff, testHistory, logs)

        assertTrue(factors.any { it.contains("Large commit size") })
    }

    @Test
    fun `test causal factors identification for unstable history`() {
        val commitDiff = ""
        val testHistory = List(10) { it % 2 == 0 } // 50% failure rate
        val logs = ""

        val factors = model.identifyCausalFactors(commitDiff, testHistory, logs)

        assertTrue(factors.any { it.contains("Unstable build history") })
    }

    @Test
    fun `test causal factors identification for memory issues`() {
        val commitDiff = ""
        val testHistory = emptyList<Boolean>()
        val logs = "OutOfMemoryError: Java heap space"

        val factors = model.identifyCausalFactors(commitDiff, testHistory, logs)

        assertTrue(factors.any { it.contains("Memory issues") })
    }
}
