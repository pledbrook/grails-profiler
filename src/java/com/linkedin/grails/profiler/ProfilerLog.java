package com.linkedin.grails.profiler;

/**
 * A logger for profiling events.
 */
public interface ProfilerLog {

	/**
	 * Turns profiling on and associates the given label with the
	 * resulting profiling information.
	 * @param label the label
	 */
	void startProfiling(String label);

	/**
	 * Turns the profiling off.
	 */
	void stopProfiling();

	/**
	 * Indicates whether profiling is currently on or off.
	 * @return <code>true</code> if profiling is currently on
	 */
	boolean isProfiling();

	/**
	 * Logs an entry event.
	 * @param clazz The class that the event relates to.
	 * @param name A name identifying the element within the class that
	 * the event relates to. This could be, for example, a method or an
	 * action name.
	 */
	void logEntry(Class<?> clazz, String name);

	/**
	 * Logs an exit event.
	 * @param clazz The class that the event relates to.
	 * @param name A name identifying the element within the class that
	 * the event relates to. This could be, for example, a method or an
	 * action name.
	 */
	void logExit(Class<?> clazz, String name);
}
