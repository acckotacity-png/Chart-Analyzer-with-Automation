#!/usr/bin/env python3
"""
Simple HTTP Server with PWA support to serve Chart Analyzer Pro web portal.
Can be used directly on Render.com or any server.
"""
import http.server
import os
import sys

PORT = int(os.environ.get("PORT", 8080))
DIRECTORY = os.path.join(os.path.dirname(os.path.abspath(__file__)), "public")

class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # Enable CORS and caching headers
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Cache-Control", "no-cache")
        super().end_headers()

if __name__ == "__main__":
    os.chdir(DIRECTORY)
    with http.server.ThreadingHTTPServer(("", PORT), Handler) as httpd:
        print(f"Server started on http://0.0.0.0:{PORT} serving {DIRECTORY}")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("Stopping server...")
            sys.exit(0)
