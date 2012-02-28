package com.linkedin.grails.profiler;

/**
 * An appender which logs profiler events to something real, such as
 * a database, or a string in memory. An appender does not have to be
 * thread-safe, but if it isn't, it must be scoped to "prototype" for
 * use with the {@link DefaultProfilerLog}.
 */
public interface ProfilerAppender {
	/**
	 * Called on entry to a method, action, or whatever.
	 * @param label An identifier for the current profiling hierarchy,
	 * e.g. the name of the current thread.
	 * @param clazz The class hosting the element that is being "entered".
	 * @param name The name of the element (method, action, ...) that
	 * is being "entered".
	 * @param entryTime The entry time in milliseconds since the epoch,
	 * i.e. what System.currentTimeMillis() returns.
	 */
	void logEntry(String label, Class<?> clazz, String name, long entryTime);

	/**
	 * Called on exit from a method, action, or whatever.
	 * @param label An identifier for the current profiling hierarchy,
	 * e.g. the name of the current thread.
	 * @param clazz The class hosting the element that is being "entered".
	 * @param name The name of the element (method, action, ...) that
	 * is being "entered".
	 * @param exitTime The exit time in milliseconds since the epoch,
	 * i.e. what System.currentTimeMillis() returns.
	 */
	void logExit(String label, Class<?> clazz, String name, long exitTime);
}
