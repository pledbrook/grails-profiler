package com.linkedin.grails.profiler;

import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Custom HTTP response wrapper for {@link ProfilerFilter} that knows
 * whether the profiler log output should be saved for the next request.
 */
public class ProfilerFilterResponse extends HttpServletResponseWrapper {
    private boolean saveOutput;

    public ProfilerFilterResponse(HttpServletResponse response) {
        super(response);
    }

    public void sendError(int i, String s) throws IOException {
        super.sendError(i, s);
        this.saveOutput = true;
    }

    public void sendError(int i) throws IOException {
        super.sendError(i);
        this.saveOutput = true;
    }

    public void sendRedirect(String s) throws IOException {
        super.sendRedirect(s);
        this.saveOutput = true;
    }

    /**
     * Returns <code>true</code> if the log output should be saved,
     * otherwise <code>false</code>.
     */
    public boolean getSaveOutput() {
        return this.saveOutput;
    }
}
