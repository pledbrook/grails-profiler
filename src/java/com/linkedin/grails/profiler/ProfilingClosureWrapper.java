package com.linkedin.grails.profiler;

import groovy.lang.Closure;

/**
 * This is a special closure that wraps another closure and profiles
 * calls to it. This wrapper can be used in place of the target closure,
 * so it can be inserted transparently.
 */
public class ProfilingClosureWrapper extends Closure {
    private Closure target;
    private ProfilerLog profiler;
    private String name;
	private Class targetClass;

    /**
     * Creates a new instance that wraps the target closure and sends
     * profiling events to the given profiler log.
     * @param closure The target closure to wrap.
     * @param profiler A profiler log to send profiling events to.
     * @param name A name to identify the closure in the profiling
     * events.
     */
    public ProfilingClosureWrapper(Class targetClass, Closure closure, ProfilerLog profiler, String name) {
        super(closure.getDelegate(), closure.getDelegate());
        this.target = closure;
        this.profiler = profiler;
        this.name = name;
		this.targetClass = targetClass;
    }

    /**
     * This is the important one: logs entry and exit of the closure
     * call.
     */
    public Object call(Object[] objects) {
        this.profiler.logEntry(targetClass, this.name);

        try {
            return this.target.call(objects);
        }
        finally {
            this.profiler.logExit(targetClass, this.name);
        }
    }

    /**
     * Compares based on identities, but unlike the standard implementation
     * this one will return <code>true</code> if the given object is the
     * target closure for this wrapper as well.
     */
    public boolean equals(Object obj) {
        return this == obj || this.target == obj;
    }

    public int hashCode() {
        return this.target.hashCode();
    }

    public Closure curry(Object[] objects) {
        return new ProfilingClosureWrapper(targetClass, this.target.curry(objects), this.profiler, this.name);
    }

    public boolean isCase(Object o) {
        return this.target.isCase(o);
    }

    public Closure asWritable() {
        return this.target.asWritable();
    }

    public Object getProperty(String property) {
        return this.target.getProperty(property);
    }

    public void setProperty(String s, Object o) {
        this.target.setProperty(s, o);
    }

    public int getMaximumNumberOfParameters() {
        return this.target.getMaximumNumberOfParameters();
    }

    public Class[] getParameterTypes() {
        return this.target.getParameterTypes();
    }

    public Object getDelegate() {
        return this.target.getDelegate();
    }

    public void setDelegate(Object o) {
        this.target.setDelegate(o);
    }

    public int getDirective() {
        return this.target.getDirective();
    }

    public void setDirective(int i) {
        this.target.setDirective(i);
    }

    public int getResolveStrategy() {
        return this.target.getResolveStrategy();
    }

    public void setResolveStrategy(int i) {
        this.target.setResolveStrategy(i);
    }
}
