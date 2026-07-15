package com.abc.studentportal.common.api;

import com.abc.studentportal.common.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashSet;
import java.util.Set;

final class StrictQueryParameterInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method))
            return true;

        Set<String> allowed = new LinkedHashSet<>();
        for (MethodParameter parameter : method.getMethodParameters()) {
            RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
            if (annotation == null)
                continue;
            String name = annotation.name().isBlank() ? annotation.value() : annotation.name();
            if (name.isBlank())
                name = parameter.getParameterName();
            if (name != null)
                allowed.add(name);
        }

        Set<String> unsupported = new LinkedHashSet<>(request.getParameterMap().keySet());
        unsupported.removeAll(allowed);
        if (!unsupported.isEmpty())
            throw new InvalidRequestException("Unsupported query parameter(s): " + String.join(", ", unsupported));
        return true;
    }

}
