#!/usr/bin/env python3
"""
Simple HTTP Media Server Simulator
Simulates the Raspberry Pi wifi.py behavior using only built-in Python libraries
"""

import os
import json
import time
import mimetypes
import urllib.parse
import http.server
import socketserver
from pathlib import Path
import threading

# Configuration
ROOT_DIR = "./test_media_files"
PORT = 34123
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

def create_test_files():
    """Create some test media files for demonstration"""
    test_files = [
        ("sample1.jpg", "image/jpeg", b"\xff\xd8\xff\xe0\x00\x10JFIF" + b"\x00" * 1024),  # JPEG
        ("sample2.png", "image/png", b"\x89PNG\r\n\x1a\n" + b"\x00" * 1024),  # PNG
        ("video1.mp4", "video/mp4", b"\x00\x00\x00\x18ftypmp4" + b"\x00" * 1024),  # MP4
        ("audio1.mp3", "audio/mpeg", b"\xff\xfb\x90" + b"\x00" * 1024),  # MP3
        ("demo_image.jpg", "image/jpeg", b"\xff\xd8\xff\xe0" + b"\x00" * 2048),  # Larger JPEG
        ("test_video.mp4", "video/mp4", b"\x00\x00\x00\x18ftyp" + b"\x00" * 2048),  # Larger MP4
    ]
    
    for filename, mime_type, content in test_files:
        file_path = os.path.join(ROOT_DIR, filename)
        if not os.path.exists(file_path):
            with open(file_path, 'wb') as f:
                f.write(content)
            print(f"[TEST] Created test file: {filename} ({len(content)} bytes)")

class MediaServerHandler(http.server.SimpleHTTPRequestHandler):
    """Custom HTTP handler for media server simulation"""
    
    def do_GET(self):
        """Handle GET requests"""
        print(f"[GET] {self.path} from {self.client_address}")
        
        # Root endpoint - JSON info for Android app
        if self.path == '/' or self.path == '/index.html':
            self.send_json_response({
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
            })
            
        # JSON API endpoint for file listing
        elif self.path == '/api/files':
            file_list = generate_file_list_json()
            self.send_json_response(file_list)
            
        # File download endpoint
        elif self.path.startswith('/download/'):
            filename = self.path[len('/download/'):]
            self.handle_file_download(filename)
            
        # Direct file serving
        elif self.path.startswith('/') and is_media_file(self.path[1:]):
            filename = self.path[1:]  # Remove leading slash
            self.handle_file_serve(filename)
            
        # Server status
        elif self.path == '/status':
            self.send_json_response({
                "status": "running",
                "server": "simple_media_server",
                "version": "1.0.0",
                "root_directory": os.path.abspath(ROOT_DIR),
                "uptime": time.time()
            })
            
        else:
            self.send_error(404, "Not Found")
    
    def do_POST(self):
        """Handle POST requests"""
        print(f"[POST] {self.path} from {self.client_address}")
        
        if self.path == '/upload':
            self.handle_file_upload()
        elif self.path.startswith('/apply/'):
            filename = self.path[len('/apply/'):]
            self.handle_file_apply(filename)
        else:
            self.send_error(404, "Not Found")
    
    def do_DELETE(self):
        """Handle DELETE requests"""
        print(f"[DELETE] {self.path} from {self.client_address}")
        
        if self.path.startswith('/delete/'):
            filename = self.path[len('/delete/'):]
            self.handle_file_delete(filename)
        else:
            self.send_error(404, "Not Found")
    
    def send_json_response(self, data):
        """Send JSON response with proper headers"""
        json_data = json.dumps(data, ensure_ascii=False, indent=2)
        self.send_response(200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')  # CORS for Android
        self.send_header('Content-Length', str(len(json_data.encode('utf-8'))))
        self.end_headers()
        self.wfile.write(json_data.encode('utf-8'))
    
    def handle_file_download(self, filename):
        """Handle file download requests"""
        try:
            decoded_name = urllib.parse.unquote(filename)
            safe_path = os.path.join(ROOT_DIR, decoded_name)
            
            if not os.path.isfile(safe_path):
                self.send_error(404, "File not found")
                return
            
            print(f"[DOWNLOAD] Serving file: {decoded_name}")
            
            # Send file
            self.send_response(200)
            self.send_header('Content-Type', 'application/octet-stream')
            self.send_header('Content-Disposition', f'attachment; filename="{decoded_name}"')
            
            with open(safe_path, 'rb') as f:
                content = f.read()
                self.send_header('Content-Length', str(len(content)))
                self.end_headers()
                self.wfile.write(content)
                
        except Exception as e:
            self.send_error(500, f"Download failed: {str(e)}")
    
    def handle_file_serve(self, filename):
        """Handle direct file serving for viewing"""
        try:
            decoded_name = urllib.parse.unquote(filename)
            safe_path = os.path.join(ROOT_DIR, decoded_name)
            
            if not os.path.isfile(safe_path):
                self.send_error(404, "File not found")
                return
            
            print(f"[SERVE] Serving file: {decoded_name}")
            
            # Determine content type
            ext = os.path.splitext(decoded_name)[1].lower()
            if ext in IMAGE_EXTENSIONS:
                content_type = f'image/{ext[1:]}'
            elif ext in VIDEO_EXTENSIONS:
                content_type = f'video/{ext[1:]}'
            else:
                content_type = 'application/octet-stream'
            
            self.send_response(200)
            self.send_header('Content-Type', content_type)
            self.send_header('Access-Control-Allow-Origin', '*')
            
            with open(safe_path, 'rb') as f:
                content = f.read()
                self.send_header('Content-Length', str(len(content)))
                self.end_headers()
                self.wfile.write(content)
                
        except Exception as e:
            self.send_error(500, f"Serve failed: {str(e)}")
    
    def handle_file_upload(self):
        """Handle file upload requests"""
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            if content_length > MAX_UPLOAD_SIZE:
                self.send_error(413, "Request entity too large")
                return
            
            # Simple multipart form data parsing (basic implementation)
            post_data = self.rfile.read(content_length)
            
            # This is a simplified implementation - in production you'd use a proper parser
            # For now, we'll simulate a successful upload
            boundary = self.headers.get('Content-Type', '').split('boundary=')[-1]
            
            # Extract filename (simplified parsing)
            try:
                filename_part = post_data.split(b'filename="')[1].split(b'"')[0]
                filename = filename_part.decode('utf-8')
                
                if not is_media_file(filename):
                    self.send_error(400, "Unsupported file type")
                    return
                
                # Extract file content (simplified)
                file_start = post_data.find(b'\r\n\r\n') + 4
                file_end = post_data.rfind(b'\r\n--' + boundary.encode())
                file_content = post_data[file_start:file_end]
                
                # Save file
                safe_filename = filename  # In production, use secure_filename
                file_path = os.path.join(ROOT_DIR, safe_filename)
                
                with open(file_path, 'wb') as f:
                    f.write(file_content)
                
                print(f"[UPLOAD] Saved file: {safe_filename} ({len(file_content)} bytes)")
                
                self.send_json_response({
                    "message": f"Successfully uploaded {safe_filename}",
                    "file": safe_filename,
                    "size": len(file_content)
                })
                
            except Exception as e:
                self.send_error(400, f"Upload parsing failed: {str(e)}")
                
        except Exception as e:
            self.send_error(500, f"Upload failed: {str(e)}")
    
    def handle_file_delete(self, filename):
        """Handle file deletion requests"""
        try:
            decoded_name = urllib.parse.unquote(filename)
            safe_path = os.path.join(ROOT_DIR, decoded_name)
            
            if not os.path.isfile(safe_path):
                self.send_error(404, "File not found")
                return
            
            os.remove(safe_path)
            print(f"[DELETE] Deleted file: {decoded_name}")
            
            self.send_json_response({
                "message": f"File deleted: {decoded_name}"
            })
            
        except Exception as e:
            self.send_error(500, f"Delete failed: {str(e)}")
    
    def handle_file_apply(self, filename):
        """Handle file apply requests"""
        try:
            decoded_name = urllib.parse.unquote(filename)
            safe_path = os.path.join(ROOT_DIR, decoded_name)
            
            if not os.path.isfile(safe_path):
                self.send_error(404, "File not found")
                return
            
            if not is_media_file(decoded_name):
                self.send_error(400, "Not a media file")
                return
            
            # Simulate apply functionality
            applied_file = "./applied_file.txt"
            with open(applied_file, 'w', encoding='utf-8') as f:
                f.write(decoded_name + '\n')
            
            print(f"[APPLY] Applied file: {decoded_name}")
            print(f"[UDP] Would send RELOAD signal for: {decoded_name}")
            
            self.send_json_response({
                "message": f"File applied: {decoded_name}"
            })
            
        except Exception as e:
            self.send_error(500, f"Apply failed: {str(e)}")
    
    def log_message(self, format, *args):
        """Override to customize logging"""
        print(f"[HTTP] {format % args}")

def get_local_ip():
    """Get local IP address"""
    try:
        with socketserver.socket.socket(socketserver.socket.AF_INET, socketserver.socket.SOCK_DGRAM) as s:
            s.connect(('10.255.255.255', 1))
            return s.getsockname()[0]
    except Exception:
        return '127.0.0.1'

def main():
    """Main function to start the server"""
    print(f"🚀 Starting Simple Media Server Simulator on port {PORT}")
    print(f"📁 Media directory: {os.path.abspath(ROOT_DIR)}")
    
    # Create test files
    create_test_files()
    
    print("\n📋 Available endpoints:")
    print(f"  http://localhost:{PORT}/           - Server info (JSON)")
    print(f"  http://localhost:{PORT}/api/files  - File list (JSON)")
    print(f"  http://localhost:{PORT}/status     - Server status")
    print("\n🔧 File operations:")
    print(f"  GET  /download/<filename> - Download file")
    print(f"  GET  /<filename>          - View file")
    print(f"  POST /upload              - Upload file")
    print(f"  DELETE /delete/<filename> - Delete file")
    print(f"  POST /apply/<filename>    - Apply file")
    
    # Get local IP for network access
    local_ip = get_local_ip()
    print(f"\n🌐 Server URLs:")
    print(f"  Local:  http://localhost:{PORT}")
    print(f"  Network: http://{local_ip}:{PORT}")
    print(f"\n📱 For Android testing, use: http://{local_ip}:{PORT}")
    print(f"\n⏹️  Press Ctrl+C to stop the server")
    
    # Start server
    with socketserver.TCPServer(("0.0.0.0", PORT), MediaServerHandler) as httpd:
        print(f"\n✅ Server started successfully!")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print(f"\n⏹️  Server stopped by user")

if __name__ == '__main__':
    main()