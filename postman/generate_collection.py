"""Build Postman requests from this repository's Spring controller signatures and DTOs."""
import json
import re
from pathlib import Path
from collections import OrderedDict

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'postman'
MODULES = ['auth-service', 'catalog-service', 'order-service', 'payment-service', 'api-gateway']

def clean(text):
    return re.sub(r'"(?:\\.|[^"\\])*"|/\*[\s\S]*?\*/|//[^\n]*',
                  lambda m: m[0] if m[0].startswith('"') else '', text)

def balanced(text, start):
    depth, quoted, escaped = 0, False, False
    for i in range(start, len(text)):
        c = text[i]
        if quoted:
            if escaped: escaped = False
            elif c == '\\': escaped = True
            elif c == '"': quoted = False
        elif c == '"': quoted = True
        elif c == '(': depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0: return text[start + 1:i], i + 1
    raise ValueError('Unbalanced Java signature')

def split(text):
    parts, start, depth, quoted, escaped = [], 0, 0, False, False
    for i, c in enumerate(text):
        if quoted:
            if escaped: escaped = False
            elif c == '\\': escaped = True
            elif c == '"': quoted = False
        elif c == '"': quoted = True
        elif c in '(<{[': depth += 1
        elif c in ')>}]': depth -= 1
        elif c == ',' and depth == 0:
            parts.append(text[start:i].strip()); start = i + 1
    if text[start:].strip(): parts.append(text[start:].strip())
    return parts

def annotation(text, name):
    match = re.search(r'@' + name + r'\b', text)
    if not match: return None
    pos = match.end()
    while pos < len(text) and text[pos].isspace(): pos += 1
    return balanced(text, pos)[0] if pos < len(text) and text[pos] == '(' else ''

def unannotated(text):
    while (match := re.search(r'@[\w.]+', text)):
        end = match.end()
        while end < len(text) and text[end].isspace(): end += 1
        if end < len(text) and text[end] == '(': _, end = balanced(text, end)
        text = text[:match.start()] + ' ' + text[end:]
    return text

variables = OrderedDict(baseUrl='http://localhost:8080', origin='http://localhost:5173',
                        authMode='bearer', username='demo@example.test', password='',
                        email='demo@example.test', accessToken='', refreshToken='',
                        gatewayAdminKey='', verificationCode='', resetToken='',
                        qr='', md5='', newPassword='')
id_names = {'Product':'productId','Category':'categoryId','CategoryIcon':'iconId',
            'SubCategory':'subCategoryId','Inventory':'inventoryId','ProductAttribute':'attributeId',
            'ProductAttributeValue':'attributeValueId','User':'userId','Address':'addressId',
            'Group':'groupId','FunctionPermission':'funcId','UserPermission':'userPermissionId',
            'GroupPermission':'groupPermissionId','UserGroup':'userGroupId',
            'Order':'orderId','Payment':'paymentId','PaymentTransaction':'transactionId',
            'GatewayRoute':'routeId','PromotionAdmin':'promotionId','Promotion':'promotionId'}

def var(name, default='1'):
    variables.setdefault(name, default)
    return '{{' + name + '}}'

def scalar(name, typ='String', group=''):
    if '_' in name: name = re.sub(r'_([a-z])', lambda m:m[1].upper(), name)
    if name == 'id': return var(id_names.get(group, 'id'))
    if name in ('productSkuId', 'skuId'): return var('skuId')
    if name in ('customerId',): return var('userId')
    if name.endswith('Id') or name.endswith('Ids'):
        return var(name)
    if name in ('username','CriteriaValue'): return var('username', 'demo@example.test')
    if name.lower() == 'password': return var('password', '')
    if name == 'email': return var('email', 'demo@example.test')
    if name == 'refreshToken': return var('refreshToken', '')
    if name == 'token': return var('resetToken', '')
    if name == 'code': return var('verificationCode', '')
    if name == 'checkoutKey': return var('checkoutKey','checkout-example-1')
    if name == 'stackable': return False
    if name == 'endAt': return '2026-10-05T00:00:00'
    if name == 'startAt': return '2026-10-02T00:00:00'
    if name in ('qr','md5','newPassword'): return var(name, '')
    if name in ('criteriaType',): return 0
    if name == 'criteriaValue': return None
    if name == 'page': return 1
    if name == 'size': return 10
    if typ in ('Boolean','boolean'): return name != 'OperatorProductAttribute'
    if typ in ('Long','Integer','int','long','BigDecimal','Double','double','Float'):
        return {'quantity':2,'price':19.99,'unitPrice':19.99,'amount':19.99,
                'lowStockThreshold':5,'rateLimit':120,'rateLimitWindowSeconds':60}.get(name,1)
    if 'DateTime' in typ: return '2026-10-01T09:00:00'
    if 'Date' in typ: return '2026-10-01'
    examples = {'paymentMethod':'CASH_ON_DELIVERY','currency':'USD','returnType':'REFUND',
                'status':'PENDING','newStatus':'SUCCESS','changedBy':'SYSTEM','phone':'0123456789',
                'phoneNumber':'0123456789','fullName':'Demo Customer','country':'Cambodia',
                'city':'Phnom Penh','zipCode':'12000','addressLine1':'123 Main Street',
                'routeKey':'recommendation-service','uri':'lb://recommendation-service',
                'pathPattern':'/api/v1/recommendations/**','httpMethod':'GET','value':'Blue',
                'birthdate':'1995-01-01','description':'Example description','remark':'Reviewed by administrator',
                'reason':'Customer request','warehouseLocation':'Warehouse A','name':'Example name'}
    if name in examples: return examples[name]
    if name in ('orderNo','orderNumber','transactionNo','refundId','returnId'):
        return var(name, '')
    return 'Example ' + name

sources = {}
for module in MODULES:
    sources[module] = {p.stem:p for p in (ROOT/module/'src/main/java').rglob('*.java')}

def dto(typ, module, depth=0):
    typ = typ.strip()
    if typ == 'CheckoutRequest':
        return {'orderId':var('orderId'), 'userId':var('userId'), 'items':[{'productSkuId':var('skuId'),'quantity':2}]}
    if depth > 5: raise ValueError('Recursive DTO: ' + typ)
    if typ.startswith(('List<','Set<')): return [dto(typ[typ.index('<')+1:-1], module, depth+1)]
    path = sources[module].get(typ)
    if not path: raise ValueError('Missing DTO: ' + module + '/' + typ)
    text = clean(path.read_text(encoding='utf-8-sig'))
    record = re.search(r'\brecord\s+' + typ + r'\s*\(', text)
    if record:
        chunks = split(balanced(text, record.end()-1)[0])
    else:
        chunks = re.findall(r'((?:(?:@[\w.]+(?:\([^;]*?\))?)\s*)*(?:(?:private|public|protected)\s+)?[\w]+(?:<[^;]+?>)?\s+\w+\s*(?:=[^;]+)?);', text)
    result = {}
    for chunk in chunks:
        bare = unannotated(chunk).strip()
        match = re.fullmatch(r'(?:(?:private|public|protected)\s+)?([\w]+(?:<[^>]+>)?)\s+(\w+)(?:\s*=.*)?', bare, re.S)
        if not match: continue
        field_type, name = match.groups()
        alias = annotation(chunk, 'JsonProperty')
        key = re.search(r'"([^"]+)"', alias)[1] if alias else name
        if field_type.startswith(('List<','Set<')):
            inner = field_type[field_type.index('<')+1:-1]
            val = [dto(inner,module,depth+1)] if inner in sources[module] else [scalar(name.removesuffix('s'),inner)]
        elif field_type in sources[module] and ('Request' in field_type or 'DTO' in field_type):
            val = dto(field_type,module,depth+1)
        else: val = scalar(name,field_type)
        if field_type in sources[module]:
            enum_text=clean(sources[module][field_type].read_text(encoding='utf-8-sig'))
            enum_match=re.search(r'\benum\s+\w+\s*\{([^;}]+)[;}]',enum_text,re.S)
            if enum_match:
                choices=re.findall(r'\b[A-Z][A-Z_0-9]*\b',enum_match[1])
                if val not in choices: val='BAKONG' if 'BAKONG' in choices else choices[0]
        if name == 'productSkuId' and typ == 'ProductSkuRequest': val = None
        if name == 'id' and typ in ('ProductAttributeRequest','ProductAttributeValueRequest'): val = None
        if typ == 'ProductSkuRequest' and name == 'OperatorProductAttribute': val = True
        if typ == 'GroupRequest' and name == 'status': val = 'ACT'
        if typ == 'GetCancelationListRequest' and name not in ('page','size'): val = None
        if typ == 'PromotionRequest':
            if name == 'code': val = var('promotionCode','WEEKEND-2026')
            if name == 'discountValue': val = 20
            if name in ('maxDiscountAmount','minimumOrderAmount','usageLimit','usagePerCustomer'): val = None
        result[key] = val
    if not result: raise ValueError('Empty DTO: ' + str(path))
    return result

def json_body(value):
    return {'mode':'raw','raw':json.dumps(value,indent=2), 'options':{'raw':{'language':'json'}}}

def request_item(name, method, path, module, group, parameters='', source='', consumes=''):
    headers, query, form, body, notes = [], [], [], None, []
    for param in split(parameters):
        bare = unannotated(param).strip()
        match = re.search(r'([\w]+(?:<[^>]+>)?)\s+(\w+)\s*$', bare)
        if not match: continue
        typ, java_name = match.groups()
        if typ == 'Pageable':
            args = annotation(param,'PageableDefault') or ''
            size = re.search(r'size\s*=\s*(\d+)',args)
            sort = re.search(r'sort\s*=\s*"([^"]+)"',args)
            query.extend([{'key':'page','value':'0'},{'key':'size','value':size[1] if size else '10'}])
            if sort: query.append({'key':'sort','value':sort[1]+(',desc' if 'DESC' in args else ',asc')})
            notes.append('Spring Pageable uses a zero-based page query parameter.')
            continue
        body_args = annotation(param,'RequestBody')
        if body_args is not None:
            body = json_body(dto(typ,module)); continue
        for kind in ('PathVariable','RequestParam','RequestPart','RequestHeader'):
            args = annotation(param,kind)
            if args is None: continue
            named = re.search(r'(?:^\s*|(?:value|name)\s*=\s*)"([^"]+)"',args)
            key = named[1] if named else java_name
            default = re.search(r'defaultValue\s*=\s*"([^"]*)"',args)
            value = default[1] if default else scalar(java_name,typ,group)
            optional = 'required = false' in args or 'required=false' in args
            if kind == 'PathVariable':
                if '{'+key+'}' not in path:
                    notes.append('BACKEND ISSUE: @PathVariable '+key+' has no matching path placeholder; this endpoint needs a controller fix.')
                path = path.replace('{'+key+'}', str(value))
            elif kind == 'RequestHeader':
                if key == 'X-Gateway-Admin-Key': headers.append({'key':key,'value':var('gatewayAdminKey','')})
            elif 'MultipartFile' in typ:
                form.append({'key':key,'type':'file','src':[], 'disabled':optional,
                             'description':'Select a local file in Postman. Repeated keys support multiple files.'})
            else:
                field = {'key':key, 'value':str(value).lower() if isinstance(value,bool) else str(value or ''), 'disabled':optional and not default}
                if 'MULTIPART' in consumes:
                    field['type']='text'; form.append(field)
                else: query.append(field)
            break
    if form:
        body={'mode':'formdata','formdata':form}
        if group=='Product':
            for field in form:
                if field['key']=='skus':
                    sku=dto('ProductSkuRequest','catalog-service')
                    if method=='PUT':
                        sku['productSkuId']=var('skuId');sku['product_attributes']=[]
                    field['value']=json.dumps([sku]);field['disabled']=False
    if group=='Product' and method=='PUT' and body and body['mode']=='raw':
        payload=json.loads(body['raw'])
        for sku in payload.get('skus',[]):
            sku['productSkuId']=var('skuId');sku['product_attributes']=[]
        body=json_body(payload)
    if body and body['mode']=='raw': headers.append({'key':'Content-Type','value':'application/json'})
    if group=='User':
        for field in query:
            if field['key']=='status': field['value']='ACT'
    base = var('catalogBaseUrl','http://localhost:8082') if path.startswith('/internal/') else '{{baseUrl}}'
    if path.startswith('/internal/'):
        var('serviceToken','')
        notes.append('Internal service endpoint. Requires a short-lived SERVICE_ORDER token in serviceToken. Not exposed by the gateway.')
    raw = base+path
    enabled = [q for q in query if not q.get('disabled')]
    if enabled: raw+='?'+'&'.join(q['key']+'='+q['value'] for q in enabled)
    url = {'raw':raw,'host':[base],'path':path.lstrip('/').split('/')}
    if query: url['query']=query
    description = f'{module}: {source}\n\n'+'\n'.join(notes)
    req={'method':method,'header':headers,'url':url,'description':description.strip()}
    if body: req['body']=body
    return {'name':name,'request':req,'response':[]}

folders=OrderedDict((module,OrderedDict()) for module in MODULES)
manifest=[]
for module in MODULES:
    for path in sorted((ROOT/module/'src/main/java').rglob('*Controller.java')):
        text=clean(path.read_text(encoding='utf-8-sig'))
        if '@RestController' not in text: continue
        group=path.stem.removesuffix('Controller')
        head=text[:text.index('public class')]
        base_args=annotation(head,'RequestMapping') or ''
        base_match=re.search(r'"([^"]*)"',base_args)
        base=base_match[1] if base_match else ('/api/v1/public/session' if 'SESSION_PATH' in base_args else '')
        for mapping in re.finditer(r'@(Get|Post|Put|Patch|Delete|Request)Mapping\b',text[len(head):]):
            start=len(head)+mapping.start(); end=len(head)+mapping.end()
            args=''
            if text[end:end+1]=='(': args,end=balanced(text,end)
            declaration=re.search(r'\bpublic\s+[\w<>?,.\[\] ]+?\s+(\w+)\s*\(',text[end:])
            if not declaration: raise ValueError(str(path))
            method_name=declaration[1]
            params,_=balanced(text,end+declaration.end()-1)
            suffix_match=re.search(r'(?:^\s*|value\s*=\s*|path\s*=\s*)"([^"]*)"',args)
            suffix=suffix_match[1] if suffix_match else ''
            route='/'+(base+suffix).lstrip('/')
            method=mapping[1].upper() if mapping[1]!='Request' else 'GET'
            display=re.sub(r'(?<!^)([A-Z])',r' \1',method_name).capitalize()
            if 'MULTIPART' in args: display+=' (multipart)'
            elif 'APPLICATION_JSON' in args: display+=' (JSON)'
            src=str(path.relative_to(ROOT)).replace('\\','/')+' — '+method_name
            item=request_item(display,method,route,module,group,params,src,args)
            if group=='Fallback':
                item['request']['url']={'raw':'{{baseUrl}}/fallback/catalog','host':['{{baseUrl}}'],'path':['fallback','catalog']}
            folders[module].setdefault(group,[]).append(item)
            manifest.append({'module':module,'controller':path.stem,'handler':method_name,'method':method,'path':route,
                             'consumes':'multipart' if 'MULTIPART' in args else 'json' if 'APPLICATION_JSON' in args else '',
                             'request':display})

# Login implemented by a Spring Security filter rather than a controller.
legacy=request_item('Legacy security-filter login', 'POST','/api/v1/auth','auth-service','Login')
legacy['request']['body']=json_body(dto('Login','auth-service'))
legacy['request']['header']=[{'key':'Content-Type','value':'application/json'}]
legacy['request']['description']='Legacy filter login returns a JWT refresh token that the database-backed /public/refresh endpoint does not accept. Prefer the email/username login or session login.'
folders['auth-service']['Login'].append(legacy)

prerequest = """const mode = pm.environment.get('authMode') || 'bearer';
const url = pm.variables.replaceIn(pm.request.url.toString()).split('?')[0];
pm.request.headers.remove('Authorization');
pm.request.headers.upsert({key: 'Origin', value: pm.environment.get('origin') || 'http://localhost:5173'});
if (url.includes('/internal/catalog/')) {
  const token = pm.environment.get('serviceToken');
  if (token) pm.request.headers.upsert({key:'Authorization', value:'Bearer ' + token});
}
const publicPath = url.includes('/api/v1/public/') && !url.endsWith('/public/logout');
const adminPath = url.includes('/api/v1/gateway/routes');
if (mode === 'bearer' && url.includes('/api/v1/') && !publicPath && !adminPath && !url.endsWith('/api/v1/auth')) {
  const token = pm.environment.get('accessToken');
  if (token) pm.request.headers.upsert({key:'Authorization', value:'Bearer ' + token});
}"""
tests = """pm.test('HTTP request succeeded', () => pm.expect(pm.response.code).to.be.within(200, 299));
const url = pm.variables.replaceIn(pm.request.url.toString()).split('?')[0];
if (pm.response.code >= 200 && pm.response.code < 300) {
  let data; try { data = pm.response.json(); } catch (_) {}
  if (data && data.access_token && !url.includes('/session/')) {
    pm.environment.set('accessToken', data.access_token);
    if (data.refresh_token) pm.environment.set('refreshToken', data.refresh_token);
    pm.environment.set('authMode', 'bearer');
    if (data.id) pm.environment.set('userId', String(data.id));
  }
  if (url.includes('/session/') && !url.endsWith('/logout')) {
    pm.environment.set('authMode', 'cookie');
    if (data && data.id) pm.environment.set('userId', String(data.id));
    pm.test('Session tokens stay out of JSON', () => {
      pm.expect(data && data.access_token).to.not.be.ok;
      pm.expect(data && data.refresh_token).to.not.be.ok;
    });
  }
  if (url.endsWith('/logout')) {
    pm.environment.unset('accessToken'); pm.environment.unset('refreshToken');
  }
}"""

# Add focused product filter examples while retaining one request per controller mapping.
product_list=next(i for i in folders['catalog-service']['Product'] if i['name']=='Get products')
for name,kind,value in [('Active products',4,None),('Search products by name',1,'Example'),
                        ('Products by subcategory',2,var('subCategoryId')),
                        ('Products by category',3,var('categoryId')),
                        ('Product by ID',5,var('productId')),('Product with SKUs',6,var('productId'))]:
    item=json.loads(json.dumps(product_list));item['name']=name
    item['request']['body']=json_body({'criteria_type':kind,'criteria_value':value,'page':1,'size':10})
    folders['catalog-service']['Product'].append(item)

ops=[]
for name,path in [('Gateway health','/actuator/health'),('Gateway info','/actuator/info'),('Gateway OpenAPI','/v3/api-docs')]:
    ops.append(request_item(name,'GET',path,'api-gateway','Operations'))
for service in MODULES[:-1]: ops.append(request_item(service+' OpenAPI','GET','/openapi/'+service,'api-gateway','Operations'))
folders['api-gateway']['Operations']=ops
collection={'info':{'name':'E-Shop — All Microservices','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
                   'description':'Generated from current controllers and DTOs. Import one supplied environment. Start with Login or Session. See postman/README.md for cookie setup, IDs, files, lifecycle requests and known backend issues. Nothing in this collection has been sent automatically.'},
            'auth':{'type':'noauth'},
            'event':[{'listen':'prerequest','script':{'type':'text/javascript','exec':prerequest.splitlines()}},
                     {'listen':'test','script':{'type':'text/javascript','exec':tests.splitlines()}}],
            'item':[{'name':module,'item':[{'name':group,'item':items} for group,items in groups.items()]} for module,groups in folders.items()]}
# Fallback intentionally returns 503; override the normal success assertion for this diagnostic.
collection['event'][1]['script']['exec'][0]="pm.test('Expected HTTP status', () => { const fallback = pm.request.url.toString().includes('/fallback/'); if (fallback) pm.expect(pm.response.code).to.equal(503); else pm.expect(pm.response.code).to.be.within(200, 299); });"

OUT.mkdir(exist_ok=True)
def write(name,value): (OUT/name).write_text(json.dumps(value,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
write('E-Shop-All.postman_collection.json',collection)
for mode in ('bearer','cookie'):
    write('E-Shop-Local-'+mode.title()+'.postman_environment.json',{
        'name':'E-Shop Local — '+mode.title(), '_postman_variable_scope':'environment',
        'values':[{'key':key,'value':mode if key=='authMode' else value,'enabled':True,
                   'type':'secret' if key in ('password','accessToken','refreshToken','gatewayAdminKey','newPassword','resetToken','serviceToken') else 'default'}
                  for key,value in variables.items()]})
write('endpoint-coverage.json',manifest)
count=sum(len(items) for groups in folders.values() for items in groups.values())
print(f'Generated {count} requests covering {len(manifest)} controller mappings across {len(MODULES)} modules.')
