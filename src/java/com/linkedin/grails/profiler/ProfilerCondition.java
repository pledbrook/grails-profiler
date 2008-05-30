package com.linkedin.grails.profiler;

import javax.servlet.http.HttpServletRequest;

/**
 * Represents an object that indicates whether profiling should occur
 * based on certain conditions.
 */
public interface ProfilerCondition {
    /**
     * Returns <code>true</code> if profiling should be performed,
     * otherwise <code>false</code>.
     */
    boolean doProfiling();
}
