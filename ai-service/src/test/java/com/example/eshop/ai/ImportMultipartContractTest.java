package com.example.eshop.ai;
import com.example.eshop.ai.client.ImportDomainClient;
import com.example.eshop.common.imports.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
class ImportMultipartContractTest {
 @Test void approvedProductRequestPreservesHeadersJsonAndImageBytes()throws Exception{
  AtomicReference<feign.Request> captured=new AtomicReference<>();ObjectMapper mapper=new ObjectMapper();
  var client=feign.Feign.builder().contract(new SpringMvcContract()).encoder(new SpringEncoder(()->new HttpMessageConverters()))
    .decoder((response,type)->mapper.readValue(response.body().asInputStream(),ImportContracts.Result.class))
    .client((request,options)->{captured.set(request);return feign.Response.builder().status(200).reason("OK").request(request).headers(Map.of("Content-Type",List.of("application/json"))).body("{\"resourceId\":\"900\"}",StandardCharsets.UTF_8).build();})
    .target(ImportDomainClient.class,"http://import-test.invalid");
  byte[] pixels={1,2,3,4};String json="{\"tool\":\"PRODUCT_BULK_CREATE\",\"values\":{\"name\":\"Phone\"}}";
  var result=client.executeProduct("Bearer test","batch:row","signed-proof",json,List.of(new ImportImageFile("product.png",pixels)));
  assertThat(result.resourceId()).isEqualTo("900");var request=captured.get();assertThat(request.url()).endsWith("/internal/ai/imports/execute-product");
  assertThat(request.headers().get("Authorization")).contains("Bearer test");assertThat(request.headers().get("X-Import-Approval")).contains("signed-proof");
  assertThat(request.headers().get("Content-Type").iterator().next()).contains("multipart/form-data");String body=new String(request.body(),StandardCharsets.ISO_8859_1);
  assertThat(body).contains("name=\"row\"",json,"name=\"files\"","filename=\"product.png\"","image/png");
  assertThat(body).contains(new String(pixels,StandardCharsets.ISO_8859_1));
 }
}
