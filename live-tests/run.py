#!/usr/bin/env python3
"""Real HTTP integration checks through the existing admin-service test runner."""
import argparse
import getpass
import http.cookiejar
import json
import os
from pathlib import Path
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:4200')
    parser.add_argument('--origin', help='Trusted browser origin; defaults to base URL')
    parser.add_argument('--username', default=os.environ.get('ESHOP_TEST_USERNAME', 'admin'))
    parser.add_argument('--guest-session', action='store_true', help='Create and revoke a real isolated guest session')
    parser.add_argument('--run-id', type=uuid.UUID, help='Read an existing run instead of starting another')
    parser.add_argument('--timeout', type=int, default=300, help='Maximum polling time in seconds')
    parser.add_argument('--report', type=Path, default=Path('live-test-report.json'))
    args = parser.parse_args()
    parsed = urllib.parse.urlsplit(args.base_url)
    if parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path not in ('', '/'):
        parser.error('base-url must be an HTTP(S) origin without credentials, path, query or fragment')
    if not 10 <= args.timeout <= 1800:
        parser.error('timeout must be between 10 and 1800 seconds')
    base = args.base_url.rstrip('/')
    origin = args.origin or base
    cookies = http.cookiejar.CookieJar()
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, req, fp, code, msg, headers, newurl):
            return None
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies), NoRedirect())
    bearer = os.environ.get('ESHOP_TEST_TOKEN')

    def request(method, path, body=None):
        headers = {'Accept': 'application/json', 'Origin': origin}
        if bearer:
            headers['Authorization'] = 'Bearer ' + bearer
        raw = None
        if body is not None:
            headers['Content-Type'] = 'application/json'
            raw = json.dumps(body).encode()
        req = urllib.request.Request(base + path, data=raw, method=method, headers=headers)
        try:
            with opener.open(req, timeout=15) as response:
                content = response.read(1024 * 1024 + 1)
                if len(content) > 1024 * 1024:
                    raise RuntimeError('Response exceeds report size limit')
                return json.loads(content)
        except urllib.error.HTTPError as error:
            # Never print bodies containing login details, cookies, business payloads or tokens.
            raise RuntimeError(f'{method} {path}: HTTP {error.code}. Check service availability and live-test permissions.') from None

    if not bearer:
        password = os.environ.get('ESHOP_TEST_PASSWORD') or getpass.getpass('E-Shop password: ')
        session = request('POST', '/api/v1/public/session/login', {'CriteriaValue': args.username, 'Password': password})
        del password
        if session.get('token_type') == 'MFA':
            raise RuntimeError('MFA is required. Use the browser Admin Live Tests page to authenticate.')
        if session.get('token_type') != 'Cookie':
            raise RuntimeError('Expected an HTTP-only cookie session; no token has been saved.')
        del session
    path = '/api/v1/admin/live-tests/runs'
    report = request('GET', path + '/' + str(args.run_id)) if args.run_id else request('POST', path, {'guestSession': args.guest_session})
    run_id = str(uuid.UUID(report['id']))
    print('Real HTTP run:', run_id, flush=True)
    shown = set()
    deadline = time.monotonic() + args.timeout
    while True:
        for index, step in enumerate(report['steps'], 1):
            key = step['key']
            if key not in shown and step['status'] not in ('PENDING', 'RUNNING'):
                shown.add(key)
                print(f"{index:02d}. {step['status']:7s} {step['title']} — HTTP {step.get('httpStatus') or '-'}, {step['durationMs']} ms", flush=True)
                print('    ' + step['detail'], flush=True)
        if report['status'] != 'RUNNING':
            break
        if time.monotonic() > deadline:
            raise RuntimeError(f'Polling timed out. The server may still be running; resume with --run-id {run_id}.')
        time.sleep(2)
        report = request('GET', path + '/' + run_id)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode='w', encoding='utf-8', dir=args.report.parent, delete=False) as output:
        json.dump(report, output, indent=2)
        output.write('\n')
        temporary = output.name
    os.replace(temporary, args.report)
    print(f"Result: {report['status']}. Report: {args.report}")
    return {'PASSED': 0, 'FAILED': 1, 'BLOCKED': 2}.get(report['status'], 3)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, urllib.error.URLError, ValueError, KeyError) as error:
        print('Live test could not complete:', str(error), file=sys.stderr)
        sys.exit(3)
    except KeyboardInterrupt:
        print('Stopped waiting. The server run is preserved in Admin → Live Tests.', file=sys.stderr)
        sys.exit(130)
