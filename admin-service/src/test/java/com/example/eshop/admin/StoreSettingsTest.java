package com.example.eshop.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import org.springframework.http.MediaType;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-settings;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "admin.monitor-enabled=false",
        "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"
})
@AutoConfigureMockMvc

class StoreSettingsTest {
    @Autowired MockMvc mvc;
    private static final String BODY = """
            {"storeName":"My Shop","email":"shop@example.test","phone":"123","address":"Street 1","timezone":"Asia/Bangkok"}
            """;

    @Test void customerCannotReadOrWriteSettings() throws Exception {
        mvc.perform(get("/api/v1/admin/settings").with(user("buyer").authorities(new SimpleGrantedAuthority("USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/admin/settings").with(user("buyer").authorities(new SimpleGrantedAuthority("USER")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    @Test void adminSavesAndManagerReadsButCannotWrite() throws Exception {
        mvc.perform(put("/api/v1/admin/settings").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.storeName").value("My Shop"));
        mvc.perform(get("/api/v1/admin/settings").with(user("manager").authorities(new SimpleGrantedAuthority("MANAGER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.storeName").value("My Shop"));
        mvc.perform(put("/api/v1/admin/settings").with(user("manager").authorities(new SimpleGrantedAuthority("MANAGER")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
    }

    @Test void invalidSettingsReturnTheExistingErrorEnvelope() throws Exception {
        mvc.perform(put("/api/v1/admin/settings").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("Asia/Bangkok", "Unknown/Zone")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        mvc.perform(put("/api/v1/admin/settings").with(user("admin").authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("shop@example.test", "invalid")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }
}
