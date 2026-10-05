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

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "admin.monitor-enabled=false",
        "jwt.secret=66546A5744444446E5A7234743777217A25432A462D4A614E645267556B587032733576"
})
@AutoConfigureMockMvc
class StaffSessionTest {

    @Autowired
    MockMvc mvc;

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/admin/session")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerIsForbidden() throws Exception {
        mvc.perform(get("/api/v1/admin/session")
                        .with(user("shopper").authorities(new SimpleGrantedAuthority("CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSeesTheTokenSubject() throws Exception {
        mvc.perform(get("/api/v1/admin/session")
                        .with(user("ada").authorities(new SimpleGrantedAuthority("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ada"))
                .andExpect(jsonPath("$.data.authorities[0]").value("ADMIN"));
    }

    @Test
    void managerIsStaff() throws Exception {
        mvc.perform(get("/api/v1/admin/session")
                        .with(user("mina").authorities(new SimpleGrantedAuthority("MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("mina"));
    }
}
