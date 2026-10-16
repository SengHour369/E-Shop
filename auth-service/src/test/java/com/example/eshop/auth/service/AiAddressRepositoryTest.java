package com.example.eshop.auth.service;

import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.AddressRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql=false,properties={"spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes=AiAddressRepositoryTest.Config.class)
class AiAddressRepositoryTest {
    @Configuration @EntityScan("com.example.eshop.auth.model") @EnableJpaRepositories("com.example.eshop.auth.repository")
    static class Config {}
    @Autowired EntityManager em; @Autowired AddressRepository addresses;
    @Test void checkoutAndAddressListsExcludeDeletedAddressesDisabledAccountsAndOtherOwners() {
        var owner=user("buyer");var other=user("other");
        var address=address();var deleted=address();deleted.setDeleted(true);
        owner.setAddresses(new ArrayList<>(List.of(address,deleted)));
        em.flush();em.clear();
        assertThat(addresses.findByUserId(owner.getId())).extracting(Address::getId).containsExactly(address.getId());
        assertThat(addresses.findOwnedPage(owner.getId(),org.springframework.data.domain.PageRequest.of(0,10)).getContent()).hasSize(1);
        assertThat(addresses.isUserHasAddress(other.getId(),address.getId())).isFalse();
        assertThat(addresses.isUserHasAddress(owner.getId(),deleted.getId())).isFalse();
        em.find(User.class,owner.getId()).setEnabled(false);em.flush();em.clear();
        assertThat(addresses.isUserHasAddress(owner.getId(),address.getId())).isFalse();
    }
    private User user(String name){var user=new User();user.setUsername(name);user.setEmail(name+"@example.test");user.setPhone(name);user.setPassword("test-hash");user.setStatus("ACTIVE");user.setEnabled(true);em.persist(user);return user;}
    private Address address(){var address=new Address();address.setAddressLine1("Street 271");address.setCity("Phnom Penh");address.setCountry("Cambodia");address.setZipCode("12000");em.persist(address);return address;}
}
