package com.example.eshop.admin;

import com.example.eshop.admin.repository.StorefrontAccessSettingsRepository;
import com.example.eshop.common.security.*;
import com.example.eshop.common.jwt.JwtProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.io.Decoders;
import java.time.Instant;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:guest-settings;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=create-drop",
 "eureka.client.enabled=false","spring.cloud.discovery.enabled=false","admin.monitor-enabled=false",
 "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"})
@AutoConfigureMockMvc
class StorefrontAccessTest {
    @Autowired MockMvc mvc;
    @Autowired StorefrontAccessSettingsRepository settings;
    @Autowired JwtProperties jwt;
    @MockitoBean LivePermissionService permissions;
    String body="""
        {"guestBrowsing":true,"guestReviews":false,"guestCart":true,"guestCheckout":true,"guestPayment":true,
        "requireDeliveryAddress":true,"requireGuestContactVerification":true,"guestOrderTracking":true}
        """;
    @BeforeEach void setup() {
        settings.deleteAll();doAnswer(i->{if(!CurrentActor.has("ADMIN")) throw new org.springframework.security.access.AccessDeniedException("Missing permission");return null;}).when(permissions).require(anyString());
    }
    String token(String role) {return "Bearer "+Jwts.builder().setSubject("operator").claim("authorities",List.of(role)).claim("isEnable",true).claim("userId",1L)
            .setExpiration(Date.from(Instant.now().plusSeconds(60))).signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret()))).compact();}
    @Test void publicDefaultsAreSafeAndContainNoSecrets() throws Exception {
        mvc.perform(get("/api/v1/storefront/access-settings")).andExpect(status().isOk()).andExpect(jsonPath("$.guestBrowsing").value(true))
            .andExpect(jsonPath("$.guestCheckout").value(false)).andExpect(jsonPath("$.guestPayment").value(false)).andExpect(jsonPath("$.updatedBy").doesNotExist());
    }
    @Test void permissionRequiredAndSavedSettingsSurvivePublicRead() throws Exception {
        mvc.perform(put("/api/v1/admin/storefront/access-settings").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/storefront/access-settings").header("Authorization",token("USER")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/storefront/access-settings").header("Authorization",token("ADMIN")).contentType("application/json").content(body)).andExpect(status().isOk());
        Assertions.assertEquals(1,settings.count());
        mvc.perform(get("/api/v1/storefront/access-settings")).andExpect(jsonPath("$.guestCheckout").value(true)).andExpect(jsonPath("$.requireGuestContactVerification").value(true));
    }
    @Test void checkoutPaymentAndPhysicalAddressDependenciesAreEnforced() throws Exception {
        for(String invalid:List.of(body.replace("\"guestCheckout\":true","\"guestCheckout\":false"),body.replace("\"guestCart\":true","\"guestCart\":false"),body.replace("\"requireDeliveryAddress\":true","\"requireDeliveryAddress\":false"))) {
            mvc.perform(put("/api/v1/admin/storefront/access-settings").header("Authorization",token("ADMIN")).contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        Assertions.assertEquals(0,settings.count());
    }
}
