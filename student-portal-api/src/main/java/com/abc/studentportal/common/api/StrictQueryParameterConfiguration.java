package com.abc.studentportal.common.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile({"local-dynamodb", "test-dynamodb"})
class StrictQueryParameterConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new StrictQueryParameterInterceptor()).addPathPatterns("/api/v1/**");
    }

}
