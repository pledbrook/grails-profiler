package com.linkedin.grails.profiler;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Custom HTTP response wrapper for {@link ProfilerFilter} that knows
 * whether the profiler log output should be saved for the next request.
 */
public class ProfilerFilterResponse extends HttpServletResponseWrapper {
	private boolean saveOutput;

	/**
	 * Constructor.
	 * @param response
	 */
	public ProfilerFilterResponse(HttpServletResponse response) {
		super(response);
	}

	@Override
	public void sendError(int i, String s) throws IOException {
		super.sendError(i, s);
		saveOutput = true;
	}

	@Override
	public void sendError(int i) throws IOException {
		super.sendError(i);
		saveOutput = true;
	}

	@Override
	public void sendRedirect(String s) throws IOException {
		super.sendRedirect(s);
		saveOutput = true;
	}

	/**
	 * @return <code>true</code> if the log output should be saved
	 */
	public boolean getSaveOutput() {
		return saveOutput;
	}
}
