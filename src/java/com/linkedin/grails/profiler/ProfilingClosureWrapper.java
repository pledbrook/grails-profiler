package com.linkedin.grails.profiler;

import groovy.lang.Closure;

/**
 * This is a special closure that wraps another closure and profiles
 * calls to it. This wrapper can be used in place of the target closure,
 * so it can be inserted transparently.
 */
@SuppressWarnings("rawtypes")
public class ProfilingClosureWrapper extends Closure {
	private static final long serialVersionUID = 1;

	private Closure target;
	private ProfilerLog profiler;
	private String name;
	private Class<?> targetClass;

	/**
	 * Creates a new instance that wraps the target closure and sends
	 * profiling events to the given profiler log.
	 * @param targetClass the target class
	 * @param closure The target closure to wrap.
	 * @param profiler A profiler log to send profiling events to.
	 * @param name A name to identify the closure in the profiling events.
	 */
	public ProfilingClosureWrapper(Class<?> targetClass, Closure closure, ProfilerLog profiler, String name) {
		super(closure.getDelegate(), closure.getDelegate());
		target = closure;
		this.profiler = profiler;
		this.name = name;
		this.targetClass = targetClass;
	}

	// This is the important one: logs entry and exit of the closure call.
	@Override
	public Object call(Object[] objects) {
		profiler.logEntry(targetClass, name);

		try {
			return target.call(objects);
		}
		finally {
			profiler.logExit(targetClass, name);
		}
	}

	/**
	 * Compares based on identities, but unlike the standard implementation
	 * this one will return <code>true</code> if the given object is the
	 * target closure for this wrapper as well.
	 */
	@Override
	public boolean equals(Object obj) {
		return this == obj || target == obj;
	}

	@Override
	public int hashCode() {
		return target.hashCode();
	}

	@Override
	public Closure curry(Object[] objects) {
		return new ProfilingClosureWrapper(targetClass, target.curry(objects), profiler, name);
	}

	@Override
	public boolean isCase(Object o) {
		return target.isCase(o);
	}

	@Override
	public Closure asWritable() {
		return target.asWritable();
	}

	@Override
	public Object getProperty(String property) {
		return target.getProperty(property);
	}

	@Override
	public void setProperty(String s, Object o) {
		target.setProperty(s, o);
	}

	@Override
	public int getMaximumNumberOfParameters() {
		return target.getMaximumNumberOfParameters();
	}

	@Override
	public Class<?>[] getParameterTypes() {
		return target.getParameterTypes();
	}

	@Override
	public Object getDelegate() {
		return target.getDelegate();
	}

	@Override
	public void setDelegate(Object o) {
		target.setDelegate(o);
	}

	@Override
	public int getDirective() {
		return target.getDirective();
	}

	@Override
	public void setDirective(int i) {
		target.setDirective(i);
	}

	@Override
	public int getResolveStrategy() {
		return target.getResolveStrategy();
	}

	@Override
	public void setResolveStrategy(int i) {
		target.setResolveStrategy(i);
	}
}
