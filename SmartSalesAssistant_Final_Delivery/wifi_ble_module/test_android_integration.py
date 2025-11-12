#!/usr/bin/env python3
"""
Android Integration Test Script
Tests the media server endpoints that the Android app will use
"""

import requests
import json
import time

def test_server_integration():
    """Test all endpoints that the Android app will use"""
    
    # Server configuration
    BASE_URL = "http://198.18.0.1:34123"
    print(f"🧪 Testing Android Integration with {BASE_URL}")
    print("=" * 60)
    
    tests_passed = 0
    tests_total = 0
    
    def test_endpoint(name, method, url, expected_status=200, data=None):
        nonlocal tests_passed, tests_total
        tests_total += 1
        print(f"\n📋 Test {tests_total}: {name}")
        print(f"   {method} {url}")
        
        try:
            if method == "GET":
                response = requests.get(url, timeout=5)
            elif method == "POST":
                response = requests.post(url, data=data, timeout=5)
            elif method == "DELETE":
                response = requests.delete(url, timeout=5)
            
            print(f"   Status: {response.status_code}")
            
            if response.status_code == expected_status:
                print(f"   ✅ PASSED")
                tests_passed += 1
                
                # Pretty print JSON responses
                try:
                    json_data = response.json()
                    print(f"   Response preview: {json.dumps(json_data, indent=2)[:200]}...")
                except:
                    print(f"   Response: {response.text[:100]}...")
                    
            else:
                print(f"   ❌ FAILED - Expected {expected_status}, got {response.status_code}")
                print(f"   Response: {response.text[:200]}")
                
        except requests.exceptions.RequestException as e:
            print(f"   ❌ FAILED - Request error: {e}")
        except Exception as e:
            print(f"   ❌ FAILED - Unexpected error: {e}")
    
    # Test 1: Server info endpoint (root)
    test_endpoint(
        "Server Info (Root)",
        "GET",
        f"{BASE_URL}/"
    )
    
    # Test 2: File list API
    test_endpoint(
        "File List API",
        "GET",
        f"{BASE_URL}/api/files"
    )
    
    # Test 3: Server status
    test_endpoint(
        "Server Status",
        "GET",
        f"{BASE_URL}/status"
    )
    
    # Test 4: File download
    test_endpoint(
        "File Download - sample1.jpg",
        "GET",
        f"{BASE_URL}/download/sample1.jpg"
    )
    
    # Test 5: Direct file access
    test_endpoint(
        "Direct File Access - sample1.jpg",
        "GET",
        f"{BASE_URL}/sample1.jpg"
    )
    
    # Test 6: File apply
    test_endpoint(
        "File Apply - sample1.jpg",
        "POST",
        f"{BASE_URL}/apply/sample1.jpg"
    )
    
    # Test 7: File delete (we'll recreate it later)
    test_endpoint(
        "File Delete - sample2.png",
        "DELETE",
        f"{BASE_URL}/delete/sample2.png"
    )
    
    # Test 8: Verify file was deleted
    test_endpoint(
        "Verify File Deleted - sample2.png",
        "GET",
        f"{BASE_URL}/download/sample2.png",
        expected_status=404
    )
    
    # Test 9: CORS headers (check if present)
    print(f"\n📋 Test {tests_total + 1}: CORS Headers Check")
    try:
        response = requests.get(f"{BASE_URL}/", timeout=5)
        cors_header = response.headers.get('Access-Control-Allow-Origin')
        if cors_header == '*':
            print(f"   ✅ PASSED - CORS header present: {cors_header}")
            tests_passed += 1
        else:
            print(f"   ❌ FAILED - CORS header missing or incorrect: {cors_header}")
        tests_total += 1
    except Exception as e:
        print(f"   ❌ FAILED - Error checking CORS: {e}")
        tests_total += 1
    
    # Summary
    print("\n" + "=" * 60)
    print(f"📊 Test Summary:")
    print(f"   Tests Passed: {tests_passed}/{tests_total}")
    print(f"   Success Rate: {(tests_passed/tests_total)*100:.1f}%")
    
    if tests_passed == tests_total:
        print("   🎉 All tests passed! Android integration should work.")
    else:
        print("   ⚠️  Some tests failed. Check server logs and configuration.")
    
    return tests_passed == tests_total

def test_android_specific_scenarios():
    """Test scenarios specific to Android integration"""
    
    BASE_URL = "http://198.18.0.1:34123"
    print(f"\n🤖 Testing Android-Specific Scenarios")
    print("=" * 60)
    
    # Test WebView compatibility
    print("\n📋 Testing WebView compatibility...")
    
    # Test if server responds correctly to Android WebView user agent
    android_ua = "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
    
    try:
        response = requests.get(f"{BASE_URL}/", headers={'User-Agent': android_ua}, timeout=5)
        if response.status_code == 200:
            print("   ✅ Server responds to Android User-Agent")
        else:
            print(f"   ❌ Server response to Android UA: {response.status_code}")
    except Exception as e:
        print(f"   ❌ Error testing Android UA: {e}")
    
    # Test JSON parsing for Android
    print("\n📋 Testing JSON response format for Android...")
    try:
        response = requests.get(f"{BASE_URL}/api/files", timeout=5)
        data = response.json()
        
        required_fields = ['files', 'total_count', 'server']
        missing_fields = [field for field in required_fields if field not in data]
        
        if not missing_fields:
            print("   ✅ All required JSON fields present")
            print(f"   📊 File count: {data['total_count']}")
            print(f"   🖼️  Image files: {len([f for f in data['files'] if f['type'] == 'image'])}")
            print(f"   🎬 Video files: {len([f for f in data['files'] if f['type'] == 'video'])}")
        else:
            print(f"   ❌ Missing JSON fields: {missing_fields}")
            
        # Test individual file structure
        if data['files']:
            sample_file = data['files'][0]
            file_fields = ['name', 'size', 'type', 'url', 'download_url']
            missing_file_fields = [field for field in file_fields if field not in sample_file]
            
            if not missing_file_fields:
                print("   ✅ File object structure correct")
            else:
                print(f"   ❌ Missing file fields: {missing_file_fields}")
                
    except Exception as e:
        print(f"   ❌ Error testing JSON format: {e}")
    
    # Test file operations that Android will use
    print("\n📋 Testing file operations for Android...")
    
    # Test download with proper headers
    try:
        response = requests.get(f"{BASE_URL}/download/sample1.jpg", timeout=10)
        if response.status_code == 200:
            content_type = response.headers.get('Content-Type', '')
            content_disposition = response.headers.get('Content-Disposition', '')
            print(f"   ✅ Download working - Content-Type: {content_type}")
            print(f"   📎 Content-Disposition: {content_disposition[:50]}...")
        else:
            print(f"   ❌ Download failed: {response.status_code}")
    except Exception as e:
        print(f"   ❌ Download error: {e}")

def simulate_android_webview_flow():
    """Simulate the complete Android WebView flow"""
    
    BASE_URL = "http://198.18.0.1:34123"
    print(f"\n📱 Simulating Complete Android WebView Flow")
    print("=" * 60)
    
    steps = [
        ("1. Initial connection", f"{BASE_URL}/"),
        ("2. Load file list", f"{BASE_URL}/api/files"),
        ("3. View sample image", f"{BASE_URL}/sample1.jpg"),
        ("4. Download sample image", f"{BASE_URL}/download/sample1.jpg"),
    ]
    
    for step_name, url in steps:
        print(f"\n📋 {step_name}")
        try:
            response = requests.get(url, timeout=10)
            if response.status_code == 200:
                print(f"   ✅ Success - Status: {response.status_code}")
                print(f"   📊 Response size: {len(response.content)} bytes")
                
                # Check content type
                content_type = response.headers.get('Content-Type', 'unknown')
                print(f"   📝 Content-Type: {content_type}")
                
            else:
                print(f"   ❌ Failed - Status: {response.status_code}")
                
        except Exception as e:
            print(f"   ❌ Error: {e}")
    
    print(f"\n✅ Android WebView flow simulation complete!")

if __name__ == '__main__':
    print("🚀 Android Integration Test Suite")
    print("=" * 60)
    print(f"Testing integration with local media server...")
    
    # Wait a moment for server to be ready
    time.sleep(1)
    
    # Run all test suites
    basic_tests_passed = test_server_integration()
    test_android_specific_scenarios()
    simulate_android_webview_flow()
    
    print("\n" + "=" * 60)
    print("🎯 Testing Complete!")
    print("=" * 60)
    
    if basic_tests_passed:
        print("\n🎉 SUCCESS: Basic integration tests passed!")
        print("📱 The Android app should be able to connect to the media server.")
        print("\n🚀 Next steps:")
        print("   1. Open the Android app")
        print("   2. Enter IP: 198.18.0.1, Port: 34123")
        print("   3. Click '媒体管理' button")
        print("   4. Test file operations in WebView")
    else:
        print("\n⚠️  Some tests failed. Check the server logs above.")
        print("🔧 Make sure the server is running on port 34123")
    
    print(f"\n📋 Server is running at: http://198.18.0.1:34123")
    print("🌐 You can also test the web UI at: http://198.18.0.1:34123/test-ui")