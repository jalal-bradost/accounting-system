package com.jalaldeveloper.accountingsystem.platform.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers platform-wide MVC extensions: the {@link CurrentCompany} argument resolver.
 * Activated automatically because component-scan picks up the {@code platform} package
 * from {@code accounting-container}'s {@code @SpringBootApplication}.
 */
@Configuration
public class PlatformWebMvcConfig implements WebMvcConfigurer {

    private final CurrentCompanyArgumentResolver currentCompanyArgumentResolver;

    public PlatformWebMvcConfig(CurrentCompanyArgumentResolver currentCompanyArgumentResolver) {
        this.currentCompanyArgumentResolver = currentCompanyArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentCompanyArgumentResolver);
    }
}
