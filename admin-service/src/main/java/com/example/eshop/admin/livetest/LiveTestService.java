package com.example.eshop.admin.livetest;

import com.example.eshop.admin.model.LiveTestRun;
import com.example.eshop.admin.repository.LiveTestRunRepository;
import com.example.eshop.common.audit.*;
import com.fasterxml.jackson.databind.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fixed real HTTP checks only. No arbitrary URLs, SQL, workflow execution or financial writes. */
@Service
public class LiveTestService {
 public record Step(String key,String title,String status,Integer httpStatus,long durationMs,String detail,String requestId) {}
 public record Report(UUID id,String status,Instant startedAt,Instant finishedAt,List<Step> steps) {}
 public record Summary(UUID id,String status,Instant startedAt,Instant finishedAt) {}
 private record Check(String key,String title,String method,String path,String body,boolean authenticated) {}
 private final LiveTestRunRepository runs;
 private final ObjectMapper json;
 private final AuditLogService audit;
 private final URI gateway;
 private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"live-http-tests");t.setDaemon(true);return t;});
 private final AtomicBoolean occupied=new AtomicBoolean();
 private static final String QUERY="{\"page\":1,\"size\":3,\"criteria_type\":0,\"criteria_value\":\"\"}";
 private static final List<Check> CHECKS=List.of(
  new Check("gateway","Gateway health","GET","/actuator/health",null,false),
  new Check("flags","Live storefront configuration","GET","/api/v1/storefront/access-settings",null,false),
  new Check("admin-denied","Anonymous admin access is rejected","GET","/api/v1/admin/settings",null,false),
  new Check("identity","Current account and effective permissions","GET","/api/v1/user/access",null,true),
  new Check("catalog","Real products and SKU structure","POST","/api/v1/products/get/all",QUERY,true),
  new Check("guest-browse","Anonymous browsing obeys configuration","GET","/api/v1/storefront/products?size=3",null,false),
  new Check("orders","Order service list","POST","/api/v1/orders/get/all",QUERY,true),
  new Check("inventory","Inventory service list","POST","/api/v1/inventory/all/?page=0&size=3","{}",true),
  new Check("payments","Payment service list","POST","/api/v1/payments/get/all",QUERY,true),
  new Check("khqr","Real KHQR availability","GET","/api/v1/payments/methods",null,true),
  new Check("notifications","Current account notification count","GET","/api/notifications/unread-count",null,true),
  new Check("ai-info","AI inference/provider information","GET","/api/ai/admin/info",null,true),
  new Check("ai-history","Real AI conversation history","GET","/api/ai/admin/conversations",null,true),
  new Check("import-tools","Authoritative Excel import fields","GET","/api/ai/imports/tools",null,true),
  new Check("my-requests","Current maker approval requests","GET","/api/ai/imports/requests?mine=true&page=0&size=3",null,true),
  new Check("workflows","Workflow engine capabilities (AUT-01–11)","GET","/api/ai/admin/workflows/catalog",null,true)
 );
 public LiveTestService(LiveTestRunRepository runs,ObjectMapper json,AuditLogService audit,
  @Value("${live-tests.gateway-url:http://api-gateway:8080}") String gateway) {
  this.runs=runs;this.json=json;this.audit=audit;this.gateway=URI.create(gateway);
  if(!Set.of("http","https").contains(this.gateway.getScheme())||this.gateway.getUserInfo()!=null||this.gateway.getHost()==null)
   throw new IllegalArgumentException("Invalid configured live test gateway");
 }
 @jakarta.annotation.PostConstruct void recoverInterruptedRuns(){
  for(var run:runs.findByStatus("RUNNING")){
   var steps=new ArrayList<>(report(run).steps());
   for(int i=0;i<steps.size();i++){var s=steps.get(i);if(Set.of("RUNNING","PENDING").contains(s.status))steps.set(i,new Step(s.key,s.title,"BLOCKED",null,0,"Service restarted before this step completed",null));}
   run.setStatus("FAILED");run.setFinishedAt(Instant.now());save(run,steps);
  }
 }
 @PreDestroy void close(){executor.shutdownNow();}
 @org.springframework.transaction.annotation.Transactional
 public Report start(long actor,String bearer,String origin,boolean guestSession) {
  if(!occupied.compareAndSet(false,true))throw new ResponseStatusException(HttpStatus.CONFLICT,"A live test is already running");
  try {
   var run=new LiveTestRun();run.setId(UUID.randomUUID());run.setActorId(actor);run.setStartedAt(Instant.now());run.setStatus("RUNNING");
   var steps=new ArrayList<Step>();for(var c:CHECKS)steps.add(step(c,"PENDING",null,0,"Waiting",null));
   if(guestSession)steps.add(new Step("guest-session","Guest cookie session and revocation","PENDING",null,0,"Waiting",null));
   save(run,steps);audit.record(AuditEvent.success(AuditAction.CREATE,"LIVE_TEST_RUN",run.getId(),null,Map.of("guestSession",guestSession)));
   org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
    @Override public void afterCommit(){executor.submit(()->execute(run,bearer,origin,steps,guestSession));}
    @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)occupied.set(false);}
   });return report(run);
  } catch(RuntimeException error){occupied.set(false);throw error;}
 }
 public List<Summary> history(long actor){return runs.findTop20ByActorIdOrderByStartedAtDesc(actor).stream().map(r->new Summary(r.getId(),r.getStatus(),r.getStartedAt(),r.getFinishedAt())).toList();}
 public Report get(UUID id,long actor){var r=runs.findById(id).filter(v->v.getActorId()==actor).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));return report(r);}
 private Report report(LiveTestRun run){try{return new Report(run.getId(),run.getStatus(),run.getStartedAt(),run.getFinishedAt(),Arrays.asList(json.readValue(run.getStepsJson(),Step[].class)));}catch(Exception e){throw new IllegalStateException("Stored test report cannot be read");}}
 private void save(LiveTestRun run,List<Step> steps){try{run.setStepsJson(json.writeValueAsString(steps));runs.saveAndFlush(run);}catch(Exception e){throw new IllegalStateException("Test progress cannot be saved");}}
 private static Step step(Check c,String status,Integer http,long ms,String detail,String request){return new Step(c.key,c.title,status,http,ms,detail,request);}
 private HttpClient client(CookieManager cookies){var b=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER);if(cookies!=null)b.cookieHandler(cookies);return b.build();}
 private record Response(int status,JsonNode body,String requestId) {}
 private Response call(HttpClient client,String method,String path,String body,String bearer,String origin) throws Exception {
  var b=HttpRequest.newBuilder(gateway.resolve(path)).timeout(Duration.ofSeconds(5)).header("Accept","application/json").header("X-Request-ID","live-test-"+UUID.randomUUID());
  if(bearer!=null)b.header("Authorization",bearer);
  if(origin!=null)b.header("Origin",origin);
  if(body!=null)b.header("Content-Type","application/json");
  b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
  var response=client.send(b.build(),HttpResponse.BodyHandlers.ofInputStream());
  byte[] bytes;try(var stream=response.body()){bytes=stream.readNBytes(131073);}
  if(bytes.length>131072)throw new IllegalStateException("Response exceeded test size limit");
  JsonNode value;try{value=json.readTree(bytes);}catch(Exception ignored){value=json.nullNode();}
  String requestId=response.headers().firstValue("X-Request-ID").orElse(null);
  if(requestId!=null&&!requestId.matches("[A-Za-z0-9._:-]{1,128}"))requestId=null;
  return new Response(response.statusCode(),value==null?json.nullNode():value,requestId);
 }
 private void execute(LiveTestRun run,String bearer,String origin,List<Step> steps,boolean guestSession) {
  try {
   var http=client(null);Boolean guestBrowsing=null;
   for(int i=0;i<CHECKS.size();i++){
    var c=CHECKS.get(i);steps.set(i,step(c,"RUNNING",null,0,"Requesting real service",null));save(run,steps);long start=System.nanoTime();Integer responseStatus=null;String responseRequest=null;
    try {
     var r=call(http,c.method,c.path,c.body,c.authenticated?bearer:null,null);responseStatus=r.status;responseRequest=r.requestId;String status="PASSED",detail="Real HTTP response validated";
     boolean success=r.status>=200&&r.status<300;
     if(c.key.equals("admin-denied")){status=r.status==401||r.status==403?"PASSED":"FAILED";detail="Expected unauthenticated access denial";}
     else if(c.key.equals("workflows")&&Set.of(404,405,501).contains(r.status)){status="BLOCKED";detail="Workflow API is not implemented/deployed; AUT-01–11 cannot be verified";}
     else if(c.key.equals("guest-browse")&&Boolean.FALSE.equals(guestBrowsing)&&r.status==403){status="BLOCKED";detail="Guest browsing is disabled by saved configuration";}
     else if(!success){status="FAILED";detail="Service returned HTTP "+r.status+"; no success assumed";}
     else if(r.body.path("success").isBoolean()&&!r.body.path("success").asBoolean()||r.body.path("code").asInt(0)>=400){status="FAILED";detail="API returned a business failure inside HTTP success";}
     else if(c.key.equals("flags")){if(!r.body.path("guestBrowsing").isBoolean())throw new IllegalStateException();guestBrowsing=r.body.path("guestBrowsing").asBoolean();detail="Saved feature flags received";}
     else if(c.key.equals("identity")){if(!r.body.path("permissions").isArray()||r.body.path("userId").asLong()<=0)throw new IllegalStateException();detail="Current account and live permission snapshot validated";}
     else if(c.key.equals("catalog")){var data=r.body.has("data")?r.body.path("data"):r.body;var list=data.path("payload");if(!list.isArray())throw new IllegalStateException();int skus=0;for(var p:list){if(!p.path("skus").isArray())throw new IllegalStateException();for(var sku:p.path("skus")){if(sku.path("id").asLong()<=0||sku.path("sku").asText().isBlank()||!sku.path("price").isNumber()||sku.path("price").asDouble()<=0)throw new IllegalStateException();}skus+=p.path("skus").size();}detail="Returned "+list.size()+" real products and "+skus+" SKUs; no sample response used";if(list.isEmpty()){status="BLOCKED";detail="Catalog is empty; product scenarios require existing products";}}
     else if(c.key.equals("import-tools")){if(!r.body.isArray()||r.body.isEmpty())throw new IllegalStateException();boolean product=false;for(var tool:r.body){if(!tool.path("name").isTextual()||!tool.path("schema").isObject()||!tool.path("permission").isTextual())throw new IllegalStateException();if("PRODUCT_BULK_CREATE".equals(tool.path("name").asText()))product=tool.path("schema").has("skus")&&tool.path("schema").has("subCategoryId");}if(!product)throw new IllegalStateException();detail="Live tool registry and backend product fields validated";}
     else if(c.key.equals("my-requests")){if(!r.body.path("items").isArray()||!r.body.path("total").isNumber()||!r.body.path("counts").isObject())throw new IllegalStateException();detail="Owner-scoped approval request contract validated; submission and independent review are not exercised";}
     else if(c.key.equals("workflows")){if(!r.body.path("tools").isArray()||!r.body.path("triggerTypes").isArray()||!r.body.path("executionAvailable").isBoolean())throw new IllegalStateException();if(!r.body.path("executionAvailable").asBoolean()){status="BLOCKED";detail=r.body.path("limitation").asText("Full workflow execution is unavailable");}else detail="Workflow capability contract received; end-to-end execution is not exercised";}
     else if(c.key.equals("khqr")){if(!r.body.isArray())throw new IllegalStateException();boolean enabled=false;for(var m:r.body)enabled|=Set.of("BAKONG","KHQR","ABA").contains(m.asText());status=enabled?"PASSED":"BLOCKED";detail=enabled?"Online method is configured; no transfer performed or verified":"Only offline payment is available; receiving account/configuration required";}
     else if(Set.of("orders","inventory","payments").contains(c.key)){var data=r.body.has("data")?r.body.path("data"):r.body;if(!data.isArray()&&!data.path("payload").isArray()&&!data.path("content").isArray())throw new IllegalStateException();detail="Real paginated service response validated (empty lists are allowed)";}
     else if(c.key.equals("gateway")){if(!"UP".equals(r.body.path("status").asText()))throw new IllegalStateException();}
     else if(c.key.equals("guest-browse")){if(!r.body.path("payload").isArray())throw new IllegalStateException();}
     else if(c.key.equals("notifications")){if(!r.body.path("count").isNumber()||r.body.path("count").asLong()<0)throw new IllegalStateException();}
     else if(c.key.equals("ai-info")){if(!r.body.path("provider").isTextual()||!r.body.path("model").isTextual())throw new IllegalStateException();detail="Real provider/model configuration received; inference accuracy is not tested";}
     else if(c.key.equals("ai-history")){if(!r.body.isArray())throw new IllegalStateException();}
     else if(r.body.isNull())throw new IllegalStateException();
     steps.set(i,step(c,status,r.status,(System.nanoTime()-start)/1000000,detail,r.requestId));
    }catch(Exception e){steps.set(i,step(c,"FAILED",responseStatus,(System.nanoTime()-start)/1000000,"Connection, timeout or response-contract failure; inspect service logs",responseRequest));}
    save(run,steps);
   }
   if(guestSession){int i=steps.size()-1;var before=steps.get(i);steps.set(i,new Step(before.key,before.title,"RUNNING",null,0,"Creating an isolated real guest session",null));save(run,steps);steps.set(i,guest(origin));save(run,steps);}
   run.setStatus(steps.stream().anyMatch(s->s.status.equals("FAILED"))?"FAILED":steps.stream().anyMatch(s->s.status.equals("BLOCKED"))?"BLOCKED":"PASSED");
  }catch(Exception e){run.setStatus("FAILED");for(int i=0;i<steps.size();i++){var s=steps.get(i);if(Set.of("PENDING","RUNNING").contains(s.status))steps.set(i,new Step(s.key,s.title,"FAILED",null,0,"Run interrupted; step not verified",null));}}
  finally{try{run.setFinishedAt(Instant.now());save(run,steps);}finally{occupied.set(false);}}
 }
 private Step guest(String origin){long start=System.nanoTime();String key="guest-session",title="Guest cookie session and revocation";
  if(origin==null)return new Step(key,title,"BLOCKED",null,0,"Browser Origin is required for the real guest test",null);
  var cookies=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);var http=client(cookies);
  try {
   var r=call(http,"POST","/api/v1/guest/session","{}",null,origin);
   if(r.status!=200||!r.body.path("csrfToken").isTextual())return new Step(key,title,"FAILED",r.status,0,"Guest bootstrap failed",r.requestId);
   var token=r.body.path("csrfToken").asText();if(!token.matches("[A-Za-z0-9_-]{43}"))throw new IllegalStateException();
   var request=HttpRequest.newBuilder(gateway.resolve("/api/v1/guest/session")).timeout(Duration.ofSeconds(5)).header("Origin",origin).header("X-Guest-CSRF",token).DELETE().build();
   var response=http.send(request,HttpResponse.BodyHandlers.discarding());
   return new Step(key,title,response.statusCode()==200||response.statusCode()==204?"PASSED":"FAILED",response.statusCode(),(System.nanoTime()-start)/1000000,"Real anonymous session created and revocation requested; no cart, order, email or payment created",r.requestId);
  }catch(Exception e){return new Step(key,title,"FAILED",null,(System.nanoTime()-start)/1000000,"Guest session request failed; no authenticated session used",null);}finally{cookies.getCookieStore().removeAll();}
 }
}
