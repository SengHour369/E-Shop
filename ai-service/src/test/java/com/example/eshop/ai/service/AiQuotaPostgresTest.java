package com.example.eshop.ai.service;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
/** Only the disposable local quota_verification database; never the shop database. */
@DataJpaTest(properties={"spring.datasource.url=jdbc:postgresql://127.0.0.1:55434/quota_verification","spring.datasource.username=postgres","spring.datasource.password=quota-test-only","spring.jpa.hibernate.ddl-auto=create-drop","eureka.client.enabled=false"})
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named="ESHOP_QUOTA_POSTGRES_TEST",matches="true")
class AiQuotaPostgresTest extends AiQuotaServiceTest {}
