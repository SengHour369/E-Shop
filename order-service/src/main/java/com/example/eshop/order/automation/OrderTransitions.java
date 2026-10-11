package com.example.eshop.order.automation;
import java.util.Map;
import java.util.Set;
import com.example.eshop.common.exception.BusinessLogicException;
public final class OrderTransitions {
    private OrderTransitions() {}
    private static final Map<String,Set<String>> NEXT=Map.of(
        "PENDING",Set.of("CANCELLED","CONFIRMED"),
        "CONFIRMED",Set.of("PROCESSING"),"PROCESSING",Set.of("SHIPPED"),"SHIPPED",Set.of("DELIVERED"));
    public static void require(String from,String to,boolean verifiedOnline) {
        if(from.equals(to)) return;
        if(!NEXT.getOrDefault(from,Set.of()).contains(to) || "PENDING".equals(from) && "CONFIRMED".equals(to) && !verifiedOnline)
            throw new BusinessLogicException("Invalid order transition or missing payment verification");
    }
}
