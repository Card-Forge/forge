import logging
import threading
import time
import json
import os
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(message)s')
logger = logging.getLogger(__name__)

# Global state
current_request = None
current_response = None
request_event = threading.Event()
response_event = threading.Event()

class ManualAgentHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == '/':
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            with open('templates/index.html', 'rb') as f:
                self.wfile.write(f.read())
        elif parsed.path == '/api/poll':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            
            data = {"waiting": False}
            if request_event.is_set():
                data = {
                    "waiting": True,
                    "data": current_request
                }
            self.wfile.write(json.dumps(data).encode('utf-8'))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        global current_request, current_response
        
        parsed = urlparse(self.path)
        # Handle both /decide and root path / for requests
        if parsed.path == '/decide' or parsed.path == '/':
            length = int(self.headers.get('content-length', 0))
            body = self.rfile.read(length)
            
            # If it's a health check or empty body, just return OK
            if length == 0 or body == b'{"healthCheck":true}':
                self.send_response(200)
                self.end_headers()
                return

            data = json.loads(body)
            
            logger.info(f"Received request from Forge: {data.get('requestType', 'unknown')}")
            
            # Store request and signal frontend
            current_request = data
            current_response = None
            response_event.clear()
            request_event.set()
            
            # Block and wait for user response
            logger.info("Waiting for user input...")
            response_event.wait()
            
            # Return user response
            logger.info("User input received, sending response to Forge")
            request_event.clear()
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps(current_response).encode('utf-8'))
            
        elif parsed.path == '/api/respond':
            length = int(self.headers.get('content-length', 0))
            body = self.rfile.read(length)
            data = json.loads(body)
            
            logger.info("Received response from user")
            current_response = data
            response_event.set()
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(b'{"status": "ok"}')
        else:
            self.send_response(404)
            self.end_headers()

class ThreadedHTTPServer(HTTPServer):
    """Handle requests in a separate thread."""
    def process_request(self, request, client_address):
        thread = threading.Thread(target=self.__new_request_thread, args=(request, client_address))
        thread.daemon = True
        thread.start()
    
    def __new_request_thread(self, request, client_address):
        try:
            self.finish_request(request, client_address)
            self.shutdown_request(request)
        except Exception:
            self.handle_error(request, client_address)
            self.shutdown_request(request)

if __name__ == '__main__':
    port = 5001
    print(f"Starting Manual Agent on port {port}...", flush=True)
    server = ThreadedHTTPServer(('0.0.0.0', port), ManualAgentHandler)
    server.serve_forever()
