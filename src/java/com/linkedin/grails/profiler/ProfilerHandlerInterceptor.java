package com.linkedin.grails.profiler;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handler interceptor that profiles the current HTTP request. This is
 * particularly useful for profiling the view.
 */
public class ProfilerHandlerInterceptor extends HandlerInterceptorAdapter {
    /**
     * The name of the request attribute that holds the current status
     * of the request. This will be "Controller" before the controller
     * action is invoked, and "View" afterwards.
     */
    public static final String REQUEST_STATUS_ATTR = "com.linkedin.grails.profiler.STATUS";

    private ProfilerLog profiler;

    /**
     * Wires in the profiler log to use.
     */
    public void setProfiler(ProfilerLog profiler) {
        this.profiler = profiler;
    }

    /**
     * Called before the controller action is invoked, this method logs
     * an "entry" profiling event for "Controller".
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) throws Exception {
        this.profiler.logEntry(getClass(), "Controller");
        request.setAttribute(REQUEST_STATUS_ATTR, "Controller");
        return true;
    }

    /**
     * Called after the controller action has been invoked, but before
     * the view has been rendered, this method logs an "exit" profiling
     * event for "Controller" and an "entry" profiling event for "View".
     * Note that this method may not actually be called in the case of
     * a redirect or similar.
     */
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object o,
            ModelAndView modelAndView) throws Exception {
        this.profiler.logExit(getClass(), "Controller");
        this.profiler.logEntry(getClass(), "View");
        request.setAttribute(REQUEST_STATUS_ATTR, "View");
    }

    /**
     * Called after the request has finished, this method logs an "exit"
     * profiling event for either "Controller" or "View" depending on
     * what the current request status is.
     */
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object o,
            Exception e) throws Exception {
        String status = (String) request.getAttribute(REQUEST_STATUS_ATTR);
        if (status != null) {
            this.profiler.logExit(getClass(), status);
        }
    }
}
