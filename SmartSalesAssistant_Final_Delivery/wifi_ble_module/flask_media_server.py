#!/usr/bin/env python3
"""
Flask-based Media Server Simulator
Simulates the Raspberry Pi wifi.py behavior for local testing
"""

import os
import json
import time
import mimetypes
import urllib.parse
from pathlib import Path
from flask import Flask, request, jsonify, send_file, render_template_string
from flask_cors import CORS
from werkzeug.utils import secure_filename

app = Flask(__name__)
CORS(app)  # Enable CORS for Android WebView access

# Configuration
ROOT_DIR = "./test_media_files"
PORT = 8000
MAX_UPLOAD_SIZE = 5 * 1024 * 1024  # 5MB

IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif'}
VIDEO_EXTENSIONS = {'.mp4', '.mp3', '.avi', '.mov'}

# Ensure test directory exists
os.makedirs(ROOT_DIR, exist_ok=True)

def is_media_file(filename):
    """Check if file is a supported media file"""
    ext = os.path.splitext(filename)[1].lower()
    return ext in IMAGE_EXTENSIONS or ext in VIDEO_EXTENSIONS

def generate_file_list_json():
    """Generate JSON response for Android app file listing"""
    try:
        files = []
        for f in os.listdir(ROOT_DIR):
            full_path = os.path.join(ROOT_DIR, f)
            if os.path.isfile(full_path) and is_media_file(f):
                stat = os.stat(full_path)
                ext = os.path.splitext(f)[1].lower()
                file_type = 'image' if ext in IMAGE_EXTENSIONS else 'video'
                files.append({
                    'name': f,
                    'size': stat.st_size,
                    'modified': stat.st_mtime,
                    'type': file_type,
                    'url': f'/{urllib.parse.quote(f, safe="")}',
                    'download_url': f'/download/{urllib.parse.quote(f, safe="")}'
                })
        files.sort(key=lambda x: x['name'].lower())
        return {
            'files': files,
            'total_count': len(files),
            'directory': os.path.abspath(ROOT_DIR),
            'server': 'media_server'
        }
    except Exception as e:
        return {
            'files': [],
            'total_count': 0,
            'directory': os.path.abspath(ROOT_DIR),
            'error': str(e)
        }

@app.route('/')
def index():
    """Root endpoint - returns JSON info for Android app"""
    print(f"[ANDROID] Media server root accessed from {request.remote_addr}")
    response = {
        "message": "Media server is running. Use /api/files for file listing.",
        "android_integration": True,
        "server": "media_server",
        "timestamp": time.time(),
        "endpoints": {
            "file_list": "/api/files",
            "file_download": "/download/{filename}",
            "file_upload": "/upload",
            "file_delete": "/delete/{filename}",
            "file_apply": "/apply/{filename}"
        }
    }
    return jsonify(response)

@app.route('/api/files')
def api_files():
    """JSON API endpoint for file listing"""
    print(f"[ANDROID] File list API accessed from {request.remote_addr}")
    file_list = generate_file_list_json()
    return jsonify(file_list)

@app.route('/download/<path:filename>')
def download_file(filename):
    """Download a file"""
    try:
        # Decode URL-encoded filename
        decoded_name = urllib.parse.unquote(filename)
        safe_path = os.path.join(ROOT_DIR, decoded_name)
        
        if not os.path.isfile(safe_path):
            return jsonify({"error": "File not found"}), 404
        
        print(f"[DOWNLOAD] Serving file: {decoded_name}")
        return send_file(safe_path, as_attachment=True, download_name=decoded_name)
    except Exception as e:
        return jsonify({"error": f"Download failed: {str(e)}"}), 500

@app.route('/<path:filename>')
def serve_file(filename):
    """Serve a file directly (for viewing in browser/WebView)"""
    try:
        decoded_name = urllib.parse.unquote(filename)
        safe_path = os.path.join(ROOT_DIR, decoded_name)
        
        if not os.path.isfile(safe_path):
            return jsonify({"error": "File not found"}), 404
        
        if not is_media_file(decoded_name):
            return jsonify({"error": "Unsupported file type"}), 400
        
        print(f"[SERVE] Serving file: {decoded_name}")
        return send_file(safe_path)
    except Exception as e:
        return jsonify({"error": f"Serve failed: {str(e)}"}), 500

@app.route('/upload', methods=['POST'])
def upload_file():
    """Upload a file"""
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file part"}), 400
        
        files = request.files.getlist('file')
        uploaded_files = []
        
        for file in files:
            if file.filename == '':
                continue
            
            filename = secure_filename(file.filename)
            if not is_media_file(filename):
                return jsonify({"error": f"Unsupported file type: {filename}"}), 400
            
            # Check file size
            file.seek(0, os.SEEK_END)
            file_size = file.tell()
            file.seek(0)
            
            if file_size > MAX_UPLOAD_SIZE:
                return jsonify({"error": f"File too large: {filename}"}), 413
            
            # Save file
            file_path = os.path.join(ROOT_DIR, filename)
            file.save(file_path)
            uploaded_files.append(filename)
            print(f"[UPLOAD] Saved file: {filename} ({file_size} bytes)")
        
        if uploaded_files:
            return jsonify({
                "message": f"Successfully uploaded {len(uploaded_files)} file(s)",
                "files": uploaded_files
            })
        else:
            return jsonify({"error": "No files uploaded"}), 400
            
    except Exception as e:
        return jsonify({"error": f"Upload failed: {str(e)}"}), 500

@app.route('/delete/<path:filename>', methods=['DELETE'])
def delete_file(filename):
    """Delete a file"""
    try:
        decoded_name = urllib.parse.unquote(filename)
        safe_path = os.path.join(ROOT_DIR, decoded_name)
        
        if not os.path.isfile(safe_path):
            return jsonify({"error": "File not found"}), 404
        
        os.remove(safe_path)
        print(f"[DELETE] Deleted file: {decoded_name}")
        return jsonify({"message": f"File deleted: {decoded_name}"})
        
    except Exception as e:
        return jsonify({"error": f"Delete failed: {str(e)}"}), 500

@app.route('/apply/<path:filename>', methods=['POST'])
def apply_file(filename):
    """Apply/select a file"""
    try:
        decoded_name = urllib.parse.unquote(filename)
        safe_path = os.path.join(ROOT_DIR, decoded_name)
        
        if not os.path.isfile(safe_path):
            return jsonify({"error": "File not found"}), 404
        
        if not is_media_file(decoded_name):
            return jsonify({"error": "Not a media file"}), 400
        
        # Simulate the apply functionality (write to target file)
        target_file = "./applied_file.txt"
        with open(target_file, 'w', encoding='utf-8') as f:
            f.write(decoded_name + '\n')
        
        print(f"[APPLY] Applied file: {decoded_name}")
        
        # Simulate UDP signal (just print for now)
        print(f"[UDP] Would send RELOAD signal for: {decoded_name}")
        
        return jsonify({"message": f"File applied: {decoded_name}"})
        
    except Exception as e:
        return jsonify({"error": f"Apply failed: {str(e)}"}), 500

@app.route('/status')
def status():
    """Server status endpoint"""
    return jsonify({
        "status": "running",
        "server": "flask_media_server",
        "version": "1.0.0",
        "root_directory": os.path.abspath(ROOT_DIR),
        "uptime": time.time()
    })

@app.route('/test-ui')
def test_ui():
    """Test the original web UI (for comparison)"""
    files = generate_file_list_json()['files']
    
    html_template = '''
    <!DOCTYPE html>
    <html>
    <head>
        <title>Test Media Server UI</title>
        <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .file-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 20px; }
            .file-item { border: 1px solid #ddd; padding: 10px; border-radius: 5px; }
            .file-name { font-weight: bold; margin-bottom: 5px; }
            .file-actions { margin-top: 10px; }
            .btn { padding: 5px 10px; margin: 2px; text-decoration: none; color: white; border-radius: 3px; }
            .btn-download { background: #4CAF50; }
            .btn-delete { background: #f44336; }
            .btn-apply { background: #673ab7; }
            .upload-area { margin: 20px 0; padding: 20px; border: 2px dashed #ccc; text-align: center; }
        </style>
    </head>
    <body>
        <h1>🎬 Test Media Server UI</h1>
        <p>This is a simplified version of the original UI for testing purposes.</p>
        
        <div class="upload-area">
            <h3>Upload Files</h3>
            <form action="/upload" method="post" enctype="multipart/form-data">
                <input type="file" name="file" multiple accept="image/*,video/*">
                <button type="submit">Upload</button>
            </form>
        </div>
        
        <h2>Files ({{ files|length }})</h2>
        <div class="file-grid">
        {% for file in files %}
            <div class="file-item">
                <div class="file-name">{{ file.name }}</div>
                <div class="file-info">
                    Type: {{ file.type }} | Size: {{ file.size }} bytes
                </div>
                <div class="file-actions">
                    <a href="/download/{{ file.name|urlencode }}" class="btn btn-download">Download</a>
                    <a href="/apply/{{ file.name|urlencode }}" class="btn btn-apply" onclick="return confirm('Apply this file?')">Apply</a>
                    <a href="/delete/{{ file.name|urlencode }}" class="btn btn-delete" onclick="return confirm('Delete this file?')">Delete</a>
                </div>
            </div>
        {% endfor %}
        </div>
    </body>
    </html>
    '''
    
    return render_template_string(html_template, files=files)

def create_test_files():
    """Create some test media files for demonstration"""
    test_files = [
        ("sample1.jpg", "image/jpeg", b"\xff\xd8\xff\xe0\x00\x10JFIF"),  # JPEG header
        ("sample2.png", "image/png", b"\x89PNG\r\n\x1a\n"),  # PNG header
        ("video1.mp4", "video/mp4", b"\x00\x00\x00\x18ftypmp4"),  # MP4 header
        ("audio1.mp3", "audio/mpeg", b"\xff\xfb\x90"),  # MP3 header
    ]
    
    for filename, mime_type, header in test_files:
        file_path = os.path.join(ROOT_DIR, filename)
        if not os.path.exists(file_path):
            with open(file_path, 'wb') as f:
                f.write(header + b"\x00" * 1024)  # 1KB test files
            print(f"[TEST] Created test file: {filename}")

if __name__ == '__main__':
    print(f"🚀 Starting Flask Media Server Simulator on port {PORT}")
    print(f"📁 Media directory: {os.path.abspath(ROOT_DIR)}")
    
    # Create test files
    create_test_files()
    
    print("\n📋 Available endpoints:")
    print(f"  http://localhost:{PORT}/           - Server info (JSON)")
    print(f"  http://localhost:{PORT}/api/files  - File list (JSON)")
    print(f"  http://localhost:{PORT}/test-ui    - Test web UI")
    print(f"  http://localhost:{PORT}/status     - Server status")
    print("\n🔧 File operations:")
    print(f"  GET  /download/<filename> - Download file")
    print(f"  GET  /<filename>          - View file")
    print(f"  POST /upload              - Upload file")
    print(f"  DELETE /delete/<filename> - Delete file")
    print(f"  POST /apply/<filename>    - Apply file")
    print(f"\n🌐 Starting server...")
    
    app.run(host='0.0.0.0', port=PORT, debug=True)