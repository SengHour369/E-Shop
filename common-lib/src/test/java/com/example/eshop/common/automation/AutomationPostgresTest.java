package com.example.eshop.common.automation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="AUTOMATION_TEST_JDBC_URL",matches=".+")
class AutomationPostgresTest {
    JdbcTemplate jdbc;TransactionTemplate tx;AutomationEvents events;AutomationWorker worker;CommerceHandler handler;
    KafkaTemplate<String,String> kafka;
    @BeforeEach void setup() throws Exception {
        var ds=new DriverManagerDataSource(System.getenv("AUTOMATION_TEST_JDBC_URL"),"postgres","");jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("DROP SCHEMA IF EXISTS automation_test CASCADE");jdbc.execute("CREATE SCHEMA automation_test");
        ds.setUrl(System.getenv("AUTOMATION_TEST_JDBC_URL")+"?currentSchema=automation_test");
        jdbc.execute("CREATE TABLE order_details(id BIGINT PRIMARY KEY)");
        String migration=Files.readString(Path.of("../order-service/src/main/resources/db/upgrade/20261011_commerce_automation.sql"));
        for(String sql:migration.split(";"))if(!sql.isBlank())jdbc.execute(sql);
        for(String sql:migration.split(";"))if(!sql.isBlank())jdbc.execute(sql); // Repeatable existing-schema upgrade.
        var mapper=new ObjectMapper().findAndRegisterModules();events=new AutomationEvents(jdbc,mapper);handler=mock(CommerceHandler.class);kafka=mock(KafkaTemplate.class);
        worker=new AutomationWorker(events,jdbc,mapper,kafka,tx,handler);
    }
    CommerceEvent event(){return AutomationEvents.event(CommerceEvent.Type.ORDER_CREATED,1,2,BigDecimal.TEN,"BAKONG",null);}
    @Test void rollbackDoesNotPublishAndDuplicateInboxExecutesOnce(){
        var e=event();tx.executeWithoutResult(t->{events.emit(CommerceEvent.ORDERS,e);t.setRollbackOnly();});
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM automation_events",Long.class)).isZero();
        events.receive(CommerceEvent.ORDERS,e);events.receive(CommerceEvent.ORDERS,e);worker.tick();worker.tick();
        verify(handler,times(1)).handle(e);assertThat(jdbc.queryForObject("SELECT state FROM automation_events",String.class)).isEqualTo("DONE");
    }
    @Test void failedHandlerRollsBackAndCanRecover(){
        var e=event();events.receive(CommerceEvent.ORDERS,e);
        doAnswer(i->{jdbc.update("INSERT INTO automation_reviews(order_id,code,summary,request_id) VALUES(1,'rolled-back','test','test')");throw new IllegalStateException("offline");}).when(handler).handle(e);
        worker.tick();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM automation_reviews",Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT attempts FROM automation_events",Integer.class)).isEqualTo(1);
        reset(handler);jdbc.update("UPDATE automation_events SET next_attempt_at=CURRENT_TIMESTAMP");worker.tick();
        assertThat(jdbc.queryForObject("SELECT state FROM automation_events",String.class)).isEqualTo("DONE");
    }
    @Test void exhaustedEventIsRetainedAndPublishedToDeadLetterTopic(){
        var e=event();events.receive(CommerceEvent.ORDERS,e);jdbc.update("UPDATE automation_events SET attempts=9");doThrow(new IllegalStateException("offline")).when(handler).handle(e);
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));worker.tick();
        assertThat(jdbc.queryForObject("SELECT state FROM automation_events",String.class)).isEqualTo("DEAD");
        verify(kafka).send(eq(CommerceEvent.ORDERS+".DLT"),eq("1"),anyString());assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM automation_reviews",Long.class)).isEqualTo(1);
    }
    @Test void brokerFailureKeepsOutboxPending(){var e=event();events.emit(CommerceEvent.ORDERS,e);when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker")));worker.tick();assertThat(jdbc.queryForObject("SELECT state FROM automation_events",String.class)).isEqualTo("PENDING");}
    @Test void allUpgradeScriptsApplyRepeatablyAndUniqueQrOrderIsEnforced() throws Exception {
        jdbc.execute("CREATE TABLE payment_transactions(id BIGINT PRIMARY KEY, order_id BIGINT, status VARCHAR(30))");
        jdbc.execute("CREATE TABLE notifications(id BIGINT PRIMARY KEY, type VARCHAR(40))");
        jdbc.execute("CREATE TABLE ai_executions(id BIGINT PRIMARY KEY, intent VARCHAR(80))");
        for(String file:java.util.List.of("../payment-service/src/main/resources/db/upgrade/20261011_commerce_automation.sql","../notification-service/src/main/resources/db/upgrade/20261011_commerce_notifications.sql","../ai-service/src/main/resources/db/upgrade/20261011_automation_review_intent.sql"))
            for(int i=0;i<2;i++)for(String sql:Files.readString(Path.of(file)).split(";"))if(!sql.isBlank())jdbc.execute(sql);
        jdbc.update("INSERT INTO payment_transactions(id,order_id,status,qr_md5) VALUES(1,7,'PENDING',?)","a".repeat(32));
        assertThatThrownBy(()->jdbc.update("INSERT INTO payment_transactions(id,order_id,status,qr_md5) VALUES(2,7,'PENDING',?)","b".repeat(32))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        events.review(7,"DUPLICATE_TEST","test");events.review(7,"DUPLICATE_TEST","test");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM automation_reviews",Long.class)).isEqualTo(1);
    }
}

