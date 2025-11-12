#!/usr/bin/env python3
import os
import urllib.parse
import html
from http.server import HTTPServer, BaseHTTPRequestHandler
import socket
import shutil
import cgi
from http import HTTPStatus
import threading
import time
import socket
import json
import mimetypes

# 配置
ROOT_DIR = "/home/cat/pic"
PORT = 8000
CHECK_INTERVAL = 5  # 每5秒检查一次IP
pub_socket = None
# 全局状态（线程安全）
_current_server = None
_current_ip = None
_server_lock = threading.Lock()
_stop_event = threading.Event()

IMAGE_EXTENSIONS = {'.jpg', '.jpeg', '.png', '.gif'}
VIDEO_EXTENSIONS = {'.mp4', '.mp3'}
MAX_UPLOAD_SIZE = 5 * 1024 * 1024  # 5MB
    #UDP
# 配置
server_HOST = '127.0.0.1'  # 本地回环地址
server_PORT = 9000        # 端口号
server_socket = None

print(f"UDP 服务端已启动，监听 {server_HOST}:{server_PORT}")

def is_media_file(filename):
    ext = os.path.splitext(filename)[1].lower()
    return ext in IMAGE_EXTENSIONS or ext in VIDEO_EXTENSIONS

def safe_join(base, path):
    resolved = os.path.normpath(os.path.join(base, path))
    if not resolved.startswith(os.path.abspath(base)):
        raise ValueError("Invalid path")
    return resolved

# ...（generate_index_html() 函数保持不变）...
# COMMENTED OUT FOR ANDROID INTEGRATION - Original UI generation function
# This function generated the HTML/CSS/JS web interface for direct browser access
# Now the UI will be accessed through Android WebView instead
# def generate_index_html():
#     try:
#         files = []
#         for f in os.listdir(ROOT_DIR):
#             full_path = os.path.join(ROOT_DIR, f)
#             if os.path.isfile(full_path) and is_media_file(f):
#                 files.append(f)
#         files.sort(key=lambda x: x.lower())
#     except Exception:
#         files = []
# 
#     file_count = len(files)
# 
#     html_head = '''<!DOCTYPE html>
# <html lang="zh-CN">
# <head>
#     <meta charset="UTF-8">
#     <meta name="viewport" content="width=device-width, initial-scale=1.0">
#     <title>📸 媒体库</title>
#     <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
#     <style>
#         * {{
#             margin: 0;
#             padding: 0;
#             box-sizing: border-box;
#         }}
#         body {{
#             font-family: 'Segoe UI', system-ui, sans-serif;
#             background: linear-gradient(135deg, #f8f9fa, #e9ecef);
#             padding: 20px;
#             min-height: 100vh;
#         }}
#         .container {{
#             max-width: 1400px;
#             margin: 0 auto;
#         }}
#         header {{
#             text-align: center;
#             margin-bottom: 30px;
#             padding: 20px;
#         }}
#         h1 {{
#             font-size: 2.2rem;
#             color: #2c3e50;
#             margin-bottom: 8px;
#         }}
#         .subtitle {{
#             color: #7f8c8d;
#             font-size: 1rem;
#         }}
#         .upload-area {{
#             text-align: center;
#             margin-bottom: 30px;
#         }}
#         .btn-upload {{
#             background: #2196F3;
#             color: white;
#             padding: 10px 20px;
#             border: none;
#             border-radius: 6px;
#             font-size: 16px;
#             font-weight: 600;
#             cursor: pointer;
#             transition: opacity 0.2s;
#         }}
#         .btn-upload:hover {{
#             opacity: 0.9;
#         }}
#         .grid {{
#             display: grid;
#             grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
#             gap: 24px;
#         }}
#         .item {{
#             background: white;
#             border-radius: 12px;
#             box-shadow: 0 4px 12px rgba(0,0,0,0.08);
#             overflow: hidden;
#             transition: transform 0.25s ease, box-shadow 0.25s ease;
#         }}
#         .item:hover {{
#             transform: translateY(-6px);
#             box-shadow: 0 6px 16px rgba(0,0,0,0.12);
#         }}
#         .media {{
#             width: 100%;
#             height: 160px;
#             display: flex;
#             align-items: center;
#             justify-content: center;
#             background: #f1f3f5;
#             overflow: hidden;
#         }}
#         .media img {{
#             width: 100%;
#             height: 100%;
#             object-fit: cover;
#         }}
#         .media video {{
#             width: 100%;
#             height: 100%;
#             object-fit: cover;
#             background: black;
#         }}
#         .media i {{
#             font-size: 48px;
#             color: #adb5bd;
#         }}
#         .info {{
#             padding: 12px;
#             text-align: center;
#         }}
#         .name {{
#             font-size: 14px;
#             color: #343a40;
#             margin-bottom: 10px;
#             word-break: break-word;
#             line-height: 1.4;
#         }}
#         .actions {{
#             display: flex;
#             justify-content: center;
#             gap: 12px;
#         }}
#         .btn {{
#             flex: 1;
#             padding: 6px 12px;
#             border: none;
#             border-radius: 6px;
#             font-size: 13px;
#             font-weight: 600;
#             cursor: pointer;
#             transition: opacity 0.2s;
#         }}
#         .btn:hover {{
#             opacity: 0.9;
#         }}
#         .btn-download {{
#             background: #4CAF50;
#             color: white;
#         }}
#         .btn-delete {{
#             background: #f44336;
#             color: white;
#         }}
#         .empty {{
#             text-align: center;
#             padding: 60px 20px;
            color: #6c757d;
#             grid-column: 1 / -1;
#         }}
#         @media (max-width: 600px) {{
#             .grid {{
#                 grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
#             }}
#             .media {{
#                 height: 120px;
#             }}
#         }}
#     </style>
# </head>
# <body>
#     <div class="container">
#         <header>
#             <h1>📸 媒体库</h1>
#             <div class="subtitle">共 {} 个文件</div>
#         </header>
#
#         <div class="upload-area">
#             <form id="uploadForm" enctype="multipart/form-data">
#                 <input type="file" id="fileInput" name="file" accept="image/*,video/*" multiple style="display:none;" />
#                 <button type="button" class="btn-upload" onclick="document.getElementById('fileInput').click()">
#                     <i class="fas fa-cloud-upload-alt"></i> 上传媒体（单文件 ≤5MB）
#                 </button>
#             </form>
#         </div>
#
#         <div class="grid">
'''.format(file_count)

    html_parts = [html_head]

    if not files:
        html_parts.append('''
            <div class="empty">
                <i class="fas fa-folder-open fa-3x" style="margin-bottom:16px;"></i>
                <p>暂无媒体文件</p>
            </div>
''')
    else:
        for f in files:
            safe_name_html = html.escape(f)
            safe_name_url = urllib.parse.quote(f, safe='')
            ext = os.path.splitext(f)[1].lower()

            if ext in IMAGE_EXTENSIONS:
                media_tag = f'<img src="/{safe_name_url}" alt="图片">'
            elif ext in VIDEO_EXTENSIONS:
                mime_type = f"video/{ext[1:]}"
                media_tag = f'<video controls preload="metadata"><source src="/{safe_name_url}" type="{mime_type}">您的浏览器不支持视频。</video>'
            else:
                media_tag = '<i class="fas fa-file"></i>'

            html_parts.append(f'''
                <div class="item">
                    <div class="media">
                        {media_tag}
                    </div>
                    <div class="info">
                        <div class="name">{safe_name_html}</div>
                        <div class="actions">
                            <button class="btn btn-download" onclick="downloadFile('{safe_name_html}')">
                                <i class="fas fa-download"></i> 下载
                            </button>
                            <button class="btn btn-delete" onclick="deleteFile('{safe_name_html}')">
                                <i class="fas fa-trash"></i> 删除
                            </button>
                            <button class="btn" style="background:#673ab7;color:white;" onclick="applyFile('{safe_name_html}')">
                                <i class="fas fa-check-circle"></i> 应用
                        </div>
                    </div>
                </div>
            ''')

    html_parts.append('''
        </div>
    </div>

    <script>
        const MAX_FILE_SIZE = 5 * 1024 * 1024; // 10 MB

        function downloadFile(filename) {
            const url = '/download/' + encodeURIComponent(filename);
            window.location.href = url;
        }

        function deleteFile(filename) {
            if (!confirm('⚠️ 确定要删除文件「' + filename + '」吗？此操作不可恢复！')) {
                return;
            }
            fetch('/delete/' + encodeURIComponent(filename), {
                method: 'DELETE',
            })
            .then(response => {
                if (response.ok) {
                    alert('✅ 文件已删除');
                    location.reload();
                } else {
                    response.text().then(text => alert('❌ 删除失败: ' + text));
                }
            })
            .catch(err => {
                console.error(err);
                alert('❌ 网络错误');
            });
        }
        function applyFile(filename) {
            fetch('/apply/' + encodeURIComponent(filename), {
                method: 'POST',
            })
            .then(response => {
                if (response.ok) {
                    alert('✅ 已应用：' + filename);
                } else {
                    return response.text().then(text => {
                        throw new Error(text || '应用失败');
                    });
                }
            })
            .catch(err => {
                console.error(err);
                alert('❌ 应用失败: ' + err.message);
            });
        }
        document.getElementById('fileInput').addEventListener('change', function(event) {
            const files = Array.from(event.target.files);
            if (files.length === 0) return;

            const oversized = files.filter(file => file.size > MAX_FILE_SIZE);
            if (oversized.length > 0) {
                const names = oversized.map(f => f.name).join(', ');
                alert(`❌ 以下文件超过 5MB 限制：\\n${names}`);
                event.target.value = '';
                return;
            }

            const invalid = files.filter(file => 
                !file.type.startsWith('image/') && !file.type.startsWith('video/')
            );
            if (invalid.length > 0) {
                const names = invalid.map(f => f.name).join(', ');
                alert(`❌ 仅支持图片和视频文件：\\n${names}`);
                event.target.value = '';
                return;
            }

            const formData = new FormData();
            files.forEach(file => formData.append('file', file));

            fetch('/upload', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                if (response.ok) {
                    alert('✅ 上传成功！');
                    location.reload();
                } else {
                    return response.text().then(text => {
                        throw new Error(text || '上传失败');
                    });
                }
            })
            .catch(err => {
                console.error(err);
                alert('❌ 上传失败: ' + err.message);
            });
        });
    </script>
</body>
</html>
''')
    return ''.join(html_parts)

# ANDROID INTEGRATION: JSON API helper function
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
            'directory': ROOT_DIR,
            'server': 'media_server'
        }
    except Exception as e:
        return {
            'files': [],
            'total_count': 0,
            'directory': ROOT_DIR,
            'error': str(e)
        }

def get_ip():
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(('10.255.255.255', 1))
            return s.getsockname()[0]
    except Exception:
        return "127.0.0.1"

# ========================
# Handler（保持不变）
# ========================
class MediaHandler(BaseHTTPRequestHandler):
    def handle(self):
        try:
            super().handle()
        except (BrokenPipeError, ConnectionResetError):
            return

    def do_GET(self):
        # ANDROID INTEGRATION: Modified to support both web UI and JSON API
        if self.path == '/' or self.path == '/index.html':
            # Original HTML UI - now commented out for Android integration
            # self.send_response(200)
            # self.send_header("Content-type", "text/html; charset=utf-8")
            # self.end_headers()
            # self.wfile.write(generate_index_html().encode('utf-8'))
            
            # Return simple JSON response for Android app
            self.send_response(200)
            self.send_header("Content-type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")  # Enable CORS for Android WebView
            self.end_headers()
            import json
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
            print(f"[ANDROID] Media server root accessed from {self.client_address}")
            self.wfile.write(json.dumps(response, ensure_ascii=False).encode('utf-8'))
            
        elif self.path.startswith('/download/'):
            filename = self.path[len('/download/'):]
            self.serve_download(filename)
        elif self.path == '/api/files':
            # JSON API endpoint for Android app file listing
            self.send_response(200)
            self.send_header("Content-type", "application/json; charset=utf-8")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            import json
            file_list = generate_file_list_json()
            print(f"[ANDROID] File list API accessed from {self.client_address}")
            self.wfile.write(json.dumps(file_list, ensure_ascii=False).encode('utf-8'))
        else:
            self.serve_file(self.path.lstrip('/'))

    def do_POST(self):
        if self.path == '/upload':
            self.handle_upload()
        elif self.path.startswith('/apply/'):
            filename = self.path[len('/apply/'):]
            self.apply_file(filename)
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def do_DELETE(self):
        if self.path.startswith('/delete/'):
            filename = self.path[len('/delete/'):]
            self.delete_file(filename)
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def serve_file(self, filename):
        try:
            safe_path = safe_join(ROOT_DIR, filename)
            if not os.path.isfile(safe_path):
                self.send_error(HTTPStatus.NOT_FOUND, "File not found")
                return

            self.send_response(HTTPStatus.OK)
            ext = os.path.splitext(filename)[1].lower()
            if ext in IMAGE_EXTENSIONS:
                content_type = f"image/{ext[1:]}"
            elif ext in VIDEO_EXTENSIONS:
                content_type = f"video/{ext[1:]}"
            else:
                content_type = "application/octet-stream"
            self.send_header("Content-type", content_type)
            self.end_headers()

            with open(safe_path, 'rb') as f:
                shutil.copyfileobj(f, self.wfile)
        except (ValueError, OSError):
            self.send_error(HTTPStatus.FORBIDDEN, "Access denied")

    def serve_download(self, filename):
        try:
            decoded_name = urllib.parse.unquote(filename)
            safe_path = safe_join(ROOT_DIR, decoded_name)
            if not os.path.isfile(safe_path):
                self.send_error(HTTPStatus.NOT_FOUND, "File not found")
                return

            self.send_response(HTTPStatus.OK)
            ascii_name = decoded_name.encode('ascii', errors='replace').decode()
            self.send_header("Content-Disposition", f'attachment; filename="{ascii_name}"; filename*=UTF-8\'\'{urllib.parse.quote(decoded_name)}')
            self.send_header("Content-Type", "application/octet-stream")
            self.end_headers()

            with open(safe_path, 'rb') as f:
                shutil.copyfileobj(f, self.wfile)
        except (ValueError, OSError):
            self.send_error(HTTPStatus.FORBIDDEN, "Access denied")

    def delete_file(self, filename):
        try:
            decoded_name = urllib.parse.unquote(filename)
            safe_path = safe_join(ROOT_DIR, decoded_name)
            if not os.path.isfile(safe_path):
                self.send_error(HTTPStatus.NOT_FOUND, "File not found")
                return

            os.remove(safe_path)
            self.send_response(HTTPStatus.OK)
            self.end_headers()
            self.wfile.write(b"OK")
        except (ValueError, OSError) as e:
            self.send_error(HTTPStatus.FORBIDDEN, f"Delete failed: {e}")
    def apply_file(self, filename):
        try:
            decoded_name = urllib.parse.unquote(filename)
            # 安全校验：确保是合法文件名，且存在于 ROOT_DIR
            if not decoded_name or '/' in decoded_name or '\\' in decoded_name:
                self.send_error(HTTPStatus.BAD_REQUEST, "非法文件名")
                return

            full_path = os.path.join(ROOT_DIR, decoded_name)
            if not os.path.isfile(full_path) or not is_media_file(decoded_name):
                self.send_error(HTTPStatus.NOT_FOUND, "文件不存在或非媒体文件")
                return

            # 写入目标文件
            target_file = "/home/cat/py/python/gif/gif.txt"
            os.makedirs(os.path.dirname(target_file), exist_ok=True)

            with open(target_file, 'w', encoding='utf-8') as f:
                f.write(decoded_name + '\n')
            print(f"[APPLY] 已清空并写入: {decoded_name} -> {target_file}")

            self.send_response(HTTPStatus.OK)
            self.end_headers()
            self.wfile.write(b"OK")
            message = f"WiFi:Publishing:{filename}"
            #发送客户选中的文件名称
            global server_socket
            if server_socket is None:
                server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            server_socket.sendto(b"wifi-gif:RELOAD", ('127.0.0.1', 9000))
            print("[UDP] 已发送 RELOAD 信号")


        except Exception as e:
            self.send_error(HTTPStatus.INTERNAL_SERVER_ERROR, f"写入失败: {e}")
    

    def handle_upload(self):
        MAX_UPLOAD_SIZE = 5 * 1024 * 1024

        content_length_header = self.headers.get('Content-Length')
        if content_length_header is not None:
            try:
                content_length = int(content_length_header)
                if content_length > MAX_UPLOAD_SIZE:
                    self.send_error(HTTPStatus.REQUEST_ENTITY_TOO_LARGE, "上传内容过大：总大小不能超过 5MB")
                    return
            except (ValueError, OverflowError):
                self.send_error(HTTPStatus.BAD_REQUEST, "无效的 Content-Length")
                return

        content_type = self.headers.get('Content-Type', '')
        if not content_type.startswith('multipart/form-data'):
            self.send_error(HTTPStatus.BAD_REQUEST, "请求类型必须为 multipart/form-data")
            return

        try:
            form = cgi.FieldStorage(
                fp=self.rfile,
                headers=self.headers,
                environ={'REQUEST_METHOD': 'POST', 'CONTENT_TYPE': content_type}
            )

            if 'file' not in form:
                self.send_error(HTTPStatus.BAD_REQUEST, "缺少 file 字段")
                return

            file_items = form['file']
            if not isinstance(file_items, list):
                file_items = [file_items]

            for item in file_items:
                if not item.filename:
                    continue

                filename = os.path.basename(item.filename)
                if not is_media_file(filename):
                    self.send_error(HTTPStatus.BAD_REQUEST, f"不支持的文件类型: {filename}")
                    return

                safe_path = safe_join(ROOT_DIR, filename)
                with open(safe_path, 'wb') as f:
                    f.write(item.file.read())

            self.send_response(HTTPStatus.OK)
            self.end_headers()
            self.wfile.write(b"OK")
        except Exception as e:
            self.send_error(HTTPStatus.INTERNAL_SERVER_ERROR, f"上传失败: {e}")

    def log_message(self, format, *args):
        pass

# ========================
# 启动 HTTP 服务器（非阻塞）
# ========================
def start_http_server():
    global _current_server
    with _server_lock:
        if _current_server is not None:
            print("🛑 关闭旧服务器...")
            _current_server.shutdown()
            _current_server.server_close()
            _current_server = None

        os.makedirs(ROOT_DIR, exist_ok=True)
        try:
            _current_server = HTTPServer(('0.0.0.0', PORT), MediaHandler)
            _current_server.allow_reuse_address = True
            thread = threading.Thread(target=_current_server.serve_forever, daemon=True)
            thread.start()
            current_ip = get_ip()
            print(f"✅ HTTP 服务器已启动: http://{current_ip}:{PORT}")
        except Exception as e:
            print(f"❌ 启动服务器失败: {e}")

# ========================
# IP 监控循环
# ========================
def ip_monitor_loop():
    global _current_ip
    while not _stop_event.is_set():
        try:
            new_ip = get_ip()
            if new_ip != _current_ip:
                print(f"📡 检测到 IP 变更: {_current_ip} → {new_ip}")
                _current_ip = new_ip
                start_http_server()
            _stop_event.wait(timeout=CHECK_INTERVAL)
        except Exception as e:
            print(f"⚠️ IP 检测异常: {e}")
            _stop_event.wait(timeout=CHECK_INTERVAL)

# ========================
# 主程序
# ========================
def main():
    global _current_ip
    print("🔧 初始化媒体服务器...")
    _current_ip = get_ip()
    start_http_server()

    print(f"⏱️  启动 IP 监控定时器（每 {CHECK_INTERVAL} 秒）...")
    monitor_thread = threading.Thread(target=ip_monitor_loop, daemon=True)
    monitor_thread.start()
    # zmqContext = zmq.Context()
    # pub_socket = zmqContext.socket(zmq.PUB)
    # pub_socket.bind("tcp://127.0.0.1:5555")  # 绑定本地端口
    try:
        # 主线程保持运行
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n🛑 正在关闭服务...")
        _stop_event.set()
        server_socket.close()

        with _server_lock:
            if _current_server:
                _current_server.shutdown()
                _current_server.server_close()
        print("👋 服务已停止")

if __name__ == '__main__':
    main()
