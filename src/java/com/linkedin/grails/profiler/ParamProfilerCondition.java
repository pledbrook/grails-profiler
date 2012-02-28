package com.linkedin.grails.profiler;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * A profiler condition that checks whether the value of a particular
 * request parameter lies within a set of acceptable values. This can
 * only be used after the GrailsWebRequestFilter has done its stuff.
 */
public class ParamProfilerCondition implements ProfilerCondition {
	private String paramName;
	private Set<String> values;

	/**
	 * Returns the name of the request parameter that determines whether profiling should occur.
	 * @return the name
	 */
	public String getParamName() {
		return paramName;
	}

	/**
	 * Sets the name of the request parameter that determines whether profiling should occur.
	 * @param name the name
	 */
	public void setParamName(String name) {
		paramName = name;
	}

	/**
	 * Returns the set of allowed values for the named parameter that will switch profiling on.
	 * @return the values
	 */
	public Set<String> getValues() {
		return Collections.unmodifiableSet(values);
	}

	/**
	 * Initialises or updates the set of allowed values for the named
	 * parameter that will switch profiling on.
	 * @param values the values
	 */
	public void setValues(Set<String> values) {
		this.values = new HashSet<String>(values);
	}

	/**
	 * Returns <code>true</code> if the value of the named parameter
	 * for the current request matches any of the values in the set.
	 */
	public boolean doProfiling() {
		// Get the required parameter from the current request.
		GrailsWebRequest webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
		HttpServletRequest httpRequest = webRequest.getCurrentRequest();
		String profilerParam = httpRequest.getParameter(paramName);

		// Compare it the values that switch profiling on.
		return values.contains(profilerParam);
	}
}
