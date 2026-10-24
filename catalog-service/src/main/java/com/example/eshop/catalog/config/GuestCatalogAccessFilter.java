package com.example.eshop.catalog.config;

import com.example.eshop.common.storefront.StorefrontAccessClient;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

/** Legacy public browsing endpoints obey the same effective switch as the storefront API. */
public class GuestCatalogAccessFilter extends OncePerRequestFilter {

    private final StorefrontAccessClient access;

    public GuestCatalogAccessFilter(StorefrontAccessClient access) {
        this.access = access;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = auth != null && auth.isAuthenticated();
        boolean catalog = path.startsWith("/api/v1/products/")
                || path.startsWith("/api/v1/categories/")
                || path.startsWith("/api/v1/subcategories/")
                || path.startsWith("/api/v1/promotions/");

        if (catalog && !signedIn) {
            try {
                if (!access.current().guestBrowsing()) {
                    response.sendError(403);
                    return;
                }
            } catch (Exception e) {
                response.sendError(503);
                return;
            }
        }

        chain.doFilter(request, response);
    }
}