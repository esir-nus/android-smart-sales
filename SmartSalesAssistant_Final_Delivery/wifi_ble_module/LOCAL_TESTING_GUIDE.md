# Local Testing Guide - Media Server Integration

## 🚀 Quick Start

The local media server is now running and ready for Android app testing!

### 📋 Server Details
- **Port**: 34123
- **Local URL**: http://localhost:34123
- **Network URL**: http://198.18.0.1:34123
- **Status**: ✅ Running

### 🎯 For Android Testing
Use this IP address in your Android app: `198.18.0.1:34123`

## 📱 Android App Configuration

1. **Open the Android app** (wifiBleTestApp)
2. **Connect via BLE** to your simulated device (or skip if just testing HTTP)
3. **Enter the server IP**: `198.18.0.1`
4. **Enter the port**: `34123`
5. **Click "媒体管理" (Media Management)** button

## 🔧 Available Endpoints

### JSON API Endpoints
```
GET  http://198.18.0.1:34123/           → Server info (JSON)
GET  http://198.18.0.1:34123/api/files  → File list (JSON)
GET  http://198.18.0.1:34123/status     → Server status
```

### File Operations
```
GET  http://198.18.0.1:34123/download/<filename> → Download file
GET  http://198.18.0.1:34123/<filename>          → View file
POST http://198.18.0.1:34123/upload              → Upload file
DELETE http://198.18.0.1:34123/delete/<filename> → Delete file
POST http://198.18.0.1:34123/apply/<filename>    → Apply file
```

## 🧪 Testing the Integration

### Step 1: Verify Server is Running
```bash
curl http://198.18.0.1:34123/
```
Expected response:
```json
{
  "message": "Media server is running. Use /api/files for file listing.",
  "android_integration": true,
  "server": "media_server",
  "timestamp": 1234567890.123,
  "endpoints": { ... }
}
```

### Step 2: Check File List
```bash
curl http://198.18.0.1:34123/api/files
```
Should return 6 test files.

### Step 3: Test File Download
```bash
curl -O http://198.18.0.1:34123/download/sample1.jpg
```

### Step 4: Test Web UI
Open in browser: http://198.18.0.1:34123/test-ui

## 📁 Test Files Created
The server automatically created 6 test files:
- `sample1.jpg` (1KB test image)
- `sample2.png` (1KB test image)
- `demo_image.jpg` (2KB test image)
- `video1.mp4` (1KB test video)
- `test_video.mp4` (2KB test video)
- `audio1.mp3` (1KB test audio)

## 🌐 Web UI for Testing
Open this URL in your browser to test the web interface:
```
http://198.18.0.1:34123/test-ui
```

This provides a visual interface to test:
- File listing
- Upload functionality
- Download functionality
- Delete functionality
- Apply functionality

## 🔍 Debugging

### Check Server Logs
```bash
cd android-smart-sales/SmartSalesAssistant_Final_Delivery/wifi_ble_module
tail -f server2.log
```

### Test with curl
```bash
# Test server status
curl http://198.18.0.1:34123/status

# Test file list
curl http://198.18.0.1:34123/api/files

# Test file download
curl -I http://198.18.0.1:34123/download/sample1.jpg
```

### Check if server is responsive
```bash
ps aux | grep "simple_test_server.py"
```

## 🛑 Stopping the Server
```bash
pkill -f "python3 simple_test_server.py"
```

## 📱 Android App Testing Checklist

- [ ] Server responds to HTTP requests
- [ ] Android app can connect to server IP
- [ ] "媒体管理" button opens WebView
- [ ] File list displays in WebView
- [ ] File upload works from Android
- [ ] File download works
- [ ] File delete works
- [ ] File apply functionality works
- [ ] Server detection works correctly
- [ ] UI shows appropriate titles for media server

## 🚨 Common Issues

### Server not responding
- Check if port 34123 is available: `lsof -i :34123`
- Restart server: `pkill -f "simple_test_server.py"` then start again
- Check server logs: `tail server2.log`

### Android app can't connect
- Make sure device and computer are on same network
- Use the network IP (198.18.0.1) not localhost
- Check firewall settings
- Verify the port is correct (34123)

### WebView not loading
- Check CORS headers are working
- Test in browser first
- Check Android WebView settings
- Verify URL format: `http://198.18.0.1:34123`

## ✅ Success Criteria
- Server responds with JSON at root endpoint
- File list API returns 6 test files
- Web UI loads in browser
- Android app can connect and display media management interface
- All file operations work through WebView

The server is ready for Android app testing! 🎉