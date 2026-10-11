package com.example.eshop.order;
import com.example.eshop.order.automation.OrderTransitions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class OrderTransitionsTest {
    @Test void cannotSkipFulfilmentSteps() {assertThatThrownBy(()->OrderTransitions.require("PENDING","DELIVERED",true)).isInstanceOf(RuntimeException.class);}
    @Test void cannotReviveACancelledOrder() {assertThatThrownBy(()->OrderTransitions.require("CANCELLED","PROCESSING",true)).isInstanceOf(RuntimeException.class);}
    @Test void cannotManuallyConfirmUnverifiedOnlinePayment() {assertThatThrownBy(()->OrderTransitions.require("PENDING","CONFIRMED",false)).isInstanceOf(RuntimeException.class);}
    @Test void permitsValidFulfilmentAndIdempotentStatus() {assertThatCode(()->{OrderTransitions.require("PROCESSING","SHIPPED",true);OrderTransitions.require("SHIPPED","DELIVERED",true);OrderTransitions.require("DELIVERED","DELIVERED",true);}).doesNotThrowAnyException();}
}
