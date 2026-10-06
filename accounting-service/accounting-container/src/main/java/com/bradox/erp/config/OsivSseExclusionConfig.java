package com.bradox.erp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;

/**
 * If OSIV is re-enabled, still skip binding a persistence context on the stock-hold SSE stream
 * so long-lived connections cannot exhaust the Hikari pool.
 */
@Configuration
@ConditionalOnProperty(name = "spring.jpa.open-in-view", havingValue = "true")
public class OsivSseExclusionConfig {

    @Bean
    public FilterRegistrationBean<OpenEntityManagerInViewFilter> openEntityManagerInViewFilterExcludingSse() {
        OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                String uri = request.getRequestURI();
                return uri != null && uri.contains("/field-stock-holds/stream");
            }
        };
        filter.setEntityManagerFactoryBeanName("entityManagerFactory");
        FilterRegistrationBean<OpenEntityManagerInViewFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setName("openEntityManagerInViewFilterExcludingSse");
        reg.addUrlPatterns("/api/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return reg;
    }
}
