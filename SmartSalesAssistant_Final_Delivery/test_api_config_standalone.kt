// Standalone API config test to verify the logic without main app dependencies
import com.smartsales.data.network.ConnectivityApiConfig

fun main() {
    println("=== API Config Test Results ===")
    
    // Test 1: Default port
    val defaultUrl = ConnectivityApiConfig.buildBaseUrl("192.168.1.100")
    println("Test 1 - Default port: $defaultUrl")
    assert(defaultUrl == "http://192.168.1.100:8000/") { "Default port test failed" }
    
    // Test 2: Custom port
    val customUrl = ConnectivityApiConfig.buildBaseUrl("192.168.1.100", 9000)
    println("Test 2 - Custom port: $customUrl")
    assert(customUrl == "http://192.168.1.100:9000/") { "Custom port test failed" }
    
    // Test 3: Media server port (from your integration)
    val mediaUrl = ConnectivityApiConfig.buildBaseUrl("192.168.1.100", 34123)
    println("Test 3 - Media server port: $mediaUrl")
    assert(mediaUrl == "http://192.168.1.100:34123/") { "Media server port test failed" }
    
    println("\n✅ All API Config tests passed!")
}