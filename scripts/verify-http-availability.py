#!/usr/bin/env python3
"""Real unauthenticated HTTP inventory. Never invokes business write operations."""
import concurrent.futures
import collections
import datetime
import json
import pathlib
import re
import subprocess
import time
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
BASE = 'http://localhost:8080'
OUT = ROOT / 'reports/ai-automation/http-availability.json'


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def get(url, want_document=False):
    start = time.monotonic()
    row = {'method': 'GET', 'url': url, 'authenticated': False}
    document = None
    try:
        with urllib.request.build_opener(NoRedirect()).open(urllib.request.Request(url, headers={'Accept':'application/json'}), timeout=12) as response:
            row['httpStatus'] = response.status
            row['contentType'] = response.headers.get('Content-Type', '')
            raw = response.read(4 * 1024 * 1024 + 1)
            if len(raw) > 4 * 1024 * 1024:
                row['result'] = 'RESPONSE_TOO_LARGE'
            else:
                if 'json' not in row['contentType']:
                    row['result'] = 'EXPECTED_HTML' if url.endswith('/api/v1/public/reset-password-page') and 'text/html' in row['contentType'] else 'UNEXPECTED_CONTENT_TYPE'
                else:
                    try:
                        data = json.loads(raw)
                        row['result'] = 'RESPONDED'
                        if want_document:
                            if isinstance(data,dict) and isinstance(data.get('paths'),dict):
                                document=data
                                row['documentedPaths']=len(data['paths'])
                            else:
                                row['result']='INVALID_OPENAPI'
                    except (ValueError,TypeError):
                        row['result']='INVALID_JSON'
    except urllib.error.HTTPError as error:
        row['httpStatus']=error.code
        row['result']=('AUTH_REQUIRED' if error.code==401 else 'DENIED' if error.code==403 else
                       'NOT_FOUND' if error.code==404 else 'INPUT_REQUIRED' if error.code in (400,405,422) else
                       'RATE_LIMITED' if error.code==429 else 'SERVER_ERROR' if error.code>=500 else 'HTTP_ERROR')
    except (urllib.error.URLError,TimeoutError,OSError):
        row['httpStatus']=None
        row['result']='UNREACHABLE'
    row['durationMs']=round((time.monotonic()-start)*1000)
    return row,document


services={'auth-service':8081,'catalog-service':8082,'order-service':8083,'payment-service':8084,'admin-service':8087}
checks=[]
operations=[]
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
    docs=list(pool.map(lambda service:get(BASE+'/openapi/'+service,True),services))
for service,(check,document) in zip(services,docs):
    check['service']=service
    checks.append(check)
    if document is None:
        continue
    for path, methods in document['paths'].items():
        for method, operation in methods.items():
            if method not in {'get','post','put','patch','delete','head','options'}:
                continue
            row={'service':service,'method':method.upper(),'path':path}
            if not path.startswith('/api/'):
                row['result']='NOT_RUN_INTERNAL_ROUTE'
            elif method!='get':
                row['result']='NOT_RUN_WRITE_OR_OTHER_METHOD'
            elif '{' in path:
                row['result']='BLOCKED_RESOURCE_INPUT'
            elif re.search(r'(?i)(?:^|/)(?:delete|update|logout|refresh|send|retry|verify|qr|confirm|release|activate|disable|cancel|execute|start)(?:/|$)',path):
                row['result']='NOT_RUN_POTENTIAL_SIDE_EFFECT'
            else:
                required=[p['name'] for p in operation.get('parameters',[]) if p.get('required') and p.get('in')=='query']
                if required:
                    row['result']='BLOCKED_QUERY_INPUT'
                    row['requiredParameters']=required
                else:
                    row['result']='PENDING'
            operations.append(row)
    
static_urls=[BASE+op['path'] for op in operations if op['result']=='PENDING']
static_urls += [BASE+'/actuator/health',*[f'http://localhost:{port}/actuator/health' for port in [8081,8082,8083,8084,8085,8086,8087]],
    BASE+'/api/v1/storefront/products',BASE+'/api/v1/storefront/categories',BASE+'/api/v1/storefront/access-settings',
    BASE+'/api/ai/admin/workflows',BASE+'/api/ai/admin/workflows/catalog',BASE+'/api/ai/admin/workflows/executions',
    BASE+'/api/v1/admin/live-tests/runs','http://localhost:8085/api/ai/admin/workflows/catalog']
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
    results=list(pool.map(get,sorted(set(static_urls))))
checks.extend(row for row,_ in results)
# The AI service is private to Docker in this deployment. Probe the actual local container address.
try:
    addresses=subprocess.check_output(['docker','inspect','--format','{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}','e-shop-ai-service-1'],text=True,stderr=subprocess.DEVNULL).split()
    bindings=subprocess.check_output(['docker','port','e-shop-ai-service-1'],text=True,stderr=subprocess.DEVNULL).strip()
    if not bindings:
        for row in checks:
            if row['url'].startswith('http://localhost:8085/'):
                row['deploymentNote']='AI container has no published host port; test Docker-network address separately'
    for address in addresses:
        for path in ['/actuator/health','/api/ai/admin/workflows/catalog']:
            row,_=get('http://'+address+':8085'+path)
            row['surface']='docker-network'
            checks.append(row)
except (OSError,subprocess.CalledProcessError):
    pass

by_url={r['url']:r for r in checks}
for row in operations:
    if row['result']=='PENDING':
        check=by_url[BASE+row['path']]
        row.update(result=check['result'],httpStatus=check['httpStatus'])
report={'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'baseUrl':BASE,'authenticated':False,
        'scope':'Documented safe GET routes without guessed resource IDs or missing required parameters; protected routes only test authentication boundary. No business mutation calls.',
        'checks':checks,'documentedOperations':operations,
        'httpSummary':dict(collections.Counter(row['result'] for row in checks)),
        'operationSummary':dict(collections.Counter(row['result'] for row in operations))}
OUT.parent.mkdir(parents=True,exist_ok=True)
OUT.write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({'report':str(OUT),'httpChecks':len(checks),'documentedOperations':len(operations),'httpSummary':report['httpSummary'],'operationSummary':report['operationSummary']},indent=2))
for row in checks:
    if row['result'] in {'SERVER_ERROR','UNREACHABLE','UNEXPECTED_CONTENT_TYPE','INVALID_JSON','INVALID_OPENAPI'}:
        print(row['result'],row['httpStatus'],row['url'])
