package com.example.eshop.catalog.scanner;

import com.example.eshop.catalog.dto.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;
import java.net.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogVisualComparisonTest {
    @Test void imagesCannotTargetLocalHostsRedirectsOrAnotherCloud() {
        var service=new CatalogVisualComparison(new ObjectMapper(),"http://inference:8000","private-test-key","shop");
        assertThat(service.approved("https://res.cloudinary.com/shop/image/upload/product.jpg")).isTrue();
        for(String url:List.of("http://127.0.0.1/image.jpg","https://res.cloudinary.com.evil.test/shop/image/upload/x",
                "https://res.cloudinary.com/other/image/upload/x","https://user:pass@res.cloudinary.com/shop/image/upload/x",
                "https://res.cloudinary.com:1234/shop/image/upload/x")) assertThat(service.approved(url)).isFalse();
    }
    @Test void unconfiguredComparisonNeverClaimsVerifiedSimilarity() {
        var service=new CatalogVisualComparison(new ObjectMapper(),"http://inference:8000","","shop");
        var result=service.compare(photo(),labels());
        assertThat(result.getMatchType()).isEqualTo("AI_LABEL_CANDIDATES");
        assertThat(result.isExact()).isFalse();assertThat(result.isRequiresSelection()).isTrue();
    }
    @Test void rankedIndicesResolveOnlyActualCatalogCandidatesAndSecretsStayOnTheServiceCall() throws Exception {
        var http=mock(HttpClient.class);var sent=new ArrayList<HttpRequest>();
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(invocation->{
            HttpRequest request=invocation.getArgument(0);sent.add(request);
            var response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Content-Type",List.of("image/jpeg")),(a,b)->true));
            byte[] bytes="res.cloudinary.com".equals(request.uri().getHost())?new byte[]{(byte)255,(byte)216,(byte)255,0}:
                "{\"matches\":[{\"index\":1,\"confidence\":0.85},{\"index\":99,\"confidence\":1}]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            when(response.body()).thenReturn(new ByteArrayInputStream(bytes));return response;
        });
        var service=new CatalogVisualComparison(new ObjectMapper(),"http://inference:8000","private-test-key","shop",http);
        var result=service.compare(photo(),labels());
        assertThat(result.getMatchType()).isEqualTo("AI_VISUAL_COMPARISON");
        assertThat(result.getCandidates()).extracting(ImageCandidate::getProductId).containsExactly(7L);
        assertThat(result.isRequiresSelection()).isTrue();assertThat(result.isExact()).isFalse();
        assertThat(sent).hasSize(3);
        sent.stream().filter(r->r.uri().getHost().equals("res.cloudinary.com")).forEach(r->{
            assertThat(r.headers().firstValue("X-Vision-Service-Key")).isEmpty();assertThat(r.headers().firstValue("Authorization")).isEmpty();
        });
        assertThat(sent.get(2).headers().firstValue("X-Vision-Service-Key")).contains("private-test-key");
    }
    private MockMultipartFile photo(){return new MockMultipartFile("image","photo.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255,0});}
    private ImageMatchResponse labels(){return ImageMatchResponse.builder().matchType("AI_IMAGE").exact(false).requiresSelection(true).message("Select")
        .candidates(List.of(new ImageCandidate(6L,60L,"Phone A","A","https://res.cloudinary.com/shop/image/upload/a.jpg",0.8),
                new ImageCandidate(7L,70L,"Phone B","B","https://res.cloudinary.com/shop/image/upload/b.jpg",0.7))).build();}
}
