#!/usr/bin/env python3
import pathlib,re,ssl,urllib.request,urllib.error,json,concurrent.futures,time
import argparse,os,getpass,urllib.parse
parser=argparse.ArgumentParser(description='Read real admin API data over HTTPS. Does not certify UI actions or modify business records.')
parser.add_argument('--base-url',default='https://localhost:8443')
parser.add_argument('--ca-file',help='Optional trusted local CA certificate; certificate verification remains enabled')
parser.add_argument('--username',default=os.environ.get('ESHOP_TEST_USERNAME','admin'))
parser.add_argument('--report',type=pathlib.Path,default=pathlib.Path('admin-pages-report.json'))
args=parser.parse_args()
base=args.base_url.rstrip('/')
parsed=urllib.parse.urlsplit(base)
if parsed.scheme!='https' or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path:
 parser.error('Use an HTTPS origin without credentials, path, query, or fragment')
ctx=ssl.create_default_context(cafile=args.ca_file)
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,newurl):return None
opener=urllib.request.build_opener(urllib.request.HTTPSHandler(context=ctx),NoRedirect())
def call(method,path,body=None,token=None):
 headers={'Content-Type':'application/json','Origin':base}
 if token: headers['Authorization']='Bearer '+token
 req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),method=method,headers=headers)
 with opener.open(req,timeout=30) as r:return r.status,json.load(r)
token=os.environ.get('ESHOP_TEST_TOKEN')
if not token:
 password=os.environ.get('ESHOP_TEST_PASSWORD') or getpass.getpass('E-Shop password: ')
 try:
  _,login=call('POST','/api/v1/public/email/username/login',{'CriteriaValue':args.username,'Password':password})
 except Exception:
  raise SystemExit('Sign-in failed. Check credentials and HTTPS configuration.') from None
 finally:
  del password
 token=login.get('access_token')
 if not token:raise SystemExit('Bearer login is unavailable or requires MFA. Supply an authenticated ESHOP_TEST_TOKEN through your environment.')
query={'page':1,'size':10,'criteria_type':0,'criteria_value':''}
checks=[]
for name,path in [('Products','products/get/all'),('Brands','subcategories/get/all'),('Orders','orders/get/all'),('Payments','payments/get/all'),('Transactions','payment-transactions/get/all'),('Refunds','refunds/list'),('User groups','user-groups/get/all'),('Functions','functions/get/all'),('Group permissions','group-permissions/get/all'),('User permissions','user-permissions/get/all')]: checks.append((name,'POST','/api/v1/'+path,query))
for name,path in [('Customers','user/All'),('Returns','returns/returns'),('Cancellations','cancelations/list'),('Categories','categories/get/all?page=0&size=10'),('Inventory','inventory/search?page=0&size=10')]:checks.append((name,'POST','/api/v1/'+path,{} if name not in ('Returns','Cancellations') else {'page':1,'size':10}))
for name,path in [('Settings','admin/settings'),('Monitor','admin/monitor'),('Admin session','admin/session'),('Storefront settings','admin/storefront/access-settings'),('Order summary','orders/summary'),('Inventory summary','inventory/summary'),('Return summary','returns/summary'),('Refund summary','refunds/summary'),('Cancellation summary','cancelations/summary'),('Groups','groups/get/all?page=0&size=10'),('Attributes','attributes'),('Category icons','category-icons/get/all'),('Brand logos','brand-logos/get/all'),('Promotions','admin/promotions?page=1&size=10'),('Sales report','orders/reports/monthly?year=2026'),('Sales details','orders/reports/sales-details?year=2026'),('Payment report','payments/reports/monthly?year=2026'),('Payment breakdown','payments/reports/breakdown?year=2026'),('User editor options','user/editor-options')]:checks.append((name,'GET','/api/v1/'+path,None))
for service in ['auth','catalog','order','payment','admin','ai','notification']:checks.append(('Audit '+service,'GET','/api/admin/audit-logs/'+service+'?page=1&size=10',None))
for service in ['orders','payments']:checks.append(('Automation '+service,'GET','/api/v1/'+service+'/automation/events?page=0&size=10',None))
for name,path in [('Workflow catalog','/api/ai/admin/workflows/catalog'),('Workflow templates','/api/ai/admin/workflows/templates'),('Workflow executions','/api/ai/admin/workflows/executions'),('Workflow summary','/api/ai/admin/workflows/summary'),('Imports','/api/ai/imports'),('Approval requests','/api/ai/imports/requests')]:checks.append((name,'GET',path,None))
def check(entry):
 name,method,path,body=entry;start=time.monotonic()
 try:
  status,data=call(method,path,body,token);code=data.get('code') if isinstance(data,dict) else None;failure=str(code).isdigit() and int(code)>=400 or isinstance(data,dict) and data.get('success') is False
  result={'name':name,'method':method,'path':path,'httpStatus':status,'status':'FAILED' if failure else 'PASSED','durationMs':round((time.monotonic()-start)*1000)}
  if failure:result['applicationCode']=code
 except urllib.error.HTTPError as e:result={'name':name,'method':method,'path':path,'httpStatus':e.code,'status':'FAILED'}
 except Exception as e:result={'name':name,'method':method,'path':path,'status':'FAILED','error':type(e).__name__}
 print(result['status'],name,result.get('httpStatus','-'),flush=True);return result
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool: results=list(pool.map(check,checks))
args.report.parent.mkdir(parents=True,exist_ok=True)
args.report.write_text(json.dumps({'base':base,'scope':'Read endpoints only. No action or UI success is inferred.','checks':results},indent=2)+'\n')
print('Failures',sum(r['status']=='FAILED' for r in results),'of',len(results))

raise SystemExit(1 if any(r["status"]=="FAILED" for r in results) else 0)
