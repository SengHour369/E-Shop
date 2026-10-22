package com.example.eshop.order.guest;
import com.example.eshop.order.repository.OrderRepository;
import com.example.eshop.order.automation.PaymentAutomationClient;
import com.example.eshop.common.notification.GuestOrderNotice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.Objects;

@RestController @RequiredArgsConstructor
public class GuestNoticeController {
    private final OrderRepository orders;
    private final ObjectProvider<PaymentAutomationClient> payments;
    @GetMapping("/internal/commerce/guest-orders/{id}/notice")
    @PreAuthorize("hasAuthority('SERVICE_NOTIFICATION') and authentication.name == 'auth-service'")
    @Transactional(readOnly=true)
    public GuestOrderNotice notice(@PathVariable long id) {
        var order=orders.findById(id).filter(o->o.getGuestSessionId()!=null).orElseThrow();
        boolean verified=false;
        if(order.getPaymentId()!=null&&payments.getIfAvailable()!=null) {
            var p=payments.getObject().get(order.getPaymentId());
            verified=p!=null&&p.orderId()==id&&p.userId()==null&&"SUCCESS".equals(p.status())&&p.verifiedAt()!=null
                    &&p.providerHash()!=null&&p.providerHash().matches("[a-fA-F0-9]{64}")&&p.amount()!=null&&p.amount().compareTo(order.getTotalAmount())==0;
        }
        return new GuestOrderNotice(order.getVerifiedContactEmail(),order.getOrderNumber(),order.getStatus(),order.getTotalAmount(),"USD",verified);
    }
}
