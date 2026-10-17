package com.example.eshop.auth.service;

import com.example.eshop.auth.service.impl.EmailNotificationService;
import com.example.eshop.auth.service.impl.UserCreatedMail;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListenerFactory;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserCreatedMailTest {
    static class Transactions extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object tx, TransactionDefinition definition){}
        protected void doCommit(DefaultTransactionStatus status){}
        protected void doRollback(DefaultTransactionStatus status){}
    }
    @Test void sendsOnlyAfterCommitAndDoesNotSendForRollback() {
        var mail=mock(EmailNotificationService.class);
        try(var context=new AnnotationConfigApplicationContext()) {
            context.registerBean(TransactionalEventListenerFactory.class);
            context.registerBean(UserCreatedMail.class,()->new UserCreatedMail(mail));
            context.refresh();
            var transaction=new TransactionTemplate(new Transactions());
            var event=new UserCreatedMail.Event(12L,"jane@example.test","Jane","jane");
            transaction.executeWithoutResult(status->{context.publishEvent(event);verifyNoInteractions(mail);});
            verify(mail).sendSimpleEmail(eq("jane@example.test"),eq("Welcome to E-Shop"),contains("Your username is: jane"));
            clearInvocations(mail);
            transaction.executeWithoutResult(status->{context.publishEvent(event);status.setRollbackOnly();});
            verifyNoInteractions(mail);
        }
    }
    @Test void smtpFailureDoesNotTurnCommittedCreationIntoAnApiFailure() {
        var mail=mock(EmailNotificationService.class);
        doThrow(new RuntimeException("SMTP offline")).when(mail).sendSimpleEmail(anyString(),anyString(),anyString());
        assertDoesNotThrow(()->new UserCreatedMail(mail).welcome(new UserCreatedMail.Event(12L,"jane@example.test","Jane","jane")));
    }
}
