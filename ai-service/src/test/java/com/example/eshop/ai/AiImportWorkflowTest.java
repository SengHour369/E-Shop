package com.example.eshop.ai;

import com.example.eshop.ai.client.*;
import com.example.eshop.ai.model.*;
import com.example.eshop.ai.repository.*;
import com.example.eshop.ai.service.*;
import com.example.eshop.common.imports.*;
import com.example.eshop.common.security.*;
import com.example.eshop.common.audit.AuditLogService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.slf4j.MDC;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:imports;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false", "spring.kafka.admin.auto-create=false",
    "ai.import.signing-key=test-key-with-at-least-thirty-two-characters", "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576" })
class AiImportWorkflowTest {
    @Autowired AiImportService service;
    @Autowired AiImportBatchRepository batches;
    @Autowired AiImportApprovalRepository approvals;
    @Autowired AiImportRowRepository rows;
    @Autowired NotificationOutboxRepository outbox;
    @MockitoBean LivePermissionService permissions;
    @MockitoBean ImportDomainClient domain;
    @MockitoBean AuthToolClient auth;
    @MockitoBean ImportMappingClient mappings;
    @MockitoBean NotificationOutboxPublisher publisher;
    @MockitoBean AuditLogService audit;
    final Set<String> grants = Set.of("IMPORT_UPLOAD", "IMPORT_REVIEW", "IMPORT_APPROVE", "IMPORT_EXECUTE", "CATEGORY_CREATE", "PRODUCT_CREATE", "BRANCH_CREATE", "SKU_CREATE", "INVENTORY_ADJUST", "PROMOTION_MANAGE");
    @BeforeEach void setup() {
        actor(1);
        when(permissions.current()).thenAnswer(c -> new PermissionSnapshot(CurrentActor.userId(), false, grants));
        when(auth.importReviewer(anyString(), anyLong())).thenReturn(true);
        when(domain.validateBulk(anyString(), anyList())).thenAnswer(c -> { List<ImportContracts.Row> data = c.getArgument(1); return data.stream().map(row -> new ImportContracts.Validation(List.of(), row.tool() + ":" + row.values().get("name"), null)).toList(); });
        when(domain.execute(anyString(), anyString(), anyString(), any())).thenReturn(new ImportContracts.Result("101"));
        when(mappings.suggest(any(), anyList())).thenReturn(Map.of());
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); RequestContextHolder.resetRequestAttributes(); MDC.clear(); }
    void actor(long id) {
        var auth = new UsernamePasswordAuthenticationToken("user-" + id, null, List.of()); auth.setDetails(Map.of("userId", id)); SecurityContextHolder.getContext().setAuthentication(auth);
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer test"); RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request)); MDC.put("requestId", UUID.randomUUID().toString());
    }
    AiImportBatch upload(String... names) {
        List<ExcelImportParser.ParsedRow> data = new ArrayList<>(); for (int i=0; i<names.length; i++) data.add(new ExcelImportParser.ParsedRow(i+2, Map.of("name",names[i],"status","true")));
        return service.upload(new ExcelImportParser.ParsedFile("test.xlsx", UUID.randomUUID().toString().getBytes(), List.of(new ExcelImportParser.ParsedSheet("Category", List.of("name","status"),data))));
    }
    AiImportBatch mapped(String... names) {
        var batch = upload(names); var sheet = service.preview(batch.getId(),0,50,null).sheets().get(0);
        return service.confirmMapping(batch.getId(),sheet.getId(),batch.getRevision(),ImportTool.CATEGORY_BULK_CREATE,Map.of("name","name","status","status"));
    }
    AiImportBatch approved(String... names) {
        var batch = mapped(names); batch = service.submit(batch.getId(),batch.getRevision(),2); actor(2); return service.decide(batch.getId(),batch.getRevision(),"APPROVED","Reviewed");
    }
    @Test void assistantDraftIsMappedOwnedAndNeverExecutedWithoutReview() {
        var result=service.stage(new ImportContracts.Row(ImportTool.CATEGORY_BULK_CREATE,Map.of("name","Assistant category","status","true")));
        var id=UUID.fromString(result.get("batchId").toString());
        var preview=service.preview(id,0,10,null);
        assertThat(batches.findById(id).orElseThrow().getOwnerId()).isEqualTo(1);
        assertThat(result.get("status")).isEqualTo("DRAFT");
        assertThat(preview.rows().getContent()).hasSize(1);
        assertThat(preview.rows().getContent().get(0).getValuesJson()).contains("Assistant category");
        verifyNoInteractions(domain);
        assertThatThrownBy(()->service.execute(id,(Integer)result.get("revision"))).hasMessageContaining("approved");
        actor(9);
        when(permissions.current()).thenReturn(new PermissionSnapshot(9,false,Set.of("IMPORT_UPLOAD")));
        assertThatThrownBy(()->service.preview(id,0,10,null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void assistantDraftRequiresSpecificDomainPermissionBeforePersistence() {
        long count=batches.count();
        when(permissions.current()).thenReturn(new PermissionSnapshot(1,false,Set.of("IMPORT_UPLOAD")));
        assertThatThrownBy(()->service.stage(new ImportContracts.Row(ImportTool.CATEGORY_BULK_CREATE,Map.of("name","Denied","status","true"))))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(batches.count()).isEqualTo(count);
    }

    @Test void addedRowsAreStagedVersionedAndCannotBeDuplicated() {
        var batch = mapped("Original"); var sheet = service.preview(batch.getId(),0,50,null).sheets().get(0);
        UUID rowId = UUID.randomUUID(); int previousVersion = batch.getRevision();
        var added = service.addRow(batch.getId(),sheet.getId(),previousVersion,rowId,Map.of("name","Added","status","true"));
        assertThat(added.row().getId()).isEqualTo(rowId);
        assertThat(added.row().getRowNumber()).isEqualTo(3);
        assertThat(added.row().getResourceId()).isNull();
        assertThat(added.row().getStatus()).isEqualTo("UNVALIDATED");
        assertThat(added.row().getOriginalValues()).isEqualTo("{}");
        assertThat(added.row().isEdited()).isTrue();
        assertThat(added.batch().getRevision()).isEqualTo(previousVersion+1);
        assertThat(service.preview(batch.getId(),0,50,null).summary().totalRows()).isEqualTo(2);
        assertThatThrownBy(() -> service.addRow(batch.getId(),sheet.getId(),previousVersion,UUID.randomUUID(),Map.of())).hasMessageContaining("Batch changed");
        assertThatThrownBy(() -> service.addRow(batch.getId(),sheet.getId(),added.batch().getRevision(),rowId,Map.of("name","Added"))).hasMessageContaining("already been saved");
        assertThat(rows.findByBatchId(batch.getId())).hasSize(2);
        service.confirmMapping(batch.getId(),sheet.getId(),added.batch().getRevision(),ImportTool.CATEGORY_BULK_CREATE,Map.of("name","name","status","status"));
        assertThat(rows.findById(rowId).orElseThrow().getValuesJson()).contains("Added");
        verify(domain,never()).execute(anyString(),anyString(),anyString(),any());
    }
    @Test void addingRowsInvalidatesApprovalAndRejectsUnconfirmedOrInjectedFields() {
        var batch = approved("Approved"); var sheet = service.preview(batch.getId(),0,50,null).sheets().get(0); actor(1);
        var added = service.addRow(batch.getId(),sheet.getId(),batch.getRevision(),UUID.randomUUID(),Map.of("name","Added","status","true"));
        assertThat(added.batch().getApprovedRevision()).isNull();
        assertThat(added.batch().getReviewerId()).isNull();
        assertThat(added.batch().getStatus()).isEqualTo("DRAFT");
        assertThatThrownBy(() -> service.addRow(batch.getId(),sheet.getId(),added.batch().getRevision(),UUID.randomUUID(),Map.of("imageIds","injected"))).hasMessageContaining("Invalid row fields");
        var unconfirmed = upload("Raw"); var rawSheet = service.preview(unconfirmed.getId(),0,50,null).sheets().get(0);
        assertThatThrownBy(() -> service.addRow(unconfirmed.getId(),rawSheet.getId(),unconfirmed.getRevision(),UUID.randomUUID(),Map.of("name","Added"))).hasMessageContaining("Confirm the worksheet mapping");
        assertThatThrownBy(() -> service.addRow(batch.getId(),rawSheet.getId(),added.batch().getRevision(),UUID.randomUUID(),Map.of())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
    @Test void addingRowsRequiresDomainPermission() {
        var batch = mapped("Original"); var sheet = service.preview(batch.getId(),0,50,null).sheets().get(0);
        when(permissions.current()).thenReturn(new PermissionSnapshot(1,false,Set.of("IMPORT_UPLOAD")));
        assertThatThrownBy(() -> service.addRow(batch.getId(),sheet.getId(),batch.getRevision(),UUID.randomUUID(),Map.of("name","Denied"))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(rows.findByBatchId(batch.getId())).hasSize(1);
    }


    @Test void requiresMappingConfirmationAndRejectsNonDomainTools() {
        var batch = upload("New"); assertThat(service.validate(batch.getId(),1).getStatus()).isEqualTo("INVALID");
        verifyNoInteractions(domain);
        var sheet = service.preview(batch.getId(),0,50,null).sheets().get(0);
        assertThatThrownBy(() -> service.confirmMapping(batch.getId(),sheet.getId(),1,ImportTool.IMPORT_VALIDATE,Map.of("name","name"))).hasMessageContaining("Unavailable");
    }
    @Test void flagsBothDuplicateRowsAndDomainErrors() {
        var batch = mapped("Same","Same"); assertThat(service.validate(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("INVALID");
        assertThat(rows.findByBatchId(batch.getId())).allMatch(r -> r.getStatus().equals("DUPLICATE"));
        doReturn(List.of(new ImportContracts.Validation(List.of("DUPLICATE_SKU","UNKNOWN_BRANCH","MISSING_CATEGORY"),null,null))).when(domain).validateBulk(anyString(), anyList());
        var another = mapped("Other"); assertThat(service.validate(another.getId(),another.getRevision()).getStatus()).isEqualTo("INVALID");
    }
    @Test void previewSummaryCoversWholeBatchRegardlessOfPageOrFilterAndSeparatesEditedRows() {
        var batch = mapped("Same", "Same", "Correctable", "Skipped");
        var all = rows.findByBatchId(batch.getId());
        var corrected = all.stream().filter(r -> r.getOriginalValues().contains("Correctable")).findFirst().orElseThrow();
        batch = service.edit(batch.getId(), corrected.getId(), batch.getRevision(), Map.of("name", "Corrected", "status", "true"));
        var skipped = all.stream().filter(r -> r.getOriginalValues().contains("Skipped")).findFirst().orElseThrow();
        batch = service.skip(batch.getId(), skipped.getId(), batch.getRevision(), true);
        service.validate(batch.getId(), batch.getRevision());
        var filtered = service.preview(batch.getId(), 1, 1, "DUPLICATE");
        assertThat(filtered.rows().getContent()).hasSize(1);
        assertThat(filtered.rows().getTotalElements()).isEqualTo(2);
        assertThat(filtered.summary().totalRows()).isEqualTo(4);
        assertThat(filtered.summary().editedRows()).isEqualTo(1);
        assertThat(filtered.summary().statuses()).containsExactlyInAnyOrderEntriesOf(Map.of("DUPLICATE", 2L, "VALID", 1L, "SKIPPED", 1L));
        assertThat(service.preview(batch.getId(), 0, 1, "FAILED").summary()).isEqualTo(filtered.summary());
        when(permissions.current()).thenReturn(new PermissionSnapshot(9, false, Set.of("IMPORT_UPLOAD")));
        actor(9);
        var id = batch.getId();
        assertThatThrownBy(() -> service.preview(id, 0, 1, null)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void deniesUnauthorizedAndSelfApprovalAndStaleVersion() {
        var batch = mapped("New");
        assertThatThrownBy(() -> service.submit(batch.getId(),batch.getRevision(),1)).hasMessageContaining("independent");
        service.submit(batch.getId(),batch.getRevision(),2);
        assertThatThrownBy(() -> service.decide(batch.getId(),batch.getRevision(),"APPROVED","Self")).hasMessageContaining("reviewer");
        actor(2); when(permissions.current()).thenReturn(new PermissionSnapshot(2,false,Set.of("IMPORT_REVIEW")));
        assertThatThrownBy(() -> service.decide(batch.getId(),batch.getRevision(),"APPROVED","Denied")).hasMessageContaining("permission");
        assertThatThrownBy(() -> service.validate(batch.getId(),99)).hasMessageContaining("Batch changed");
    }
    @Test void editingApprovedDataInvalidatesApprovalAndPreservesOriginal() {
        var batch = approved("Before"); var row = rows.findByBatchId(batch.getId()).get(0); actor(1);
        var changed = service.edit(batch.getId(),row.getId(),batch.getRevision(),Map.of("name","After","status","true"));
        assertThat(changed.getApprovedRevision()).isNull(); assertThat(changed.getRevision()).isEqualTo(batch.getRevision()+1);
        assertThat(rows.findById(row.getId()).orElseThrow().getOriginalValues()).contains("Before");
        actor(2); assertThatThrownBy(() -> service.execute(batch.getId(),changed.getRevision())).hasMessageContaining("approved");
    }
    @Test void executesOnlyConfirmedRowsAndRetriesWithSameIdempotencyKey() {
        var batch = approved("One","Two");
        when(domain.execute(anyString(), anyString(), anyString(), any())).thenReturn(new ImportContracts.Result("101")).thenThrow(new RuntimeException("timeout"));
        assertThat(service.execute(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("PARTIAL_SUCCESS");
        var failed = rows.findByBatchId(batch.getId()).stream().filter(r -> r.getStatus().equals("FAILED")).findFirst().orElseThrow();
        assertThat(failed.getResourceId()).isNull();
        doReturn(new ImportContracts.Result("102")).when(domain).execute(anyString(),anyString(),anyString(),any());
        assertThat(service.execute(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("COMPLETED");
        verify(domain,times(2)).execute(anyString(),eq(batch.getId()+":"+failed.getId()),anyString(),any());
        service.execute(batch.getId(),batch.getRevision()); verify(domain,times(3)).execute(anyString(),anyString(),anyString(),any());
        assertThat(outbox.findAll()).anyMatch(n -> n.getPayload().contains("IMPORT_COMPLETED"));
        assertThat(service.history(batch.getId()).get("revisions")).isNotNull();
    }
    @Test void concurrentApprovalsProduceOneDecision() throws Exception {
        var batch = mapped("Concurrent"); service.submit(batch.getId(),batch.getRevision(),2);
        actor(2); assertThat(service.list(0,100,true).getContent()).anyMatch(b -> b.getId().equals(batch.getId()));
        actor(3); assertThat(service.list(0,100,true).getContent()).noneMatch(b -> b.getId().equals(batch.getId()));
        var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> task = () -> { actor(2); start.await(); try { service.decide(batch.getId(),batch.getRevision(),"APPROVED","Concurrent"); return true; } catch (org.springframework.web.server.ResponseStatusException ex) { return false; } finally { clear(); } };
            var first = pool.submit(task); var second = pool.submit(task); start.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(approvals.findByBatchId(batch.getId())).hasSize(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void skippingInvalidRowsIsVersionedAndExcludedFromExecution() {
        var batch = mapped("Included","Skipped"); var row = rows.findByBatchId(batch.getId()).get(0);
        batch = service.skip(batch.getId(),row.getId(),batch.getRevision(),true);
        batch = service.submit(batch.getId(),batch.getRevision(),2); actor(2);
        batch = service.decide(batch.getId(),batch.getRevision(),"APPROVED","Reviewed skipped row");
        assertThat(service.execute(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("COMPLETED");
        verify(domain,times(1)).execute(anyString(),anyString(),anyString(),any());
        assertThat(rows.findById(row.getId()).orElseThrow().getStatus()).isEqualTo("SKIPPED");
    }
    @Test void rejectsIdenticalWorkbookChecksum() {
        var parsed = new ExcelImportParser.ParsedFile("same.xlsx",UUID.randomUUID().toString().getBytes(),List.of(new ExcelImportParser.ParsedSheet("Category",List.of("name"),List.of(new ExcelImportParser.ParsedRow(2,Map.of("name","New"))))));
        service.upload(parsed);
        assertThatThrownBy(() -> service.upload(parsed)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void largeValidationUsesBoundedDomainRequests() {
        String[] names = java.util.stream.IntStream.range(0,501).mapToObj(i -> "Category " + i).toArray(String[]::new);
        var batch = mapped(names);
        assertThat(service.validate(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("VALIDATED");
        verify(domain,times(2)).validateBulk(anyString(),anyList());
        verify(domain).validateBulk(anyString(),argThat(data -> data.size() == 500));
        verify(domain).validateBulk(anyString(),argThat(data -> data.size() == 1));
    }
    @Test void adminCanBrowseAllBatchesWhileApprovalRemainsIndependent() {
        var batch = mapped("Admin-visible"); service.submit(batch.getId(),batch.getRevision(),2); actor(3);
        when(permissions.current()).thenAnswer(c -> new PermissionSnapshot(CurrentActor.userId(),true,grants));
        assertThat(service.list(0,100,false).getContent()).anyMatch(b -> b.getId().equals(batch.getId()));
        assertThat(service.list(0,100,true).getContent()).anyMatch(b -> b.getId().equals(batch.getId()));
        assertThatThrownBy(() -> service.decide(batch.getId(),batch.getRevision(),"APPROVED","Other assigned reviewer")).hasMessageContaining("Assigned reviewer");
    }
    @Test void reviewerHistoryRetainsApprovedAssignmentsWithoutGrantingOtherUsersBatches() {
        var batch = approved("Reviewer history");
        when(permissions.current()).thenAnswer(c -> new PermissionSnapshot(CurrentActor.userId(), false, Set.of("IMPORT_REVIEW")));
        assertThat(service.list(0, 100, false, true).getContent()).anyMatch(b -> b.getId().equals(batch.getId()));
        assertThat(service.list(0, 100, true, false).getContent()).noneMatch(b -> b.getId().equals(batch.getId()));
        actor(3);
        assertThat(service.list(0, 100, false, true).getContent()).noneMatch(b -> b.getId().equals(batch.getId()));
        when(permissions.current()).thenReturn(new PermissionSnapshot(3, false, Set.of("IMPORT_UPLOAD")));
        assertThatThrownBy(() -> service.list(0, 100, false, true)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    AiImportBatch productBatch(){
        var batch=service.upload(new ExcelImportParser.ParsedFile("products.xlsx",UUID.randomUUID().toString().getBytes(),List.of(new ExcelImportParser.ParsedSheet("Product",List.of("name","subCategoryId","isActive"),List.of(new ExcelImportParser.ParsedRow(2,Map.of("name","Phone","subCategoryId","1","isActive","true")))))));
        var sheet=service.preview(batch.getId(),0,50,null).sheets().get(0);
        return service.confirmMapping(batch.getId(),sheet.getId(),batch.getRevision(),ImportTool.PRODUCT_BULK_CREATE,Map.of("name","name","subCategoryId","subCategoryId","isActive","isActive"));
    }
    org.springframework.mock.web.MockMultipartFile photo()throws Exception{
        var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        return new org.springframework.mock.web.MockMultipartFile("file","product.png","image/png",out.toByteArray());
    }
    @Test void productRequiresRowOwnedImageAndChangesInvalidateApproval()throws Exception{
        var batch=productBatch();var row=rows.findByBatchId(batch.getId()).get(0);
        assertThat(service.validate(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("INVALID");
        batch=service.attachImage(batch.getId(),row.getId(),batch.getRevision(),photo());
        var preview=service.preview(batch.getId(),0,50,null);assertThat(preview.images()).hasSize(1);assertThat(preview.images().get(0).getChecksum()).hasSize(64);
        service.submit(batch.getId(),batch.getRevision(),2);actor(2);batch=service.decide(batch.getId(),batch.getRevision(),"APPROVED","Reviewed image");
        actor(1);batch=service.detachImage(batch.getId(),row.getId(),preview.images().get(0).getId(),batch.getRevision());
        assertThat(batch.getApprovedRevision()).isNull();assertThat(batch.getStatus()).isEqualTo("DRAFT");
        assertThat(service.image(batch.getId(),preview.images().get(0).getId()).getContent()).isNotEmpty();
    }
    @Test void imageReferencesCannotCrossRowsOrBatches()throws Exception{
        var first=productBatch();var firstRow=rows.findByBatchId(first.getId()).get(0);first=service.attachImage(first.getId(),firstRow.getId(),first.getRevision(),photo());
        var original=rows.findById(firstRow.getId()).orElseThrow();var values=new com.fasterxml.jackson.databind.ObjectMapper().readValue(original.getValuesJson(),new com.fasterxml.jackson.core.type.TypeReference<Map<String,String>>(){});
        var second=productBatch();var secondRow=rows.findByBatchId(second.getId()).get(0);final var another=second;assertThatThrownBy(()->service.edit(another.getId(),secondRow.getId(),another.getRevision(),values)).hasMessageContaining("managed by the attachment workflow");
        assertThat(service.validate(second.getId(),second.getRevision()).getStatus()).isEqualTo("INVALID");
        final var target=second;var foreign=service.preview(first.getId(),0,50,null).images().get(0).getId();assertThatThrownBy(()->service.image(target.getId(),foreign)).hasMessageContaining("Import not found");
    }
    @Test void productExecutionUsesApprovedMultipartImagesAndConfirmedId()throws Exception{
        var batch=productBatch();var row=rows.findByBatchId(batch.getId()).get(0);batch=service.attachImage(batch.getId(),row.getId(),batch.getRevision(),photo());
        service.submit(batch.getId(),batch.getRevision(),2);actor(2);batch=service.decide(batch.getId(),batch.getRevision(),"APPROVED","Checked images and values");
        when(domain.executeProduct(anyString(),anyString(),anyString(),anyString(),anyList())).thenReturn(new ImportContracts.Result("900"));
        assertThat(service.execute(batch.getId(),batch.getRevision()).getStatus()).isEqualTo("COMPLETED");
        assertThat(rows.findById(row.getId()).orElseThrow().getResourceId()).isEqualTo("900");verify(domain).executeProduct(anyString(),anyString(),anyString(),contains("imageChecksums"),argThat(files->files.size()==1&&files.get(0).getContentType().equals("image/png")));
    }
    @Test void spreadsheetCannotSupplyManagedImageReferences() {
        var batch=service.upload(new ExcelImportParser.ParsedFile("unsafe-products.xlsx",UUID.randomUUID().toString().getBytes(),List.of(new ExcelImportParser.ParsedSheet("Product",List.of("name","subCategoryId","isActive","Images"),List.of(new ExcelImportParser.ParsedRow(2,Map.of("name","Phone","subCategoryId","1","isActive","true","Images","foreign-image")))))));
        var sheet=service.preview(batch.getId(),0,50,null).sheets().get(0);
        assertThatThrownBy(()->service.confirmMapping(batch.getId(),sheet.getId(),batch.getRevision(),ImportTool.PRODUCT_BULK_CREATE,Map.of("name","name","subCategoryId","subCategoryId","isActive","isActive","Images","imageIds")))
                .hasMessageContaining("managed by the attachment workflow");
    }
}

