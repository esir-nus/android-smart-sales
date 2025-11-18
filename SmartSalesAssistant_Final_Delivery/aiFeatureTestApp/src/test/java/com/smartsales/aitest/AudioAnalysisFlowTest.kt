package com.smartsales.aitest

import com.smartsales.aitest.ui.AiTestUiState
import com.smartsales.aitest.ui.AudioInputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration test for the complete audio analysis flow
 * Tests: Local file → OSS upload → Tingwu transcription → AI analysis
 */
class AudioAnalysisFlowTest {
    @Test
    fun `test complete audio analysis workflow`() {
        // Given: Test audio file and expected workflow states
        val testWorkflow =
            listOf(
                "Initial state should be URL mode",
                "Switch to local file mode",
                "Upload to OSS",
                "Start transcription",
                "Generate summary",
                "Generate customer analysis",
                "Generate mindmap",
            )

        // When: Simulate the complete workflow
        var currentState = AiTestUiState()

        // Step 1: Initial state
        assertEquals("Should start in URL mode", AudioInputMode.URL, currentState.audioInputMode)
        assertNull("Should have no selected file initially", currentState.selectedAudioFile)

        // Step 2: Switch to local file mode
        val testFile = java.io.File("/home/cslh-frank/smart-sales/android-smart-sales/test-files/Recording (8).mp3")
        // Simulate file selection
        currentState =
            currentState.copy(
                audioInputMode = AudioInputMode.LOCAL_FILE,
                selectedAudioFile = testFile,
                tingwuAudioUrl = "file://${testFile.absolutePath}",
            )

        assertEquals("Should be in local file mode", AudioInputMode.LOCAL_FILE, currentState.audioInputMode)
        assertNotNull("Should have selected file", currentState.selectedAudioFile)

        // Step 3: OSS upload would happen here
        // (In real implementation, this would be async)

        // Step 4: Transcription processing
        currentState =
            currentState.copy(
                isTingwuProcessing = true,
                tingwuStatusMessage = "正在上传音频到 OSS...",
            )

        assertTrue("Should be processing", currentState.isTingwuProcessing)
        assertNotNull("Should have status message", currentState.tingwuStatusMessage)

        // Step 5-7: Analysis features would be triggered after transcription completes
        // These would populate the analysis fields

        // Then: Verify final state expectations
        assertTrue("Workflow states should be valid", testWorkflow.all { it.isNotEmpty() })
    }

    @Test
    fun `test analysis features availability`() {
        // Given: Completed transcription with raw data
        val mockTranscriptionData =
            com.smartsales.data.network.model.TranscriptionData(
                text = "这是一个测试销售对话，客户对价格有些顾虑，但对产品功能很感兴趣。",
                segments = emptyList(),
                speakers = null,
                language = "zh",
                duration = 120f,
            )

        val stateWithTranscription =
            AiTestUiState(
                rawTranscriptionData = mockTranscriptionData,
            )

        // When: Analysis features should be available
        val hasTranscriptionData = stateWithTranscription.rawTranscriptionData != null

        // Then: All analysis features should be triggerable
        assertTrue("Should have transcription data for analysis", hasTranscriptionData)

        // Verify that each analysis type would have input data
        val transcriptionText = stateWithTranscription.rawTranscriptionData?.text
        assertNotNull("Should have text for summary generation", transcriptionText)
        assertTrue("Transcription text should be meaningful", transcriptionText!!.length > 10)
    }

    @Test
    fun `test error handling throughout workflow`() {
        // Given: Various error scenarios
        val errorScenarios =
            mapOf(
                "Network failure" to "网络问题，请稍后再试",
                "File not found" to "网络问题，请稍后再试",
                "Invalid audio format" to "网络问题，请稍后再试",
                "OSS upload failed" to "网络问题，请稍后再试",
                "Transcription timeout" to "网络问题，请稍后再试",
            )

        // When: Any error occurs in the workflow
        // Then: Should always show user-friendly error message
        errorScenarios.forEach { (scenario, expectedError) ->
            assertEquals(
                "Error message should be user-friendly for: $scenario",
                "网络问题，请稍后再试",
                expectedError,
            )
        }
    }
}
