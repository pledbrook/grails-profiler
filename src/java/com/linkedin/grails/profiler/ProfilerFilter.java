package com.linkedin.grails.profiler;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet filter that triggers profiling if a particular request
 * parameter is set ("profiler").
 */
public class ProfilerFilter extends OncePerRequestFilter {
    public static final String SAVED_OUTPUT_KEY = "com.linkedin.grails.profiler.saved_output";

    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Let's start with the Spring ApplicationContext for this web
        // app.
        WebApplicationContext appContext =
                WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

        // Retrieve the bean that determines whether profiling should
        // occur for the current request or not.
        ProfilerCondition conditionBean = (ProfilerCondition) appContext.getBean("profilerCondition");

        // Check for any log output stored in the session. If yes, we
        // continue profiling regardless. Note that we don't create
        // the session - it's best to leave this until we know that
        // we are actually profiling.
        Object output = null;
        HttpSession session = request.getSession(false);

        if (session != null) {
            // The session exists, so retrieve the saved log output if
            // it's there.
            output = session.getAttribute(SAVED_OUTPUT_KEY);

            // We now have a reference to the output if it exists, so
            // we can remove the attribute from the session.
            session.removeAttribute(SAVED_OUTPUT_KEY);
        }

        // Determine whether we should profile this request.
        boolean doProfiling = (output != null) || conditionBean.doProfiling();

        // If we are profiling, mark the request as such and log the
        // start time for this request.
        ProfilerLog profiler = null;
        RequestBufferedAppender appender = null;
        if (doProfiling) {
            // Since we are profiling, create a session.
            session = request.getSession(true);

            // Fetch the currently configured logger for profiling from
            // the application context.
            profiler = (ProfilerLog) appContext.getBean("profilerLog");

            // Check whether there is any saved output from a previous
            // request.
            appender = (RequestBufferedAppender) appContext.getBean("bufferedAppender");
            if (output instanceof String) {
                // Add the saved output to the request buffered appender.
                appender.prependOutput((String) output);
            }

            // Configure the profiler to log the start and end times.
            profiler.startProfiling("uri: " + request.getRequestURI());

            // Start time.
            profiler.logEntry(getClass(), "Web Request");
        }

        // Pass execution on to the next filter.
        ProfilerFilterResponse filterResponse = new ProfilerFilterResponse(response);
        try {
            filterChain.doFilter(request, filterResponse);
        }
        finally {
            // End time.
            if (doProfiling) {
                profiler.logExit(getClass(), "Web Request");
                profiler.stopProfiling();

                // Only errors and redirects should require us to save
                // the log output for the next request.
                if (filterResponse.getSaveOutput()) {
                    // Save the log output to the session.
                    session.setAttribute(SAVED_OUTPUT_KEY, appender.getOutput());
                }
            }
        }
    }
}
