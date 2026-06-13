package com.chobolevel.logging.config;

import com.chobolevel.logging.filter.MdcTraceFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnMissingBean(MdcTraceFilter.class)
class WebFilterConfiguration {

    @Bean
    public MdcTraceFilter mdcTraceFilter() {
        return new MdcTraceFilter();
    }
}