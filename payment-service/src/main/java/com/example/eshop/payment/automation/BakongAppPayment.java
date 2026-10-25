package com.example.eshop.payment.automation;

import com.example.eshop.common.audit.*;
import com.example.eshop.payment.bakong.service.BakongTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

/** A handoff only: a provider link or app return can never verify a payment. */
@Service
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name="automation.enabled", havingValue="true")
public class BakongAppPayment {
    private final PaymentAutomation payments;
    private final RestClient restClient;
    private final BakongTokenService tokens;
    private final AuditLogService audit;
    private final org.springframework.transaction.support.TransactionTemplate transactions;
    @Value("${bakong.app-payment.enabled:false}") private boolean enabled;
    @Value("${bakong.app-payment.source-info-enabled:true}") private boolean sourceInfoEnabled = true;
    @Value("${bakong.base-url}") private String provider;
    @Value("${bakong.app-payment.return-origin:}") private String returnOrigin;
    @Value("${bakong.app-payment.allowed-link-hosts:}") private String allowedHosts;
    @Value("${bakong.app-icon-url:}") private String icon;
    @Value("${bakong.app-name:E-Shop}") private String name;

    public Map<String,Object> launch(long orderId) {
        // Ownership, amount, currency and the current authoritative order are checked first.
        var payment=payments.customerAppQr(orderId);
        if (!"PENDING".equals(payment.get("status")) || !"PENDING".equals(payment.get("orderStatus"))
                || !(payment.get("expiresAt") instanceof Instant expires) || !expires.isAfter(Instant.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This payment is no longer payable. Check its status.");
        if(!enabled) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Bakong app payment is not configured. Scan the KHQR instead.");
        String callback;
        Set<String> hosts=new HashSet<>();
        try {
            URI origin=https(returnOrigin);
            if(!Set.of("", "/").contains(origin.getPath()) || origin.getQuery()!=null)
                throw new IllegalArgumentException();
            if(sourceInfoEnabled) https(icon);
            https(provider);
            for(String host:allowedHosts.split(",")) if(!host.isBlank()) hosts.add(host.trim().toLowerCase(Locale.ROOT));
            if(hosts.isEmpty()) throw new IllegalArgumentException();
            callback=returnOrigin.replaceAll("/+$", "")+"/checkout/payment/"+orderId+"?bakongReturn=1";
        } catch(RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Bakong app payment configuration is incomplete. Scan the KHQR instead.");
        }
        try {
            var body=new LinkedHashMap<String,Object>();
            body.put("qr",payment.get("qr"));
            if(sourceInfoEnabled)body.put("sourceInfo",Map.of("appName",name,"appIconUrl",icon,"appDeepLinkCallback",callback));
            JsonNode response=restClient.post().uri(provider.replaceAll("/+$", "")+"/v1/generate_deeplink_by_qr")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION,"Bearer "+tokens.getToken())
                    .body(body)
                    .retrieve().body(JsonNode.class);
            if(response==null || response.path("responseCode").asInt(-1)!=0
                    || !response.path("errorCode").isMissingNode() && !response.path("errorCode").isNull() && response.path("errorCode").asInt(-1)!=0)
                throw new IllegalStateException();
            String link=response.path("data").path("shortLink").asText("");
            URI target=https(link);
            if(!hosts.contains(target.getHost().toLowerCase(Locale.ROOT))) throw new IllegalStateException();
            transactions.executeWithoutResult(status -> audit.record(new AuditEvent(AuditAction.UPDATE,"PAYMENT",payment.get("paymentId").toString(),null,
                    Map.of("action","BAKONG_APP_HANDOFF","orderId",orderId),AuditResult.SUCCESS,null)));
            var result=new LinkedHashMap<>(payment);
            result.remove("qr");
            result.put("deepLink",link);result.put("returnUrl",callback);result.put("automaticReturn",sourceInfoEnabled);
            return result;
        } catch(Exception ex) {
            // Log only the failure class, never provider payloads, links or credentials.
            log.warn("Bakong app handoff failed orderId={} failureType={}",orderId,ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Bakong app link is unavailable. Scan the existing KHQR instead.");
        }
    }
    private static URI https(String value) {
        URI uri=URI.create(value);
        if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null)
            throw new IllegalArgumentException();
        return uri;
    }
}
