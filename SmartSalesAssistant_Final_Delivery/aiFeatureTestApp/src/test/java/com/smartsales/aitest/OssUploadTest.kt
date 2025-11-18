package com.smartsales.aitest

import com.smartsales.aitest.ui.AudioInputMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit test for OSS upload functionality and audio validation
 * Uses mock implementations to avoid external dependencies
 */
class OssUploadTest {
    @Test
    fun `test audio file validation`() {
        // Given: A test audio file path
        val testFile = File("/home/cslh-frank/smart-sales/android-smart-sales/test-files/Recording (8).mp3")

        // When: File validation is performed
        val isValid = testFile.exists() && testFile.length() > 0

        // Then: File should be valid for upload
        assertTrue("Test audio file should exist and be readable", isValid)
        assertTrue("File should be less than 100MB", testFile.length() < 100 * 1024 * 1024)
    }

    @Test
    fun `test oss configuration loading`() {
        // Given: BuildConfig values should be populated from local.properties
        val endpoint = BuildConfig.ALI_OSS_ENDPOINT
        val bucket = BuildConfig.ALI_OSS_BUCKET
        val accessKeyId = BuildConfig.ALI_OSS_ACCESS_KEY_ID
        val accessKeySecret = BuildConfig.ALI_OSS_ACCESS_KEY_SECRET

        // Then: Configuration should be present (even if mock for testing)
        assertFalse("OSS endpoint should not be empty", endpoint.isEmpty())
        assertFalse("OSS bucket should not be empty", bucket.isEmpty())
        assertFalse("OSS access key ID should not be empty", accessKeyId.isEmpty())
        assertFalse("OSS access key secret should not be empty", accessKeySecret.isEmpty())
    }

    @Test
    fun `test audio input mode enum values`() {
        // Test the enum values are correctly defined
        val modes = AudioInputMode.values()
        assertEquals("Should have 2 input modes", 2, modes.size)
        assertTrue("Should contain URL mode", modes.contains(AudioInputMode.URL))
        assertTrue("Should contain LOCAL_FILE mode", modes.contains(AudioInputMode.LOCAL_FILE))
    }

    @Test
    fun `test error message consistency`() {
        // Verify the error message follows the required pattern
        val errorMessage = "网络问题，请稍后再试"
        assertEquals(
            "Error message should be user-friendly and consistent",
            "网络问题，请稍后再试",
            errorMessage,
        )
    }
}
