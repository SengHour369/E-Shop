package com.example.eshop.auth.service;

import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.service.impl.AddressServiceImpl;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.dto.request.AddressRequest;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiAddressOwnershipTest {
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    private void actor(){var actor=new UsernamePasswordAuthenticationToken("buyer",null,List.of());actor.setDetails(Map.of("userId",7L));SecurityContextHolder.getContext().setAuthentication(actor);}
    @Test void suppliedCustomerIdCannotReadOrWriteAnotherCustomersAddresses(){
        actor();var addresses=mock(AddressRepository.class);var users=mock(UserRepository.class);var service=new AddressServiceImpl(addresses,users);
        assertThatThrownBy(()->service.getUserAddresses(8L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.createAddress(new AddressRequest(),8L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.deleteAddress(1L,8L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(addresses,users);
    }
    @Test void directAddressLookupChecksLiveAccountAndOwnershipBeforeReturningDetails(){
        actor();var addresses=mock(AddressRepository.class);var users=mock(UserRepository.class);var user=new User();user.setId(7L);user.setEnabled(true);
        when(users.findUserById(7L)).thenReturn(Optional.of(user));
        var service=new AddressServiceImpl(addresses,users);
        assertThatThrownBy(()->service.getAddressById(1L)).hasMessage("Address unavailable");
        verify(addresses,never()).findByAddressId(anyLong());
        user.setEnabled(false);
        assertThatThrownBy(()->service.getUserAddresses(7L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
}
