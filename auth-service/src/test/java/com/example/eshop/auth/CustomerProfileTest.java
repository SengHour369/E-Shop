package com.example.eshop.auth;
import com.example.eshop.auth.controller.CustomerProfileController;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CustomerProfileTest {
    UserRepository users=mock(UserRepository.class);
    CustomerProfileController controller=new CustomerProfileController(users);
    User owner;
    @BeforeEach void setup(){
        var auth=new UsernamePasswordAuthenticationToken("shopper","",List.of());auth.setDetails(Map.of("userId",7));
        SecurityContextHolder.getContext().setAuthentication(auth);
        owner=User.builder().id(7L).username("shopper").email("shopper@example.com").password("unchanged").enabled(true).status("ACT").deleted(false).build();
        when(users.findById(7L)).thenReturn(Optional.of(owner));when(users.saveAndFlush(owner)).thenReturn(owner);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void updatesOnlyCurrentCustomerProfile(){
        var result=controller.update(new CustomerProfileController.Update("  Shopper Name  ","+855 12345678","1998-01-15"));
        assertEquals(7L,result.id());assertEquals("Shopper Name",result.fullName());assertEquals("+855 12345678",result.phone());
        assertEquals("unchanged",owner.getPassword());assertEquals("shopper@example.com",owner.getEmail());
        verify(users).findById(7L);verify(users).saveAndFlush(owner);
    }
    @Test void blocksAnonymous(){SecurityContextHolder.clearContext();assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.get());verifyNoInteractions(users);}
    @Test void blocksDisabledAccount(){owner.setEnabled(false);assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.get());verify(users,never()).saveAndFlush(any());}
    @Test void rejectsInvalidAndFutureDates(){
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.update(new CustomerProfileController.Update("Shopper","+85512345678","2026-02-30")));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.update(new CustomerProfileController.Update("Shopper","+85512345678","2999-01-01")));
        verify(users,never()).saveAndFlush(any());
    }
    @Test void mapsPhoneConflict(){when(users.saveAndFlush(owner)).thenThrow(new org.springframework.dao.DataIntegrityViolationException("constraint"));var error=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.update(new CustomerProfileController.Update("Shopper","+85512345678",null)));assertEquals(409,error.getStatusCode().value());}
    @Test void validatesRequiredFields(){try(var validator=jakarta.validation.Validation.buildDefaultValidatorFactory()){assertFalse(validator.getValidator().validate(new CustomerProfileController.Update("","abc",null)).isEmpty());}}
}
