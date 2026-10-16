package com.example.eshop.auth.service;
import com.example.eshop.auth.dto.request.GroupRequest;
import com.example.eshop.auth.exception.DuplicateResourceException;
import com.example.eshop.auth.model.Group;
import com.example.eshop.auth.repository.GroupRepository;
import com.example.eshop.auth.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class GroupCodeTest {
 @Mock GroupRepository repository;
 @InjectMocks GroupServiceImpl service;
 @Test void persistsExplicitCodeAndGroupType() {
  when(repository.save(any())).thenAnswer(invocation -> { Group group=invocation.getArgument(0);group.setId(12L);return group; });
  service.createGroup(GroupRequest.builder().name("VIP Customers").groupCode("VIP").type("VIP").build());
  ArgumentCaptor<Group> captor=ArgumentCaptor.forClass(Group.class);verify(repository).save(captor.capture());assertThat(captor.getValue().getGroupCode()).isEqualTo("VIP");
 }
 @Test void rejectsDuplicateExplicitCode() {
  when(repository.existsByGroupCode("VIP")).thenReturn(true);
  assertThatThrownBy(()->service.createGroup(GroupRequest.builder().name("VIP").groupCode("VIP").build())).isInstanceOf(DuplicateResourceException.class);
  verify(repository,never()).save(any());
 }
 @Test void keepsOwnCodeAndUpdatesType() {
  Group group=Group.builder().id(12L).groupCode("VIP").type("CUSTOMER").build();
  when(repository.findById(12L)).thenReturn(Optional.of(group));when(repository.save(group)).thenReturn(group);
  service.updateGroup(12L,GroupRequest.builder().name("VIP Customers").groupCode("VIP").type("VIP").build());
  assertThat(group.getType()).isEqualTo("VIP");verify(repository,never()).existsByGroupCode(anyString());
 }
}
