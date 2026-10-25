package com.example.eshop.common.security;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class WorkflowReadScopeTest {
 @Test void readScopeCannotMutateOrAccessOtherAdminEndpoints(){assertThat(WorkflowReadScope.allows("GET","/internal/ai/products/1")).isTrue();assertThat(WorkflowReadScope.allows("GET","/internal/authorization/me")).isTrue();for(String method:List.of("POST","PUT","PATCH","DELETE"))assertThat(WorkflowReadScope.allows(method,"/internal/ai/products/1")).isFalse();for(String path:List.of("/api/v1/user/workflow-grants","/api/ai/admin/workflows","/internal/workflow-authorization/token","/internal/ai/products/1/edit","/api/v1/user/admin"))assertThat(WorkflowReadScope.allows("GET",path)).isFalse();}
 @Test void backgroundIdentityRestoresPreviousAuthenticationEvenOnFailure(){var previous=new UsernamePasswordAuthenticationToken("previous",null,List.of());SecurityContextHolder.getContext().setAuthentication(previous);try{assertThatThrownBy(()->BackgroundIdentity.run(42,"owner","Bearer scoped",()->{assertThat(CurrentActor.userId()).isEqualTo(42);assertThat(BackgroundIdentity.bearer()).isEqualTo("Bearer scoped");throw new IllegalStateException("interrupted");})).hasMessage("interrupted");assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(previous);assertThat(BackgroundIdentity.bearer()).isNull();}finally{SecurityContextHolder.clearContext();}}
}
