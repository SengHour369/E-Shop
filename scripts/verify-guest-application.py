#!/usr/bin/env python3
"""Boot production order-service classes with disposable Redis/H2 and exercise real HTTP security.
Run Maven order-service tests/build first. No production data, email or payment provider is used.
"""
import http.server
import json
import os
import pathlib
import re
import subprocess
import threading
import time
import urllib.error
import urllib.request
import uuid
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
NAME = 'eshop-guest-http-test-' + uuid.uuid4().hex[:8]
LOG = pathlib.Path('/tmp/guest-application-smoke.log')
java = pathlib.Path(os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-17-openjdk-amd64')) / 'bin/java'
report = ROOT / 'order-service/target/surefire-reports/TEST-com.example.eshop.order.guest.GuestCommerceIntegrationTest.xml'
properties = {p.attrib['name']: p.attrib.get('value', '') for p in ET.parse(report).getroot().find('properties')}
classpath = os.pathsep.join(p for p in properties['java.class.path'].split(os.pathsep) if '/test-classes' not in p)
flags = dict(guestBrowsing=True, guestReviews=False, guestCart=True, guestCheckout=False, guestPayment=False,
             requireDeliveryAddress=True, requireGuestContactVerification=False, guestOrderTracking=False)
class Settings(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != '/api/v1/storefront/access-settings':
            self.send_error(404)
            return
        payload = json.dumps(flags).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(payload)
    def log_message(self, *_): pass
settings = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Settings)
threading.Thread(target=settings.serve_forever, daemon=True).start()
process = None

def request(port, method, path, body=None, cookie=None, origin=None, csrf=None):
    headers = {'Content-Type': 'application/json'}
    if cookie: headers['Cookie'] = cookie
    if origin: headers['Origin'] = origin
    if csrf: headers['X-Guest-CSRF'] = csrf
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request('http://127.0.0.1:'+str(port)+path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            raw = response.read()
            return response.status, response.headers, json.loads(raw) if raw else None
    except urllib.error.HTTPError as response:
        return response.code, response.headers, None

try:
    subprocess.run(['docker', 'run', '--rm', '-d', '--name', NAME, '-p', '127.0.0.1::6379', 'redis:7-alpine'], check=True, stdout=subprocess.DEVNULL)
    port_data = json.loads(subprocess.check_output(['docker', 'inspect', NAME]))[0]['NetworkSettings']['Ports']['6379/tcp'][0]['HostPort']
    args = [str(java), '-cp', classpath, 'com.example.eshop.order.OrderServiceApplication',
          '--server.port=0', '--spring.datasource.url=jdbc:h2:mem:guest_http;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1',
          '--spring.datasource.driver-class-name=org.h2.Driver', '--spring.datasource.username=sa', '--spring.datasource.password=',
          '--spring.jpa.hibernate.ddl-auto=create-drop', '--spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect',
          '--spring.jpa.show-sql=false', '--eureka.client.enabled=false', '--spring.cloud.discovery.enabled=false', '--automation.enabled=false',
          '--guest.allowed-origins=https://shop.test', '--guest.cookie-secure=true', '--spring.data.redis.host=127.0.0.1',
          '--spring.data.redis.port='+port_data, '--storefront.admin-url=http://127.0.0.1:'+str(settings.server_port)]
    with LOG.open('w') as output:
        process = subprocess.Popen(args, cwd=ROOT/'order-service', stdout=output, stderr=subprocess.STDOUT)
        port = None
        for attempt in range(100):
            if process.poll() is not None: raise RuntimeError('Order-service startup failed; inspect '+str(LOG))
            match = re.search(r'Tomcat started on port (\d+)', LOG.read_text())
            if match:
                port = int(match.group(1))
                break
            time.sleep(.5)
        if not port: raise RuntimeError('Startup timeout; inspect '+str(LOG))
        status, headers, session = request(port, 'POST', '/api/v1/guest/session', {}, origin='https://shop.test')
        assert status == 200, (status, 'bootstrap')
        cookie = headers.get('Set-Cookie')
        assert all(flag in cookie for flag in ['HttpOnly', 'Secure', 'SameSite=Lax', 'Path=/api/v1/guest'])
        cookie = cookie.split(';', 1)[0]
        status, _, cart = request(port, 'GET', '/api/v1/guest/cart', cookie=cookie)
        assert status == 200 and cart['data']['total_items'] == 0, (status, 'guest cart')
        assert request(port, 'GET', '/api/v1/guest/cart')[0] == 401
        assert request(port, 'POST', '/api/v1/guest/session', {}, origin='https://attacker.test')[0] == 403
        assert request(port, 'DELETE', '/api/v1/guest/cart/items', cookie=cookie, origin='https://shop.test')[0] == 403
        assert request(port, 'DELETE', '/api/v1/guest/cart/items', cookie=cookie, origin='https://shop.test', csrf=session['csrfToken'])[0] == 200
        assert request(port, 'POST', '/api/v1/cart/user/id/get-or-create?userId=1', {})[0] == 401
        flags['guestCart'] = False
        assert request(port, 'GET', '/api/v1/guest/cart', cookie=cookie)[0] == 403
        print(json.dumps({'application': 'production order-service classes', 'startup': 'passed', 'repositories': 'discovered',
                          'secureCookie': 'passed', 'guestCart': 'passed', 'originCsrf': 'passed', 'privateCustomerApi': 'protected',
                          'liveFeatureDisable': 'passed', 'settings': 'local HTTP fixture', 'redis': 'real disposable instance'}))
finally:
    if process:
        process.terminate()
        try: process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
    settings.shutdown()
    subprocess.run(['docker', 'rm', '-f', NAME], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
