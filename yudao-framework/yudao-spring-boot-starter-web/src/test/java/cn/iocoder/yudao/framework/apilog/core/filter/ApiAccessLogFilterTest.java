package cn.iocoder.yudao.framework.apilog.core.filter;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.apilog.core.interceptor.ApiAccessLogInterceptor;
import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.iocoder.yudao.framework.web.config.WebProperties;
import cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiAccessLogFilterTest {

    @Test
    void shouldSkipDisabledEndpointUsingMvcHandler() throws Exception {
        assertLogCount(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, "count", 0);
    }

    @Test
    void shouldSkipDisabledEndpointUsingInterceptorHandler() throws Exception {
        assertLogCount(ApiAccessLogInterceptor.ATTRIBUTE_HANDLER_METHOD, "count", 0);
    }

    @Test
    void shouldRecordOrdinaryEndpointUsingMvcHandler() throws Exception {
        assertLogCount(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, "normal", 1);
    }

    private void assertLogCount(String attribute, String method, int expectedCount) throws Exception {
        WebProperties properties = new WebProperties();
        new WebFrameworkUtils(properties);
        List<ApiAccessLogCreateReqDTO> logs = new ArrayList<>();
        ApiAccessLogCommonApi api = logs::add;
        ApiAccessLogFilter filter = new ApiAccessLogFilter(properties, "test", api);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin-api/test/" + method);
        request.setServletPath(request.getRequestURI());
        HandlerMethod handler = new HandlerMethod(new TestController(), TestController.class.getMethod(method));

        filter.doFilter(request, new MockHttpServletResponse(), (req, resp) -> req.setAttribute(attribute, handler));

        assertEquals(expectedCount, logs.size());
    }

    public static class TestController {

        @ApiAccessLog(enable = false)
        public void count() {
        }

        public void normal() {
        }
    }
}
