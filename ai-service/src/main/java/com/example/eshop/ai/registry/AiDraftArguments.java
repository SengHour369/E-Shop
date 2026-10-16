package com.example.eshop.ai.registry;

import com.example.eshop.ai.enums.AiExecutionStatus;
import com.example.eshop.ai.service.AiFailure;
import com.example.eshop.common.imports.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;

/** Staging uses exactly the existing approved import schemas, never a user-selected API. */
public final class AiDraftArguments {
    private AiDraftArguments() {}
    public static ImportContracts.Row parse(JsonNode parameters) {
        try {
            var tool=ImportTool.valueOf(parameters.path("toolType").asText());
            if(!tool.enabled||!tool.domainTool())throw new IllegalArgumentException();
            JsonNode object=com.fasterxml.jackson.databind.json.JsonMapper.builder().enable(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build().readTree(parameters.path("valuesJson").asText());
            if(!object.isObject()||object.size()>32)throw new IllegalArgumentException();
            var values=new TreeMap<String,String>();
            object.fields().forEachRemaining(entry->{
                if(!entry.getValue().isValueNode()||entry.getValue().isNull()||Set.of("imageIds","imageChecksums").contains(entry.getKey()))throw new IllegalArgumentException();
                values.put(entry.getKey(),entry.getValue().asText());
            });
            var errors=tool.validate(values);
            // Product images must be attached through the existing reviewed upload UI before validation/execution.
            if(errors.stream().anyMatch(error->!(tool==ImportTool.PRODUCT_BULK_CREATE&&error.equals("IMAGE_ATTACHMENT_REQUIRED"))))
                throw new IllegalArgumentException();
            return new ImportContracts.Row(tool,values);
        }catch(Exception invalid){
            throw new AiFailure("INVALID_DRAFT",AiExecutionStatus.NEEDS_INPUT,"Provide valid fields for a registered import operation. Images are attached during draft review.");
        }
    }
}
