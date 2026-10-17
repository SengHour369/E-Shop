package com.example.eshop.auth.service;
import com.example.eshop.auth.service.impl.*;
import com.example.eshop.auth.model.User;
import com.example.eshop.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.util.Optional;
class LoginSecondFactorTest {
    final EmailService mail=mock(EmailService.class);
    final UserRepository users=mock(UserRepository.class);
    final LoginSecondFactor service=new LoginSecondFactor(mail,users);
    User user(){return User.builder().id(7L).email("person@example.test").build();}
    @Test void requiresEmailedCodeAndRejectsReplay() throws Exception {
        User user=user(); when(users.findUserById(7L)).thenReturn(Optional.of(user));
        var response=service.issue(user);
        assertThat(response.accessToken()).isNull(); assertThat(response.tokenType()).isEqualTo("MFA");
        var body=ArgumentCaptor.forClass(String.class);verify(mail).sendVerificationEmail(eq(user.getEmail()),anyString(),body.capture());
        String code=body.getValue().split("<strong>")[1].split("</strong>")[0];
        assertThatThrownBy(()->service.verify(response.refreshToken(),"wrong")).hasMessageContaining("Invalid");
        assertThat(service.verify(response.refreshToken(),code)).isSameAs(user);
        assertThatThrownBy(()->service.verify(response.refreshToken(),code)).hasMessageContaining("expired");
    }
    @Test void locksChallengeAfterFiveIncorrectAttempts() {
        var response=service.issue(user());
        for(int i=0;i<5;i++) assertThatThrownBy(()->service.verify(response.refreshToken(),"wrong")).hasMessageContaining("Invalid");
        assertThatThrownBy(()->service.verify(response.refreshToken(),"wrong")).hasMessageContaining("expired");
        verifyNoInteractions(users);
    }
    @Test void replacingChallengeInvalidatesPreviousCode() {
        var first=service.issue(user());service.issue(user());
        assertThatThrownBy(()->service.verify(first.refreshToken(),"000000")).hasMessageContaining("expired");
    }
}
