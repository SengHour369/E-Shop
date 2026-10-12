package com.example.eshop.auth.mapper;
import com.example.eshop.auth.model.Address;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(classes=AddressPersistenceTest.Config.class, properties={
 "spring.datasource.url=jdbc:h2:mem:address_delivery;DB_CLOSE_DELAY=-1", "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop", "eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"
})
@Transactional
class AddressPersistenceTest {
 @Configuration @EnableAutoConfiguration @EntityScan(basePackageClasses=Address.class) static class Config {}
 @Autowired EntityManager entities;
 @Test void storesAndReloadsDeliveryDetails() {
  var address=Address.builder().addressLine1("Street 271").city("Phnom Penh").zipCode("120101").country("Cambodia").label("Home").recipientName("Customer").phoneNumber("+85512345678").district("Chamkar Mon").commune("Tuol Tumpung Ti Muoy").additionalInformation("Floor 2").isDefault(true).build();
  entities.persist(address);entities.flush();var id=address.getId();entities.clear();
  var loaded=entities.find(Address.class,id);
  assertEquals("Home",loaded.getLabel()); assertEquals("Customer",loaded.getRecipientName()); assertEquals("+85512345678",loaded.getPhoneNumber()); assertEquals("Chamkar Mon",loaded.getDistrict()); assertEquals("Tuol Tumpung Ti Muoy",loaded.getCommune()); assertEquals("Floor 2",loaded.getAdditionalInformation());
 }
}
