package com.linkedin.grails.profiler;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Servlet filter that triggers profiling if a particular request
 * parameter is set ("profiler").
 */
public class ProfilerFilter extends OncePerRequestFilter {

    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Determine whether we should profile this request.
        boolean doProfiling = false;
        String profilerParam = request.getParameter("profiler");

        if (profilerParam != null && (profilerParam.equals("on") || profilerParam.equals("1"))) {
            doProfiling = true;
        }

        // If we are profiling, mark the request as such and log the
        // start time for this request.
        ProfilerLog profiler = null;
        if (doProfiling) {
            // Fetch the currently configured logger for profiling from
            // the application context.
            WebApplicationContext appContext =
                    WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
            profiler = (ProfilerLog) appContext.getBean("profilerLog");

            // Configure the profiler to log the start and end times.
            profiler.startProfiling("uri: " + request.getRequestURI());

            // Start time.
            profiler.logEntry(getClass(), "Web Request");
        }

        // Pass execution on to the next filter.
        try {
            filterChain.doFilter(request, response);
        }
        finally {
            // End time.
            if (doProfiling) {
                profiler.logExit(getClass(), "Web Request");
                profiler.stopProfiling();
            }
        }
    }
}
