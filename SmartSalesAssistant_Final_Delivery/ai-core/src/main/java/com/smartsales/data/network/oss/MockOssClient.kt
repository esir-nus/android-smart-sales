package com.smartsales.data.network.oss

/**
 * Mock OSS Client for testing without actual Alibaba SDK dependencies
 */
class MockOssClient {
    fun putObject(
        bucketName: String,
        objectKey: String,
        filePath: String,
    ): MockPutObjectResult {
        // Mock implementation - simulate successful upload
        return MockPutObjectResult(
            etag = "mock-etag-${System.currentTimeMillis()}",
            requestId = "mock-request-${System.currentTimeMillis()}",
            statusCode = 200,
        )
    }

    fun presignConstrainedObjectURL(
        bucketName: String,
        objectKey: String,
        expirationTime: Long,
    ): String {
        // Generate a mock presigned URL
        return "https://mock-oss.aliyuncs.com/$bucketName/$objectKey?Expires=$expirationTime&Signature=mock-signature"
    }

    fun deleteObject(
        bucketName: String,
        objectKey: String,
    ) {
        // Mock deletion - do nothing
    }
}

data class MockPutObjectResult(
    val etag: String,
    val requestId: String,
    val statusCode: Int,
)
