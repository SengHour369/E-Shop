package com.example.eshop.ai;
import com.example.eshop.ai.workflow.*;
import com.example.eshop.ai.registry.*;
import com.example.eshop.ai.client.AiProviderClient;
import com.example.eshop.ai.enums.*;
import com.example.eshop.ai.dto.AiIntentResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class WorkflowAuthoringTest {
 ObjectMapper mapper=new ObjectMapper();
 AiToolRegistry registry=mock(AiToolRegistry.class);AiProviderClient provider=mock(AiProviderClient.class);
 WorkflowPublicationService publications=mock(WorkflowPublicationService.class);WorkflowController validation=mock(WorkflowController.class);
 WorkflowAuthoringController controller=new WorkflowAuthoringController(publications,validation,registry,provider,mapper);
 AiToolDefinition read(){return new AiToolDefinition(AiIntent.PRODUCT_GET,"Read product","catalog-service","GET","GET","/fixed",Map.of(),"PRODUCT_VIEW",AiToolRisk.READ_ONLY,false,true);}
 @Test void templatesLeaveRequiredResourceReferencesUnfilled(){
  var tool=new AiToolDefinition(AiIntent.PRODUCT_GET,"Read product","catalog-service","GET","GET","/fixed",Map.of("id",new AiToolDefinition.Parameter("integer",true,"Product ID")),"PRODUCT_VIEW",AiToolRisk.READ_ONLY,false,true);
  when(registry.available()).thenReturn(List.of(tool));var templates=controller.templates();assertThat(templates).hasSize(1);var definition=(com.fasterxml.jackson.databind.JsonNode)templates.get(0).get("definition");assertThat(definition.path("steps").get(0).path("parameters").path("id").isNull()).isTrue();assertThat(templates.get(0).get("requiresInput")).isEqualTo(true);verifyNoInteractions(provider);
 }
 @Test void unsupportedAiProposalNeverBecomesAnExecutableDefinition(){when(registry.available()).thenReturn(List.of(read()));when(provider.detect(anyString(),anyList())).thenReturn(new AiIntentResult(AiIntent.PAYMENT_REFUND,1,mapper.createObjectNode()));assertThatThrownBy(()->controller.propose(new WorkflowAuthoringController.Proposal("refund the payment"))).hasMessageContaining("422");verify(registry,never()).authorize(any());verifyNoInteractions(validation);}
 @Test void providerProposalIsReauthorizedAndValidatedWithoutExecutingTools(){var tool=read();when(registry.available()).thenReturn(List.of(tool));when(provider.detect(anyString(),anyList())).thenReturn(new AiIntentResult(AiIntent.PRODUCT_GET,.95,mapper.createObjectNode().put("id",7)));when(registry.authorize(AiIntent.PRODUCT_GET)).thenReturn(tool);when(validation.validate(any())).thenReturn(Map.of("valid",true));var result=controller.propose(new WorkflowAuthoringController.Proposal("read product number 7"));assertThat(result.get("executed")).isEqualTo(false);verify(registry).validate(eq(tool),any());verify(validation).validate(any());}
 @Test void providerFailureIsReportedAsUnavailableInsteadOfAUserAuthenticationFailure(){when(registry.available()).thenReturn(List.of(read()));when(provider.detect(anyString(),anyList())).thenThrow(new com.example.eshop.ai.service.AiFailure("AI_PROVIDER_FAILURE",AiExecutionStatus.FAILURE,"Unavailable"));assertThatThrownBy(()->controller.propose(new WorkflowAuthoringController.Proposal("Find products matching shoes"))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class).hasMessageContaining("503").hasMessageContaining("configure steps manually");}
}
