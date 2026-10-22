#!/usr/bin/env python3
"""Real authenticated workflow smoke test. Creates retained draft/version/run records.
No mocks, secret output, payments, stock mutations, notifications or arbitrary tool URLs.
"""
import argparse
import json
import os
import sys
import uuid
from urllib.error import HTTPError, URLError
from urllib.request import Request, build_opener, HTTPRedirectHandler


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', default='http://localhost:8080')
    parser.add_argument('--product-id', type=int, required=True)
    parser.add_argument('--query', required=True, help='Actual catalog search words')
    parser.add_argument('--report', default='workflow-live-report.json')
    args = parser.parse_args()
    token = os.environ.get('WORKFLOW_API_TOKEN', '')
    if not token or args.product_id < 1:
        parser.error('WORKFLOW_API_TOKEN and a positive real product ID are required')
    if not args.base.startswith(('http://localhost:', 'http://127.0.0.1:', 'https://')):
        parser.error('Use localhost HTTP or HTTPS; credentials must not cross insecure remote HTTP')
    opener = build_opener(NoRedirect())
    request_id = 'workflow-smoke-' + str(uuid.uuid4())
    report = {'requestId': request_id, 'steps': [], 'status': 'RUNNING'}

    def request(title, method, path, body=None):
        payload = None if body is None else json.dumps(body).encode()
        req = Request(args.base.rstrip('/') + '/api/ai/admin/workflows' + path,
                      data=payload, method=method,
                      headers={'Authorization': 'Bearer ' + token,
                               'Content-Type': 'application/json', 'X-Request-ID': request_id})
        try:
            with opener.open(req, timeout=300) as response:
                result = json.load(response)
                report['steps'].append({'title': title, 'httpStatus': response.status, 'status': 'PASSED'})
                print(f"{len(report['steps'])}. PASSED {title} (HTTP {response.status})")
                return result
        except HTTPError as error:
            # Deliberately never print response bodies, which may contain provider/identity information.
            report['steps'].append({'title': title, 'httpStatus': error.code, 'status': 'FAILED'})
            raise RuntimeError(f'{title}: HTTP {error.code}') from None
        except (URLError, TimeoutError):
            report['steps'].append({'title': title, 'status': 'UNKNOWN'})
            raise RuntimeError(f'{title}: service unavailable; outcome UNKNOWN') from None

    try:
        catalog = request('Load permitted registry', 'GET', '/catalog')
        allowed = {tool['code'] for tool in catalog['tools']}
        if not {'PRODUCT_GET', 'PRODUCT_SEARCH'} <= allowed:
            raise RuntimeError('Required registered catalog tools are unavailable for this actor')
        definition = {'name': 'Real catalog verification ' + str(uuid.uuid4())[:8],
                      'description': 'Read real product details and search the real catalog.',
                      'module': 'catalog-service', 'triggerType': 'MANUAL', 'steps': [
                          {'key': 'product', 'name': 'Read product', 'tool': 'PRODUCT_GET',
                           'parameters': {'id': args.product_id}},
                          {'key': 'search', 'name': 'Search catalog', 'tool': 'PRODUCT_SEARCH',
                           'parameters': {'query': args.query}}]}
        draft = request('Persist workflow draft', 'POST', '', {'definition': definition})
        report['workflowId'] = draft['id']
        validation = request('Validate without executing tools', 'POST', '/validate', {'definition': definition})
        if not validation.get('valid') or validation.get('executed') is not False:
            raise RuntimeError('Workflow validation did not pass in validation-only mode')
        version = request('Publish immutable read-only version', 'POST', '/' + draft['id'] + '/publish', {'revision': draft['revision']})
        report['versionId'] = version['id']
        key = str(uuid.uuid4())
        report['executionId'] = key
        result = request('Execute real registered catalog handlers', 'POST', '/versions/' + version['id'] + '/execute', {'idempotencyKey': key})
        report['execution'] = result
        if result['run']['status'] != 'SUCCEEDED' or any(step['status'] != 'SUCCEEDED' for step in result['steps']):
            raise RuntimeError('Real tool execution failed or is UNKNOWN; inspect retained step results')
        detail = request('Read persisted execution timeline', 'GET', '/executions/' + key)
        if len(detail['steps']) != 2 or any(not s.get('outputSha256') for s in detail['steps']):
            raise RuntimeError('Persisted response evidence is missing')
        duplicate = request('Verify idempotent duplicate request', 'POST', '/versions/' + version['id'] + '/execute', {'idempotencyKey': key})
        if duplicate['run']['finishedAt'] != result['run']['finishedAt'] or [s['id'] for s in duplicate['steps']] != [s['id'] for s in result['steps']]:
            raise RuntimeError('Duplicate request did not return the original execution')
        report['status'] = 'PASSED'
    except (RuntimeError, KeyError, ValueError) as error:
        report['status'] = 'FAILED'
        # All messages are locally controlled; never include HTTP bodies or credentials.
        print(f'FAILED: {error}', file=sys.stderr)
    finally:
        with open(args.report, 'w', encoding='utf-8') as target:
            json.dump(report, target, indent=2)
            target.write('\n')
    return 0 if report['status'] == 'PASSED' else 1


if __name__ == '__main__':
    sys.exit(main())
