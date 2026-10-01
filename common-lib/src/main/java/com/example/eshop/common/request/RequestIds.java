package com.example.eshop.common.request;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

public final class RequestIds {
    public static final String HEADER = "X-Request-ID";
    public static final String MDC_KEY = "requestId";
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private RequestIds() {}
    public static String resolve(String value) {
        return value != null && SAFE.matcher(value).matches() ? value : "req_" + UUID.randomUUID();
    }
    public static String current() { return MDC.get(MDC_KEY); }
}
