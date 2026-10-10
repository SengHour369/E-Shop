package com.example.eshop.ai;
import com.example.eshop.ai.service.AiAdminWorkspaceService;
import com.example.eshop.ai.controller.AiAdminWorkspaceController;
import com.example.eshop.ai.dto.*;
import com.example.eshop.ai.enums.*;
import com.example.eshop.common.security.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class AiAdminWorkspaceTest {
    StringRedisTemplate redis;
    ValueOperations<String,String> values;
    ZSetOperations<String,String> index;
    Map<String,String> storage;
    AiAdminWorkspaceService workspace;
    @BeforeEach void setup() {
        redis = mock(StringRedisTemplate.class); values = mock(ValueOperations.class); index = mock(ZSetOperations.class);
        storage = new HashMap<>();
        when(redis.opsForValue()).thenReturn(values); when(redis.opsForZSet()).thenReturn(index);
        when(values.get(anyString())).thenAnswer(call -> storage.get(call.getArgument(0)));
        doAnswer(call -> { storage.put(call.getArgument(0),call.getArgument(1)); return null; }).when(values).set(anyString(),anyString(),any(Duration.class));
        workspace = new AiAdminWorkspaceService(redis,new ObjectMapper().findAndRegisterModules()); actor(7);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void actor(long id) {
        var auth = new UsernamePasswordAuthenticationToken("admin", "", List.of());
        auth.setDetails(Map.of("userId",id)); SecurityContextHolder.getContext().setAuthentication(auth);
    }
    AiChatResponse reply(UUID id) {
        return new AiChatResponse(id,"SUPER_ADMIN","ConversationCard",List.of(), new AiResponse(null,null,null,AiIntent.UNKNOWN,AiExecutionStatus.SUCCESS,"Hello",null,null));
    }
    @Test void savesAndRestoresStructuredTurnsAndRenames() {
        UUID id = UUID.randomUUID(); workspace.remember("Question",reply(id));
        assertThat(workspace.get(id).messages()).hasSize(2);
        assertThat(workspace.get(id).messages().get(1).result().message()).isEqualTo("Hello");
        workspace.rename(id,"New name"); assertThat(workspace.get(id).title()).isEqualTo("New name");
        verify(values,atLeastOnce()).set(eq("ai:workspace:7:"+id),anyString(),eq(Duration.ofMinutes(30)));
    }
    @Test void anotherAdminCannotReadOrRenameAnOwnedConversation() {
        UUID id = UUID.randomUUID(); workspace.remember("Private",reply(id)); actor(8);
        assertThatThrownBy(() -> workspace.get(id)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> workspace.rename(id,"Changed")).isInstanceOf(ResponseStatusException.class);
        actor(7); assertThat(workspace.get(id).title()).isEqualTo("Private");
    }
    @Test void transcriptAndListAreBounded() {
        UUID id = UUID.randomUUID(); for (int i=0;i<25;i++) workspace.remember("Question "+i,reply(id));
        assertThat(workspace.get(id).messages()).hasSize(40);
        verify(index,atLeastOnce()).removeRange("ai:workspace:7:index",0,-21);
    }
    @Test void expiredConversationIsNotFound() {
        assertThatThrownBy(() -> workspace.get(UUID.randomUUID())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void nonAdminCannotUseWorkspaceController() {
        LivePermissionService permissions = mock(LivePermissionService.class);
        when(permissions.current()).thenReturn(new PermissionSnapshot(7L,false,Set.of()));
        var controller = new AiAdminWorkspaceController(workspace,permissions);
        assertThatThrownBy(controller::list).isInstanceOf(AccessDeniedException.class); verifyNoInteractions(values);
    }
}
