package com.example.eshop.ai;
import com.example.eshop.common.imports.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ImportSecurityTest {
    @Test void approvalProofBindsActorPayloadKeyAndSignature() {
        var proof = new ImportApprovalProof("test-key-with-at-least-thirty-two-characters", new ObjectMapper());
        var row = new ImportContracts.Row(ImportTool.INVENTORY_STOCK_ADJUST_BULK, Map.of("inventoryId","1","quantity","10"));
        String key = UUID.randomUUID()+":"+UUID.randomUUID(), signed = proof.issue(key,row,2,1);
        proof.verify(signed,key,row,2);
        assertThatThrownBy(() -> proof.verify(signed,key,row,1)).hasMessageContaining("approval");
        assertThatThrownBy(() -> proof.verify(signed,key+"x",row,2)).hasMessageContaining("approval");
        assertThatThrownBy(() -> proof.verify(signed,key,new ImportContracts.Row(row.tool(),Map.of("inventoryId","1","quantity","999")),2)).hasMessageContaining("approval");
        assertThatThrownBy(() -> proof.verify(signed.substring(1),key,row,2)).hasMessageContaining("approval");
        assertThatThrownBy(() -> new ImportApprovalProof("",new ObjectMapper()).issue(key,row,2,1)).hasMessageContaining("signing key");
    }
    @Test void schemasRejectArbitraryApiFieldsInferredValuesAndInvalidNumbers() {
        assertThat(ImportTool.CATEGORY_BULK_CREATE.validate(Map.of("name","New","status","true","api","http://attacker"))).contains("UNKNOWN_FIELD: api");
        assertThat(ImportTool.CATEGORY_BULK_CREATE.validate(Map.of("name","New"))).contains("REQUIRED: status");
        assertThat(ImportTool.INVENTORY_STOCK_ADJUST_BULK.validate(Map.of("inventoryId","1","quantity","-1"))).contains("INVALID_VALUE: quantity");
        assertThat(ImportTool.INVENTORY_STOCK_IN_BULK.validate(Map.of("inventoryId","1","quantity","0"))).contains("INVALID_VALUE: quantity");
        assertThat(ImportTool.BRANCH_BULK_CREATE.validate(Map.of("name",""))).contains("REQUIRED: name");
        assertThat(ImportTool.PRODUCT_BULK_CREATE.validate(Map.of("name","New","subCategoryId","1","isActive","true"))).contains("IMAGE_ATTACHMENT_REQUIRED");
    }
}
