package com.example.eshop.payment;
import com.example.eshop.payment.controller.UnavailableKhqrController;
import com.example.eshop.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
class UnavailableKhqrTest {
 @Test void disabledRoutesExplainConfigurationInsteadOfReturning500() throws Exception {
  var mvc=MockMvcBuilders.standaloneSetup(new UnavailableKhqrController()).setControllerAdvice(new GlobalExceptionHandler(org.mockito.Mockito.mock(com.example.eshop.common.audit.AuditLogService.class))).build();
  for(var path:new String[]{"/api/v1/payments/orders/3/qr","/api/v1/payments/orders/3/qr/image"})
   mvc.perform(get(path)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("KHQR is not configured")));
  mvc.perform(post("/api/v1/payments/orders/3/verify")).andExpect(status().isServiceUnavailable());
 }
 @Test void fallbackIsAbsentWhenRealKhqrAutomationIsEnabled() {
  new ApplicationContextRunner().withUserConfiguration(UnavailableKhqrController.class).withPropertyValues("automation.enabled=true")
   .run(context->assertThat(context).doesNotHaveBean(UnavailableKhqrController.class));
  new ApplicationContextRunner().withUserConfiguration(UnavailableKhqrController.class)
   .run(context->assertThat(context).hasSingleBean(UnavailableKhqrController.class));
 }
}
