package com.example.eshop.auth.service;
import com.example.eshop.auth.exception.BadRequestException;
import com.example.eshop.auth.exception.ResourceNotFoundException;
import com.example.eshop.auth.model.FunctionPermission;
import com.example.eshop.auth.model.Group;
import com.example.eshop.auth.model.GroupPermission;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.service.impl.GroupPermissionServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class GroupPermissionValidationTest {
    private final GroupPermissionRepository assignments = mock(GroupPermissionRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final FunctionPermissionRepository functions = mock(FunctionPermissionRepository.class);
    private final GroupPermissionServiceImpl service = new GroupPermissionServiceImpl(assignments, groups, functions);
    @Test void refusesMissingGroupsBeforeWritingAssignments() {
        when(groups.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createGroupPermission(9L, 2L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(assignments);
    }
    @Test void refusesInactiveFunctions() {
        when(groups.findById(9L)).thenReturn(Optional.of(Group.builder().isDelete(false).build()));
        when(functions.findById(2L)).thenReturn(Optional.of(FunctionPermission.builder().isActive(false).build()));
        assertThatThrownBy(() -> service.createGroupPermission(9L, 2L)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(assignments);
    }
    @Test void writesAnActiveAssignmentForAnAvailableFunction() {
        when(groups.findById(9L)).thenReturn(Optional.of(Group.builder().isDelete(false).build()));
        when(functions.findById(2L)).thenReturn(Optional.of(FunctionPermission.builder().funcId(2L).isActive(true).build()));
        when(assignments.save(any(GroupPermission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.createGroupPermission(9L, 2L);
        verify(assignments).save(argThat(value -> value.getGroupId().equals(9L) && value.getFuncId().equals(2L) && value.getIsActive()));
    }
}
