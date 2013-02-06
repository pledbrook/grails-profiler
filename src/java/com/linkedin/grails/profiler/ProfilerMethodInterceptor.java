package com.linkedin.grails.profiler;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * A Spring method interceptor that sends profiling events to a configured
 * profiler log. It basically logs method entry and exit.
 */
public class ProfilerMethodInterceptor implements MethodInterceptor {
	private ProfilerLog profiler;

	/**
	 * Wires in the profiler log to use.
	 * @param profiler
	 */
	public void setProfiler(ProfilerLog profiler) {
		this.profiler = profiler;
	}

	/**
	 * Sends profiling events before and after invoking the target method.
	 */
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		// Not interested in calls to getMetaClass().
		String methodName = methodInvocation.getMethod().getName();
		if (methodName.equals("getMetaClass")) {
			return methodInvocation.proceed();
		}

		// Handle the case where the target method is Groovy's "invokeMethod".
		// In this case, it is better to log the target of "invokeMethod",
		// rather than "invokeMethod" itself.
		if (methodName.equals("invokeMethod") &&
				methodInvocation.getMethod().getParameterTypes()[0] == String.class) {
			methodName = (String) methodInvocation.getArguments()[0];
		}

		// Log method entry.
		Class<?> clazz = ClassUtil.getRealClass(methodInvocation.getThis().getClass());
		profiler.logEntry(clazz, methodName);

		try {
			// Actually call the target method.
			return methodInvocation.proceed();
		}
		finally {
			// Now log method exit.
			profiler.logExit(clazz, methodName);
		}
	}
}
