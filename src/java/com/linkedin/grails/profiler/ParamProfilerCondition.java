package com.linkedin.grails.profiler;

import org.springframework.web.context.request.RequestContextHolder;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

/**
 * A profiler condition that checks whether the value of a particular
 * request parameter lies within a set of acceptable values. This can
 * only be used after the GrailsWebRequestFilter has done its stuff.
 */
public class ParamProfilerCondition implements ProfilerCondition {
    private String paramName;
    private Set values;

    /**
     * Returns the name of the request parameter that determines whether
     * profiling should occur.
     */
    public String getParamName() {
        return this.paramName;
    }

    /**
     * Sets the name of the request parameter that determines whether
     * profiling should occur.
     */
    public void setParamName(String name) {
        this.paramName = name;
    }

    /**
     * Returns the set of allowed values for the named parameter that
     * will switch profiling on.
     */
    public Set getValues() {
        return Collections.unmodifiableSet(this.values);
    }

    /**
     * Initialises or updates the set of allowed values for the named
     * parameter that will switch profiling on.
     */
    public void setValues(Set values) {
        this.values = new HashSet(values);
    }

    /**
     * Returns <code>true</code> if the value of the named parameter
     * for the current request matches any of the values in the set.
     */
    public boolean doProfiling() {
        // Get the required parameter from the current request.
        GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpRequest = webRequest.getCurrentRequest();
        String profilerParam = httpRequest.getParameter(this.paramName);

        // Compare it the values that switch profiling on.
        return this.values.contains(profilerParam);
    }
}
