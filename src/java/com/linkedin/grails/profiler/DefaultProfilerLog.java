package com.linkedin.grails.profiler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * Default implementation of the profiler log that logs the events to
 * a set of appenders, similar to the way Log4J works. This class is
 * thread-safe and collates profiling information on a per-thread basis.
 */
public class DefaultProfilerLog implements ProfilerLog, ApplicationContextAware {
	private ApplicationContext applicationContext;

	/**
	 * A list of the names of the appender beans to use. Note that this
	 * list cannot be modified after the bean has been initialised.
	 */
	private List<String> appenderNames;

	private ThreadLocal<String> profilingLabel = new ThreadLocal<String>();
	private ThreadLocal<List<ProfilerAppender>> appenders = new ThreadLocal<List<ProfilerAppender>>();

	/**
	 * Stores the application context used to load this bean. This should
	 * not be called once the bean has been instantiated and initialised.
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Sets which appenders should be used by this profiler log. The given list
	 * contains the bean names for the corresponding appenders. This should
	 * not be called once the profiler log bean has been instantiated and initialised.
	 * @param appenderNames the names
	 */
	public void setAppenderNames(List<String> appenderNames) {
		this.appenderNames = appenderNames;
	}

	/**
	 * Starts profiling on the current thread, using the given label
	 * as an identifier for the current thread's profiling information.
	 * Once this has been called, {@link #stopProfiling()} must be
	 * called before the current thread finishes, otherwise if the
	 * thread is part of a pool, the current profiling information
	 * will still be there when the thread is re-used.
	 */
	public void startProfiling(String label) {
		Assert.notNull(label, "Label cannot be null");

		// Do we have any appenders on the current thread? If not, get them now.
		if (appenders.get() == null) {
			// Create a list for all the appenders that we need.
			List<ProfilerAppender> localAppenders = new ArrayList<ProfilerAppender>(appenderNames.size());

			// Now populate the list by fetching the appenders from the
			// application context. Note that appenders that are not
			// thread-safe should be of "prototype" scope.
			for (String appenderName : appenderNames) {
				ProfilerAppender appender = (ProfilerAppender) applicationContext.getBean(appenderName);
				localAppenders.add(appender);
			}

			// Now store the list of appenders locally to the thread.
			appenders.set(localAppenders);
		}

		profilingLabel.set(label);
	}

	/**
	 * Stops the profiling and clears the thread-local data.
	 */
	public void stopProfiling() {
		// Clear the list of appenders and the label.
		appenders.set(null);
		profilingLabel.set(null);
	}

	/**
	 * Returns whether profiling is currently on or not.
	 */
	public boolean isProfiling() {
		return profilingLabel.get() != null;
	}

	/**
	 * Logs an entry event if profiling is currently on, and passes it
	 * through to all configured appenders.
	 * @param clazz The class that the event relates to.
	 * @param name The name of the method, action, or whatever that is being "entered".
	 */
	public void logEntry(Class<?> clazz, String name) {
		// Only log the even if profiling is on.
		if (!isProfiling()) {
			return;
		}

		// Save the current time and then log the event to all the configured appenders.
		long entryTime = System.currentTimeMillis();
		for (ProfilerAppender appender : appenders.get()) {
			appender.logEntry(profilingLabel.get(), clazz, name, entryTime);
		}
	}

	/**
	 * Logs an exit event if profiling is currently on, and passes it
	 * through to all configured appenders.
	 * @param clazz The class that the event relates to.
	 * @param name The name of the method, action, or whatever that is being "exited".
	 */
	public void logExit(Class<?> clazz, String name) {
		// Only log the even if profiling is on.
		if (!isProfiling()) {
			return;
		}

		// Save the current time and then log the event to all the configured appenders.
		long exitTime = System.currentTimeMillis();
		for (ProfilerAppender appender : appenders.get()) {
			appender.logExit(profilingLabel.get(), clazz, name, exitTime);
		}
	}
}
