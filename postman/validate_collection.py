"""Offline validation: controller coverage, Postman structure, variables and JSON bodies."""
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'postman'
collection=json.loads((OUT/'E-Shop-All.postman_collection.json').read_text(encoding='utf-8'))
manifest=json.loads((OUT/'endpoint-coverage.json').read_text(encoding='utf-8'))
requests=[r for module in collection['item'] for group in module['item'] for r in group['item']]
assert collection['info']['schema'].endswith('/v2.1.0/collection.json')
assert collection['auth']['type']=='noauth'
assert len({(r['module'],r['controller'],r['handler']) for r in manifest})==len(manifest)
for module in collection['item']:
    for path in (ROOT/module['name']/'src/main/java').rglob('*Controller.java'):
        source=path.read_text(encoding='utf-8-sig')
        source=re.sub(r'"(?:\\.|[^"\\])*"|/\*[\s\S]*?\*/|//[^\n]*',
                      lambda m:m[0] if m[0].startswith('"') else '',source)
        if '@RestController' not in source: continue
        source=source[source.index('public class'):]
        count=len(re.findall(r'@(Get|Post|Put|Patch|Delete|Request)Mapping\b',source))
        actual=sum(r['module']==module['name'] and r['controller']==path.stem for r in manifest)
        assert count==actual,(path,count,actual)

for env_path in OUT.glob('*.postman_environment.json'):
    env=json.loads(env_path.read_text(encoding='utf-8'))
    variables={v['key']:str(v['value']) for v in env['values']}
    assert len(variables)==len(env['values'])
    def resolve(value):
        names=re.findall(r'\{\{([^{}]+)\}\}',value)
        assert all(name in variables for name in names),names
        return re.sub(r'\{\{([^{}]+)\}\}',lambda m:variables[m[1]],value)
    for item in requests:
        request=item['request']
        assert request['method'] in ('GET','POST','PUT','PATCH','DELETE','OPTIONS')
        assert request['url']['raw'].startswith(('{{baseUrl}}/','{{catalogBaseUrl}}/'))
        resolved=resolve(request['url']['raw'])
        assert not re.search(r'[{}]',resolved),item['name']
        for header in request['header']: resolve(header['value'])
        for query in request['url'].get('query',[]): resolve(query['value'])
        body=request.get('body',{})
        if body.get('mode')=='raw': json.loads(resolve(body['raw']))
        elif body.get('mode')=='formdata':
            assert not any(h['key'].lower()=='content-type' for h in request['header'])
            for field in body['formdata']:
                assert field['type'] in ('text','file')
                if field['type']=='text':
                    value=resolve(field['value'])
                    if field['key']=='skus': json.loads(value)
    assert variables['password']=='' and variables['accessToken']=='' and variables['gatewayAdminKey']==''
print(f'PASS: {len(requests)} requests; {len(manifest)} controller mappings; both environments; all variables and JSON examples.')
