package com.example.eshop.ai;

import com.example.eshop.ai.registry.AiDraftArguments;
import com.example.eshop.ai.service.AiFailure;
import com.example.eshop.common.imports.ImportTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class AiDraftArgumentsTest {
    final ObjectMapper mapper=new ObjectMapper();
    com.example.eshop.common.imports.ImportContracts.Row parse(String tool,String values){
        return AiDraftArguments.parse(mapper.valueToTree(Map.of("toolType",tool,"valuesJson",values)));
    }
    @Test void validatesActualImportSchemaAndPreservesKhmer(){
        var row=parse("CATEGORY_BULK_CREATE","{\"name\":\"ទូរស័ព្ទ\",\"status\":true}");
        assertThat(row.tool()).isEqualTo(ImportTool.CATEGORY_BULK_CREATE);
        assertThat(row.values()).containsEntry("name","ទូរស័ព្ទ").containsEntry("status","true");
    }
    @Test void updatesRequireReviewedVersionAndExistingResourceId(){
        assertThatThrownBy(()->parse("CATEGORY_BULK_UPDATE","{\"name\":\"Phone\",\"status\":true}"))
            .isInstanceOf(AiFailure.class);
        assertThat(parse("CATEGORY_BULK_UPDATE","{\"id\":3,\"expectedVersion\":0,\"name\":\"Phone\",\"status\":true}").values())
            .containsEntry("expectedVersion","0");
    }
    @Test void rejectsUnknownToolsArbitraryUrlsNestedValuesAndImageReferences(){
        for(String values:new String[]{"{\"name\":\"Phone\",\"status\":true,\"url\":\"https://evil.test\"}",
                "{\"name\":{},\"status\":true}","{\"name\":\"Phone\",\"status\":true,\"imageIds\":\"fake\"}","[]","null","{} {}","{\"name\":\"A\",\"name\":\"B\",\"status\":true}"})
            assertThatThrownBy(()->parse("CATEGORY_BULK_CREATE",values)).isInstanceOf(AiFailure.class);
        assertThatThrownBy(()->parse("IMPORT_EXECUTION_STATUS","{}")).isInstanceOf(AiFailure.class);
        assertThatThrownBy(()->parse("HTTP_POST","{}")).isInstanceOf(AiFailure.class);
    }
    @Test void productDraftAllowsImagesToBeAttachedDuringReviewButNotInvented(){
        assertThat(parse("PRODUCT_BULK_CREATE","{\"name\":\"Phone\",\"subCategoryId\":1,\"isActive\":true}").values()).doesNotContainKey("imageIds");
        assertThatThrownBy(()->parse("PRODUCT_BULK_CREATE","{\"name\":\"Phone\",\"subCategoryId\":-1,\"isActive\":true}"))
            .isInstanceOf(AiFailure.class);
    }
}
