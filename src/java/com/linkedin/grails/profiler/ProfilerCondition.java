package com.linkedin.grails.profiler;

/**
 * Represents an object that indicates whether profiling should occur
 * based on certain conditions.
 */
public interface ProfilerCondition {
	/**
	 * @return <code>true</code> if profiling should be performed
	 */
	boolean doProfiling();
}
