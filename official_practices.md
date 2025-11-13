# Alibaba NUI SDK Official Practices Guide

This document contains important code snippets and schemas from the official Alibaba NUI SDK v2.7.0 sample project that can serve as positive samples for guiding development.

## Project Overview

**SDK Version**: V2.7.0-039-20251010_Android_OpenSSL  
**Source**: `/home/cslh-frank/smart-sales/V2.7.0-039-20251010_Android_OpenSSL/example/V2.7.0-039-20251010_Android/`  
**Package**: `mit.alibaba.nuidemo`

## 1. Authentication & Token Management

### Access Token Structure
```java
// mit.alibaba.nuidemo.token.AccessToken
public class AccessToken {
    private String accessToken;
    private long expireTime;
    private String tokenType;
    private String errMsg;
    
    public boolean isValid() {
        return !TextUtils.isEmpty(accessToken) && 
               System.currentTimeMillis() < expireTime && 
               TextUtils.isEmpty(errMsg);
    }
}
```

### Token Request Implementation
```java
// mit.alibaba.nuidemo.token.HttpUtil
public static String sendPost(String url, String body, String ak_id, String ak_secret) {
    HttpRequest request = new HttpRequest(url);
    request.setMethod(HttpRequest.METHOD_POST);
    request.setContentType("application/json");
    request.setBody(body.getBytes());
    
    // Add authentication headers
    Map<String, String> headers = Signer.sign(ak_id, ak_secret);
    for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.addHeader(entry.getKey(), entry.getValue());
    }
    
    HttpResponse response = HttpRequestUtil.sendRequest(request);
    return response.getResult();
}
```

### Authentication Configuration
```java
// mit.alibaba.nuidemo.Auth
public class Auth {
    public static final String AK_ID = "YOUR_ACCESS_KEY_ID";
    public static final String AK_SECRET = "YOUR_ACCESS_KEY_SECRET";
    public static final String APP_KEY = "YOUR_APP_KEY";
    public static final String TOKEN_URL = "https://nls-gateway.cn-shanghai.aliyuncs.com/stream/v1/tts";
    
    public static String getToken() {
        try {
            String body = "{\"appkey\":\"" + APP_KEY + "\",\"token_expire_time\":3600}";
            return HttpUtil.sendPost(TOKEN_URL, body, AK_ID, AK_SECRET);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
```

## 2. SDK Initialization Pattern

### Application-Level Initialization
```java
// mit.alibaba.nuidemo.MainApplication
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize NUI SDK
        String workspace = getApplicationContext().getFilesDir().getAbsolutePath() + "/";
        String debugPath = getApplicationContext().getExternalCacheDir().getAbsolutePath() + "/";
        
        // Copy resource files
        copyAssetsFile(getApplicationContext(), "nui.json", workspace);
        copyAssetsFile(getApplicationContext(), "vad.bin", workspace);
        copyAssetsFile(getApplicationContext(), "kws.bin", workspace);
        copyAssetsFile(getApplicationContext(), "cei.json", workspace);
    }
    
    private void copyAssetsFile(Context context, String assetsFileName, String workspace) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open(assetsFileName);
            File outputFile = new File(workspace, assetsFileName);
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 3. Speech Recognition Implementation

### Basic Speech Recognizer Setup
```java
// mit.alibaba.nuidemo.SpeechRecognizerActivity
public class SpeechRecognizerActivity extends AppCompatActivity {
    private SpeechRecognizer speechRecognizer;
    private StringBuilder resultText = new StringBuilder();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize recognizer
        String workspace = getFilesDir().getAbsolutePath() + "/";
        String debugPath = getExternalCacheDir().getAbsolutePath() + "/";
        
        speechRecognizer = new SpeechRecognizer(this, workspace, debugPath);
        
        // Configure parameters
        String params = "{\"app_key\":\"" + Auth.APP_KEY + "\"," +
                       "\"format\":\"pcm\"," +
                       "\"sample_rate\":16000," +
                       "\"enable_intermediate_result\":true," +
                       "\"enable_punctuation_prediction\":true," +
                       "\"enable_inverse_text_normalization\":true}";
        
        int ret = speechRecognizer.init(params);
        if (ret != 0) {
            showError("Speech recognizer initialization failed: " + ret);
            return;
        }
        
        // Set token
        String token = Auth.getToken();
        speechRecognizer.setToken(token);
        
        // Set listener
        speechRecognizer.setListener(new SpeechRecognizerListener() {
            @Override
            public void onRecognizedResultChanged(String result, boolean isLast) {
                if (!isLast) {
                    updateIntermediateResult(result);
                } else {
                    updateFinalResult(result);
                }
            }
            
            @Override
            public void onRecognizedCompleted(String result, int code) {
                if (code == 0) {
                    updateFinalResult(result);
                } else {
                    showError("Recognition failed: " + code);
                }
            }
            
            @Override
            public void onTaskFailed(int code, String errorMessage) {
                showError("Task failed: " + code + " - " + errorMessage);
            }
        });
    }
}
```

### Real-time Speech Recognition Pattern
```java
// mit.alibaba.nuidemo.DashParaformerSpeechTranscriberActivity
private void startTranscriber() {
    // Start recording
    recorder.start(new MainRecorderCallback() {
        @Override
        public void onFrame(byte[] bytes, int i) {
            // Send audio data to recognizer
            if (speechTranscriber != null) {
                speechTranscriber.sendAudio(bytes, bytes.length);
            }
        }
        
        @Override
        public void onStop() {
            // Signal end of speech
            if (speechTranscriber != null) {
                speechTranscriber.stop();
            }
        }
    });
}
```

## 4. Text-to-Speech Implementation

### Basic TTS Setup
```java
// mit.alibaba.nuidemo.TtsBasicActivity
public class TtsBasicActivity extends AppCompatActivity {
    private SpeechSynthesizer speechSynthesizer;
    private AudioPlayer audioPlayer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize TTS
        String workspace = getFilesDir().getAbsolutePath() + "/";
        String debugPath = getExternalCacheDir().getAbsolutePath() + "/";
        
        speechSynthesizer = new SpeechSynthesizer(this, workspace, debugPath);
        audioPlayer = new AudioPlayer();
        
        // Configure TTS parameters
        String params = "{\"app_key\":\"" + Auth.APP_KEY + "\"," +
                       "\"format\":\"wav\"," +
                       "\"sample_rate\":16000," +
                       "\"volume\":50," +
                       "\"speech_rate\":0," +
                       "\"pitch_rate\":0}";
        
        int ret = speechSynthesizer.init(params);
        if (ret != 0) {
            showError("TTS initialization failed: " + ret);
            return;
        }
        
        // Set token
        String token = Auth.getToken();
        speechSynthesizer.setToken(token);
        
        // Set TTS listener
        speechSynthesizer.setListener(new SpeechSynthesizerListener() {
            @Override
            public void onSynthesizerDataReceived(byte[] data, int code) {
                if (code == 0) {
                    // Play audio data
                    audioPlayer.play(data);
                } else {
                    showError("TTS synthesis failed: " + code);
                }
            }
            
            @Override
            public void onSynthesizerCompleted(int code, String message) {
                if (code == 0) {
                    showSuccess("TTS synthesis completed");
                } else {
                    showError("TTS synthesis failed: " + code + " - " + message);
                }
            }
        });
    }
    
    private void synthesizeText(String text) {
        String params = "{\"text\":\"" + text + "\"," +
                       "\"voice\":\"xiaoyun\"," +
                       "\"volume\":50," +
                       "\"speech_rate\":0," +
                       "\"pitch_rate\":0}";
        
        int ret = speechSynthesizer.startSynthesizer(params);
        if (ret != 0) {
            showError("Failed to start synthesis: " + ret);
        }
    }
}
```

### Streaming TTS Pattern
```java
// mit.alibaba.nuidemo.StreamInputTtsBasicActivity
private void startStreamTts() {
    String params = "{\"voice\":\"xiaoyun\"," +
                   "\"volume\":50," +
                   "\"speech_rate\":0," +
                   "\"pitch_rate\":0," +
                   "\"enable_subtitle\":true}";
    
    int ret = speechSynthesizer.startSynthesizer(params);
    if (ret == 0) {
        // Send text in chunks
        String[] textChunks = getTextChunks(inputText);
        for (String chunk : textChunks) {
            speechSynthesizer.sendText(chunk);
        }
        speechSynthesizer.stopSynthesizer();
    }
}
```

## 5. Wake Word Detection

### Wake Word Implementation
```java
// mit.alibaba.nuidemo.OnlyWakeupActivity
public class OnlyWakeupActivity extends AppCompatActivity {
    private SpeechSynthesizer wakeupDetector;
    private MainRecorder recorder;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize wake word detector
        String workspace = getFilesDir().getAbsolutePath() + "/";
        String debugPath = getExternalCacheDir().getAbsolutePath() + "/";
        
        wakeupDetector = new SpeechSynthesizer(this, workspace, debugPath);
        
        // Configure wake word parameters
        String params = "{\"app_key\":\"" + Auth.APP_KEY + "\"," +
                       "\"format\":\"pcm\"," +
                       "\"sample_rate\":16000," +
                       "\"enable_wakenet\":true," +
                       "\"wakenet_model_path\":\"" + workspace + "kws.bin\"}";
        
        int ret = wakeupDetector.init(params);
        if (ret != 0) {
            showError("Wake word detector initialization failed: " + ret);
            return;
        }
        
        // Set wake word listener
        wakeupDetector.setListener(new SpeechSynthesizerListener() {
            @Override
            public void onSynthesizerDataReceived(byte[] data, int code) {
                // Handle wake word detection
                if (code == 0) {
                    String wakeWord = parseWakeWord(data);
                    if (wakeWord != null) {
                        onWakeWordDetected(wakeWord);
                    }
                }
            }
            
            @Override
            public void onSynthesizerCompleted(int code, String message) {
                // Handle completion
            }
        });
    }
    
    private void startWakeWordDetection() {
        // Start continuous recording
        recorder.start(new MainRecorderCallback() {
            @Override
            public void onFrame(byte[] bytes, int i) {
                // Send audio data to wake word detector
                if (wakeupDetector != null) {
                    wakeupDetector.sendAudio(bytes, bytes.length);
                }
            }
        });
    }
}
```

## 6. Audio Recording Pattern

### Main Recorder Implementation
```java
// mit.alibaba.nuidemo.MainRecorder
public class MainRecorder {
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    
    public void start(IMainRecorderCallback callback) {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        
        audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        );
        
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            callback.onError("AudioRecord initialization failed");
            return;
        }
        
        isRecording = true;
        audioRecord.startRecording();
        
        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int readResult = audioRecord.read(buffer, 0, bufferSize);
                if (readResult > 0) {
                    callback.onFrame(buffer, readResult);
                }
            }
        });
        
        recordingThread.start();
    }
    
    public void stop() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null) {
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## 7. Audio Player Implementation

### Basic Audio Player
```java
// mit.alibaba.nuidemo.AudioPlayer
public class AudioPlayer {
    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    
    public AudioPlayer() {
        int bufferSize = AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        );
        
        audioTrack = new AudioTrack(
            AudioManager.STREAM_MUSIC,
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        );
    }
    
    public void play(byte[] audioData) {
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            if (!isPlaying) {
                audioTrack.play();
                isPlaying = true;
            }
            audioTrack.write(audioData, 0, audioData.length);
        }
    }
    
    public void stop() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            isPlaying = false;
        }
    }
}
```

## 8. Error Handling Pattern

### Consistent Error Handling
```java
// Common pattern across all activities
private void showError(String message) {
    runOnUiThread(() -> {
        new AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    });
}

private void showSuccess(String message) {
    runOnUiThread(() -> {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    });
}
```

## 9. Resource Management

### Asset Copying Pattern
```java
private void copyAssetsFile(Context context, String assetsFileName, String workspace) {
    AssetManager assetManager = context.getAssets();
    try {
        InputStream inputStream = assetManager.open(assetsFileName);
        File outputFile = new File(workspace, assetsFileName);
        
        if (outputFile.exists()) {
            return; // Already copied
        }
        
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        inputStream.close();
        outputStream.close();
    } catch (IOException e) {
        Log.e(TAG, "Failed to copy asset: " + assetsFileName, e);
    }
}
```

## 10. Manifest Configuration

### Required Permissions
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<application
    android:name=".MainApplication"
    android:largeHeap="true"
    android:hardwareAccelerated="true">
    
    <!-- Activities -->
    <activity android:name=".MainActivity"
        android:exported="true"
        android:screenOrientation="portrait">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

## 11. Gradle Configuration

### Dependencies
```gradle
// build.gradle (Module: app)
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar', '*.aar'])
    implementation 'com.alibaba:fastjson:1.1.46.android'
    implementation 'com.android.support:appcompat-v7:28.0.0'
    implementation 'com.android.support:recyclerview-v7:28.0.0'
    
    // Local SDK
    implementation files('libs/nuisdk-release.aar')
}
```

## 12. Key Best Practices

### 1. Token Management
- Always validate token before use
- Implement token refresh mechanism
- Cache tokens with expiration time

### 2. Resource Management
- Copy required resource files on first launch
- Check file existence before copying
- Handle storage permissions properly

### 3. Audio Configuration
- Use consistent sample rate (16kHz)
- Configure proper audio formats (PCM 16-bit)
- Handle audio focus changes

### 4. Error Handling
- Implement comprehensive error callbacks
- Provide user-friendly error messages
- Log detailed error information

### 5. Threading
- Run audio operations on background threads
- Update UI on main thread
- Handle thread lifecycle properly

### 6. Permissions
- Request runtime permissions for Android 6.0+
- Handle permission denials gracefully
- Provide rationale for permission requests

## 13. Advanced Authentication Patterns

### Token Refresh Strategy
```java
// Automatic token refresh before expiration
public static JSONObject refreshTokenIfNeed(JSONObject json, long distance_expire_time) {
    if (!cur_appkey.isEmpty() && !cur_token.isEmpty() && cur_token_expired_time > 0) {
        long millis = System.currentTimeMillis();
        long unixTimestampInSeconds = millis / 1000;
        
        // Refresh token if it will expire within the specified time window
        if (cur_token_expired_time - distance_expire_time < unixTimestampInSeconds) {
            String old_token = cur_token;
            long old_expire_time = cur_token_expired_time;
            json = getTicket(cur_method);
            String new_token = cur_token;
            long new_expire_time = cur_token_expired_time;
            Log.i("Auth", "Refresh old token(" + old_token + " : " + old_expire_time +
                    ") to (" + new_token + " : " + new_expire_time + ").");
        }
    }
    return json;
}
```

### Configuration from JSON File
```java
// Load authentication from secure JSON file
public static JSONObject getTicketFromJsonFile(String fileName) {
    try {
        File jsonFile = new File(fileName);
        FileReader fileReader = new FileReader(jsonFile);
        Reader reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
        
        StringBuffer sb = new StringBuffer();
        int ch;
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        
        fileReader.close();
        reader.close();
        String jsonStr = sb.toString();
        return JSON.parseObject(jsonStr);
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}
```

## 14. SDK Integration Architecture

### Native SDK Interface Pattern
```java
// NativeNui usage pattern from SpeechRecognizerActivity
NativeNui nui_instance = new NativeNui();

// Get SDK version for debugging
String version = nui_instance.GetVersion();
Log.i(TAG, "current sdk version: " + version);

// Initialize with workspace and debug paths
String workspace = getFilesDir().getAbsolutePath() + "/";
String debugPath = getExternalCacheDir().getAbsolutePath() + "/";

// Set callback for native events
nui_instance.setCallback(this); // INativeNuiCallback implementation
```

### Audio Processing Constants
```java
// Standard audio parameters used across all features
private final static int SAMPLE_RATE = 16000; // 16kHz sample rate
private final static int WAVE_FRAM_SIZE = 20 * 2 * 1 * SAMPLE_RATE / 1000; 
// 20ms audio frame for 16k/16bit/mono = 640 bytes
```

## 15. Threading and Concurrency

### Handler Thread Pattern
```java
// Background processing with HandlerThread
private Handler mHandler;
private HandlerThread mHanderThread;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Create dedicated thread for audio processing
    mHanderThread = new HandlerThread("process_thread");
    mHanderThread.start();
    mHandler = new Handler(mHanderThread.getLooper());
}

@Override
protected void onDestroy() {
    super.onDestroy();
    // Clean up background thread
    if (mHanderThread != null) {
        mHanderThread.quitSafely();
    }
}
```

### Concurrent Audio Queue
```java
// Thread-safe audio data queue
private LinkedBlockingQueue<byte[]> tmpAudioQueue = new LinkedBlockingQueue();

// Producer: Audio recording thread
public void onRecorderData(byte[] data, int size) {
    try {
        tmpAudioQueue.put(data); // Non-blocking with timeout
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}

// Consumer: Processing thread
private void processAudioQueue() {
    while (!mStopping) {
        try {
            byte[] audioData = tmpAudioQueue.take(); // Blocks until data available
            // Process audio data
            int ret = nui_instance.writeAudio(audioData, audioData.length);
        } catch (InterruptedException e) {
            break;
        }
    }
}
```

## 16. Resource File Management

### Critical Resource Files
```java
// Required resource files that must be copied from assets
private void copyRequiredResources() {
    String workspace = getFilesDir().getAbsolutePath() + "/";
    
    // Essential configuration files
    copyAssetsFile("nui.json");     // Main SDK configuration
    copyAssetsFile("vad.bin");      // Voice Activity Detection model
    copyAssetsFile("kws.bin");      // Keyword Spotting (wake word) model  
    copyAssetsFile("cei.json");     // Custom entity information
}

private boolean copyAssetsFile(String fileName) {
    AssetManager assetManager = getAssets();
    try {
        InputStream inputStream = assetManager.open(fileName);
        File outputFile = new File(getFilesDir(), fileName);
        
        // Skip if already exists
        if (outputFile.exists()) {
            return true;
        }
        
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        
        inputStream.close();
        outputStream.close();
        return true;
    } catch (IOException e) {
        Log.e(TAG, "Failed to copy asset: " + fileName, e);
        return false;
    }
}
```

## 17. Permission Handling

### Runtime Permission Pattern
```java
// Comprehensive permission handling for Android 6.0+
private final String[] permissions = {
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE
};

private void checkPermissions() {
    List<String> permissionsToRequest = new ArrayList<>();
    
    for (String permission : permissions) {
        if (ContextCompat.checkSelfPermission(this, permission) 
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(permission);
        }
    }
    
    if (!permissionsToRequest.isEmpty()) {
        ActivityCompat.requestPermissions(this, 
            permissionsToRequest.toArray(new String[0]), 
            PERMISSION_REQUEST_CODE);
    }
}

@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    
    boolean allGranted = true;
    for (int result : grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
            allGranted = false;
            break;
        }
    }
    
    if (!allGranted) {
        Toast.makeText(this, "Permissions required for audio features", Toast.LENGTH_LONG).show();
        finish();
    }
}
```

## 18. Error Handling Patterns

### Native Callback Error Handling
```java
// INativeNuiCallback implementation
@Override
public void onNuiError(String errorCode, String errorMessage) {
    Log.e(TAG, "onNuiError: " + errorCode + " - " + errorMessage);
    
    // Handle specific error codes
    if (errorCode.equals("20000001")) {
        // Network connection error
        showError("Network connection failed. Please check your internet connection.");
    } else if (errorCode.equals("20000002")) {
        // Authentication error
        showError("Authentication failed. Please check your credentials.");
    } else if (errorCode.equals("20000003")) {
        // Audio device error
        showError("Audio device error. Please check microphone permissions.");
    } else {
        // Generic error
        showError("Error: " + errorMessage);
    }
}
```

### Resource Initialization Error Handling
```java
private boolean initializeRecognizer() {
    try {
        // Load authentication
        JSONObject authInfo = Auth.getTicket(Auth.GetTicketMethod.GET_TOKEN_FROM_SERVER_FOR_ONLINE_FEATURES);
        
        if (authInfo == null || !authInfo.containsKey("token")) {
            showError("Failed to get authentication token");
            return false;
        }
        
        // Initialize native SDK
        int ret = nui_instance.initialize(authInfo.toJSONString(), 
                                         workspace, 
                                         debugPath, 
                                         Constants.LogLevel.LOG_LEVEL_VERBOSE);
        
        if (ret != 0) {
            showError("SDK initialization failed with code: " + ret);
            return false;
        }
        
        return true;
        
    } catch (Exception e) {
        Log.e(TAG, "Initialization error", e);
        showError("Initialization failed: " + e.getMessage());
        return false;
    }
}
```

## 19. Build Configuration Best Practices

### Gradle Configuration
```gradle
android {
    compileSdkVersion 33
    defaultConfig {
        applicationId "mit.alibaba.nuidemo"
        minSdkVersion 14  // Support for older devices
        targetSdkVersion 33
        
        // Multi-architecture support
        ndk {
            abiFilters "armeabi-v7a", "arm64-v8a", "x86", "x86_64"
        }
    }
    
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']  // Native libraries location
        }
    }
}

dependencies {
    // Local SDK integration
    implementation files('libs/nuisdk-release.aar')
    implementation 'com.alibaba:fastjson:1.1.46.android'
    
    // Network dependencies
    implementation 'com.squareup.okhttp3:okhttp:4.9.0'
    implementation 'com.squareup.okhttp3:okhttp-sse:4.9.0'
}
```

## 20. Key Integration Checklist

### Pre-Development Setup
1. **Obtain Alibaba Cloud credentials** (AK/SK, AppKey)
2. **Download official SDK** (nuisdk-release.aar)
3. **Prepare resource files** (nui.json, vad.bin, kws.bin, cei.json)
4. **Configure network permissions** in AndroidManifest.xml

### Development Phase
1. **Implement authentication flow** (prefer server-side token generation)
2. **Set up audio recording** (16kHz, 16-bit, mono PCM)
3. **Configure SDK initialization** with proper workspace paths
4. **Implement error handling** for all SDK callbacks
5. **Test on multiple architectures** (ARM, ARM64, x86)

### Production Considerations
1. **Secure credential storage** (never hardcode AK/SK in client)
2. **Implement token refresh** mechanism
3. **Handle network failures** gracefully
4. **Optimize battery usage** during audio recording
5. **Comply with privacy regulations** for voice data

## Summary

The official Alibaba NUI SDK demonstrates enterprise-grade patterns for:

1. **Multi-layered authentication** with STS token support
2. **Robust audio processing** with proper threading
3. **Comprehensive error handling** with user-friendly messages
4. **Resource management** with automatic copying and validation
5. **Thread-safe concurrency** for real-time audio processing
6. **Permission handling** for modern Android versions
7. **Multi-architecture support** for broad device compatibility

These practices provide a production-ready foundation for implementing voice AI features in Android applications using Alibaba's official SDK.