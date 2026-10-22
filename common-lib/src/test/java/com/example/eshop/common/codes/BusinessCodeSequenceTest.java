package com.example.eshop.common.codes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class BusinessCodeSequenceTest {
    JdbcTemplate jdbc; DataSourceTransactionManager manager; BusinessCodeSequence codes;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=20000", "sa", "");
        jdbc = new JdbcTemplate(source); manager = new DataSourceTransactionManager(source);
        jdbc.execute("CREATE TABLE business_code_counters(code_type VARCHAR(30) PRIMARY KEY,last_value BIGINT NOT NULL)");
        jdbc.update("INSERT INTO business_code_counters VALUES('ORDER',0),('PAYMENT',0)");
        codes = new BusinessCodeSequence(jdbc,manager);
    }
    @Test void generatesSeparateReadableCodesAndSurvivesServiceRestart() {
        assertThat(codes.next(BusinessCodeSequence.Kind.ORDER)).isEqualTo("anOrder_001");
        assertThat(codes.next(BusinessCodeSequence.Kind.PAYMENT)).isEqualTo("anPayment_001");
        assertThat(new BusinessCodeSequence(jdbc,manager).next(BusinessCodeSequence.Kind.ORDER)).isEqualTo("anOrder_002");
        jdbc.update("UPDATE business_code_counters SET last_value=999 WHERE code_type='ORDER'");
        assertThat(codes.next(BusinessCodeSequence.Kind.ORDER)).isEqualTo("anOrder_1000");
    }
    @Test void concurrentRequestsCannotAllocateTheSameCode() throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> tasks=new ArrayList<>();
            for(int i=0;i<100;i++)tasks.add(()->codes.next(BusinessCodeSequence.Kind.ORDER));
            Set<String> allocated=new HashSet<>();
            for(var future:pool.invokeAll(tasks))allocated.add(future.get(30,TimeUnit.SECONDS));
            assertThat(allocated).hasSize(100).contains("anOrder_001","anOrder_100");
            assertThat(jdbc.queryForObject("SELECT last_value FROM business_code_counters WHERE code_type='ORDER'",Long.class)).isEqualTo(100L);
        } finally {pool.shutdownNow();}
    }
    @Test void failsClosedWithoutTheMigratedCounterInsteadOfReturningRandomOrDuplicateCodes() {
        jdbc.update("DELETE FROM business_code_counters WHERE code_type='ORDER'");
        assertThatThrownBy(()->codes.next(BusinessCodeSequence.Kind.ORDER)).isInstanceOf(org.springframework.dao.EmptyResultDataAccessException.class);
    }
}
