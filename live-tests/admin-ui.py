#!/usr/bin/env python3
"""Run the actual admin components against real HTTPS, without mocked responses.
Creates retained workflow drafts/versions/runs; does not change commerce records.
Credentials are read privately or from the environment and are never saved.
"""
import argparse,getpass,json,os,pathlib,ssl,subprocess,urllib.request,urllib.error,urllib.parse
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url',default='https://localhost:8443')
parser.add_argument('--ca-file',type=pathlib.Path)
parser.add_argument('--username',default=os.environ.get('ESHOP_TEST_USERNAME','admin'))
parser.add_argument('--frontend',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[2]/'Order_Shop_Frontend')
parser.add_argument('--report',type=pathlib.Path,default=pathlib.Path('.local/test-reports/admin-ui-https.json'))
args=parser.parse_args()
base=args.base_url.rstrip('/')
parsed=urllib.parse.urlsplit(base)
if parsed.scheme!='https' or not parsed.hostname or parsed.username or parsed.password or parsed.path or parsed.query or parsed.fragment:
 parser.error('Use an HTTPS origin without credentials, path, query, or fragment')
ctx=ssl.create_default_context(cafile=args.ca_file)
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,newurl):return None
opener=urllib.request.build_opener(urllib.request.HTTPSHandler(context=ctx),NoRedirect())
def call(method,path,body=None,token=None):
 headers={'Content-Type':'application/json','Origin':base}
 if token:headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),method=method,headers=headers)
 with opener.open(req,timeout=30) as response:return response.status,json.load(response)
password=os.environ.get('ESHOP_TEST_PASSWORD') or getpass.getpass('E-Shop password: ')
try:
 _,login=call('POST','/api/v1/public/email/username/login',{'CriteriaValue':args.username,'Password':password})
except Exception:
 raise SystemExit('Sign-in failed. Check credentials, HTTPS trust, and service availability.') from None
finally:
 del password
token=login.get('access_token')
if not token:raise SystemExit('A bearer session is required. Complete MFA in the browser before testing that account.')
args.report.parent.mkdir(parents=True,exist_ok=True)
query={'page':1,'size':1,'criteria_type':0,'criteria_value':''}
def rows(data):
 if isinstance(data,list):return [unwrap(i) for i in data]
 if isinstance(data,dict):
  for k in ['data','object','payload','content','items']:
   if k in data:return rows(data[k])
 return []
def unwrap(v):
 while isinstance(v,dict) and ('data' in v or 'object' in v):v=v.get('data',v.get('object'))
 return v
def first(method,path,body=None):
 _,data=call(method,path,body,token);items=rows(data);return items[0] if items else None
paths=[]
unavailable=[]
for kind,method,path,body,patterns,key in [
 ('product','POST','/api/v1/products/get/all',query,['products/view/{}','products/edit/{}'],'id'),
 ('category','POST','/api/v1/categories/get/all?page=0&size=1',{},['categories/view/{}','categories/edit/{}'],'id'),
 ('brand','POST','/api/v1/subcategories/get/all',query,['brands/view/{}','brands/edit/{}'],'id'),
 ('order','POST','/api/v1/orders/get/all',query,['orders/{}'],'id'),
 ('payment','POST','/api/v1/payments/get/all',query,['payments/{}'],'order_id'),
 ('transaction','POST','/api/v1/payment-transactions/get/all',query,['transactions/{}'],'id'),
 ('user','POST','/api/v1/user/All',{},['customers/{}','users/{}','users/edit/{}'],'id'),
 ('group','GET','/api/v1/groups/get/all?page=0&size=1',None,['groups/{}','groups/edit/{}','customer-groups/{}','customer-groups/edit/{}'],'group_id'),
 ('inventory','POST','/api/v1/inventory/all/?page=0&size=1',{},['inventory/details?id={}','inventory/edit?id={}'],'id'),
 ('promotion','GET','/api/v1/admin/promotions?page=1&size=1',None,['promotions/{}'],'id'),
 ('refund','POST','/api/v1/refunds/list',query,['refunds/{}'],'refund_id'),
 ('return','POST','/api/v1/returns/returns',{'page':1,'size':1},['returns/{}'],'return_id')]:
 item=first(method,path,body)
 value=item.get(key) if item else None
 if value is not None:paths.extend(p.format(value) for p in patterns)
 else:
  unavailable.append(kind)
  print('No existing detail fixture for',kind,flush=True)
settings=os.environ.copy();settings['ESHOP_TEST_SESSION']=json.dumps(login);settings['ESHOP_TEST_TOKEN']=token;settings['ESHOP_TEST_BASE_URL']=base;settings['ESHOP_TEST_DETAIL_ROUTES']=json.dumps(paths);
if args.ca_file:settings['NODE_EXTRA_CA_CERTS']=str(args.ca_file.resolve())
print('Testing',len(paths),'additional real record routes',flush=True)
result=subprocess.run(['npm','test','--','--include=src/app/features/admin/admin-pages.live.spec.ts','--include=src/app/features/admin/ai-automation/workflows.live.spec.ts','--reporters=json','--output-file='+str(args.report.resolve())],cwd=args.frontend,env=settings)
if args.report.exists():
 report=json.loads(args.report.read_text())
 summary={'base':base,'success':report.get('success',False),'passedTests':report.get('numPassedTests'), 'failedTests':report.get('numFailedTests'),'detailRoutes':len(paths),'unavailableDetailFixtures':unavailable,'scope':'Actual Angular components and real HTTPS under jsdom. Does not certify browser cookies, browser certificate trust, financial transactions, or every mutation. Workflow tests create retained drafts, versions and read-only execution records.'}
 args.report.with_suffix('.summary.json').write_text(json.dumps(summary,indent=2)+'\n')
 print('Real HTTPS component tests:',summary['passedTests'],'passed,',summary['failedTests'],'failed',flush=True)
raise SystemExit(result.returncode)
