package com.chobolevel.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTraceFilterTest {

    private MdcTraceFilter filter;

    @BeforeEach
    void setUp() {
        filter = new MdcTraceFilter();
        MDC.clear();
    }

    @Test
    void doFilter_withTraceHeader_usesSameTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcTraceFilter.TRACE_HEADER, "given-trace-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, res) -> capturedTraceId[0] = MDC.get(MdcTraceFilter.MDC_TRACE_ID);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isEqualTo("given-trace-id");
        assertThat(response.getHeader(MdcTraceFilter.TRACE_HEADER)).isEqualTo("given-trace-id");
    }

    @Test
    void doFilter_withoutTraceHeader_generatesUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedTraceId = new String[1];
        FilterChain chain = (req, res) -> capturedTraceId[0] = MDC.get(MdcTraceFilter.MDC_TRACE_ID);

        filter.doFilter(request, response, chain);

        assertThat(capturedTraceId[0]).isNotBlank();
        assertThat(response.getHeader(MdcTraceFilter.TRACE_HEADER)).isNotBlank();
    }

    @Test
    void doFilter_clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(MdcTraceFilter.TRACE_HEADER, "trace-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(MdcTraceFilter.MDC_TRACE_ID)).isNull();
    }
}