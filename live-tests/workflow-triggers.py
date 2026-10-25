#!/usr/bin/env python3
import pathlib,re,ssl,urllib.request,urllib.error,json,concurrent.futures,time
import argparse,os,getpass,urllib.parse
parser=argparse.ArgumentParser(description='Verify durable scheduled and custom-event workflows over real HTTPS. Creates retained read-only workflow evidence.')
parser.add_argument('--base-url',default='https://localhost:8443')
parser.add_argument('--ca-file',help='Optional trusted local CA certificate; certificate verification remains enabled')
parser.add_argument('--username',default=os.environ.get('ESHOP_TEST_USERNAME','admin'))
parser.add_argument('--report',type=pathlib.Path,default=pathlib.Path('.local/test-reports/workflow-triggers-https.json'))
parser.add_argument('--restart-worker-container',help='Opt-in local durability test: restart this Docker worker container after scheduling a read')
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
import uuid,datetime
root='/api/ai/admin/workflows'
checks=[]
active=[]
def request(method,path,body=None):return call(method,path,body,token)[1]
def passed(name,**evidence):checks.append({'name':name,'status':'PASSED',**evidence});print('PASSED',name,flush=True)
def create(kind,config):
 definition={'name':'HTTPS durable '+kind+' '+str(int(time.time()*1000)),'description':'Real background read of an existing catalog product','module':'catalog-service','triggerType':kind,'trigger':{'validForDays':1,**config},'steps':[{'key':'read','name':'Read actual product','tool':'PRODUCT_GET','parameters':{'id':product}}]}
 validation=request('POST',root+'/validate',{'definition':definition});assert validation['valid'],validation
 draft=request('POST',root,{'definition':definition});version=request('POST',root+'/'+draft['id']+'/publish',{'revision':draft['revision']});registration=request('POST',root+'/versions/'+version['id']+'/activate-trigger',{})
 active.append(registration['id']);return draft,version,registration
def jobs(registration):return request('GET',root+'/triggers/'+registration['id']+'/jobs')['items']
def wait_run(registration,expected=1,timeout=45):
 deadline=time.monotonic()+timeout
 while time.monotonic()<deadline:
  try:records=jobs(registration)
  except urllib.error.HTTPError as unavailable:
   if args.restart_worker_container and unavailable.code in (502,503):
    time.sleep(1);continue
   raise
  if len(records)>=expected and records[0]['status'] not in ['QUEUED','RUNNING']:
   job=records[0];assert job['status']=='SUCCEEDED',job
   detail=request('GET',root+'/executions/'+job['executionId']);assert detail['run']['status']=='SUCCEEDED';assert len(detail['steps'][0]['outputSha256'])==64;return job,detail
  time.sleep(.75)
 raise AssertionError('Background execution did not finish')
try:
 products=request('POST','/api/v1/products/get/all',{'page':1,'size':1,'criteria_type':0,'criteria_value':''});product=products['data']['payload'][0]['id']
 catalog=request('GET',root+'/catalog');assert catalog['schedulingAvailable'] and catalog['eventExecutionAvailable'];passed('Actual automatic trigger capabilities')
 when=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=4)).isoformat()
 draft,version,scheduled=create('SCHEDULED',{'firstRunAt':when,'intervalMinutes':0});job,detail=wait_run(scheduled);assert request('GET',root+'/'+draft['id']+'/trigger')['state']=='COMPLETED';passed('Scheduled worker executed a real product read',workflowId=draft['id'],jobId=job['id'],runId=detail['run']['id'])
 time.sleep(2);assert len(jobs(scheduled))==1;passed('One-time schedule did not repeat')
 name='catalog.live.'+str(uuid.uuid4());draft,version,event=create('EVENT',{'eventType':'CUSTOM','eventName':name});key=str(uuid.uuid4());first=request('POST',root+'/signals',{'name':name,'idempotencyKey':key});job,detail=wait_run(event);passed('Custom event executed a real product read',workflowId=draft['id'],jobId=job['id'],runId=detail['run']['id'])
 second=request('POST',root+'/signals',{'name':name,'idempotencyKey':key});assert first['eventId']==second['eventId'];assert len(jobs(event))==1;passed('Duplicate event delivery did not repeat execution')
 request('POST',root+'/triggers/'+event['id']+'/pause',{});request('POST',root+'/signals',{'name':name,'idempotencyKey':str(uuid.uuid4())});time.sleep(2);assert len(jobs(event))==1;passed('Paused event subscription did not execute')
 event=request('POST',root+'/versions/'+version['id']+'/activate-trigger',{});request('POST',root+'/signals',{'name':name,'idempotencyKey':str(uuid.uuid4())});job,detail=wait_run(event,2);passed('Renewed trigger executed with fresh revocable authorization',runId=detail['run']['id'])
 if args.restart_worker_container:
  if parsed.hostname not in ('localhost','127.0.0.1','::1'):raise ValueError('Worker restart testing requires a local HTTPS origin')
  import subprocess
  when=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=10)).isoformat()
  draft,version,restarted=create('SCHEDULED',{'firstRunAt':when,'intervalMinutes':0})
  subprocess.run(['docker','restart',args.restart_worker_container],check=True,capture_output=True,timeout=60)
  job,detail=wait_run(restarted,timeout=150);assert len(jobs(restarted))==1
  passed('Scheduled authorization and job survived worker restart without an open page',workflowId=draft['id'],runId=detail['run']['id'])

except Exception as e:
 checks.append({'name':'Automatic workflow acceptance','status':'FAILED','error':type(e).__name__,'detail':str(e)[:700]});raise
finally:
 for id in active:
  try:request('POST',root+'/triggers/'+id+'/pause',{})
  except Exception:pass
 args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps({'base':base,'scope':'Real HTTPS, existing product reads, persistent scheduler and custom events. No commerce/payment events fabricated. Test workflows retained and paused.','checks':checks},indent=2)+'\n')
