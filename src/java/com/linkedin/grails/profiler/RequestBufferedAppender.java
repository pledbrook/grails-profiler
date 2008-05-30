package com.linkedin.grails.profiler;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A request-based appender that stores the log entries in a StringBuilder.
 * The buffered data can be retrieved at any time by calling {@link
 * #getOutput()}. Note that this appender is safe to use outside of a
 * web request, but it will do nothing.
 */
public class RequestBufferedAppender implements ProfilerAppender {
    /** Attribute name for the request info. */
    private static final String INFO_ATTR = "com.linkedin.grails.profile.BufferedAppenderInfo";

    /**
     * Logs the entry to a StringBuilder, indenting the message based
     * on the hierarchy of entries and exits for the current request.
     * If this is called outside of a request, nothing happens.
     */
    public void logEntry(String label, Class clazz, String name, long entryTime) {
        // Mark the start time, log the entry, and bump the indent
        // level.
        RequestInfo info = getRequestInfo();
        if (info != null) {
            info.logStart(entryTime);
            info.log("Entering " + getIdentity(label, clazz, name));
            info.incrementIndent();
        }
    }

    /**
     * Logs the exit to a StringBuilder, indenting the message based
     * on the hierarchy of entries and exits for the current request.
     * The message includes the time elapsed since the corresponding
     * entry log. If this is called outside of a request, nothing
     * happens.
     */
    public void logExit(String label, Class clazz, String name, long exitTime) {
        // Get the buffer and other info for this request.
        RequestInfo info = getRequestInfo();
        if (info != null) {
            // Calculate the total time taken.
            long totalTime = exitTime - info.getStartTime();

            // Decrease the indent level and log the message (including
            // time taken).
            info.decrementIndent();
            info.log("Exiting " + getIdentity(label, clazz, name) + "   (Time: " + totalTime + ")");
        }
    }

    /**
     * Adds some text before the start of the current logging output.
     * Used mainly to include output from previous requests. An extra
     * new-line is added after the given text to separate it from the
     * current log output.
     * @param text The text to prepend to the log output.
     */
    public void prependOutput(String text) {
        RequestInfo info = getRequestInfo();
        if (info != null) {
            info.prependText(text + '\n');
        }
    }

    /**
     * Returns the output that is currently buffered, or <code>null</code>
     * if this is called from outside of a web request or no events
     * have been logged yet.
     */
    public String getOutput() {
        GrailsWebRequest webRequest = getWebRequest();
        if (webRequest != null) {
            RequestInfo info = (RequestInfo) webRequest.getAttribute(INFO_ATTR, RequestAttributes.SCOPE_REQUEST);
            return info == null ? null : info.getOutput();
        }
        else {
            return null;
        }
    }

    /**
     * Returns the current web request, or <code>null</code> if this
     * is called outside of a web request.
     */
    private GrailsWebRequest getWebRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        return requestAttributes == null ? null : (GrailsWebRequest) requestAttributes;
    }

    /**
     * Gets the RequestInfo instance for the current request. If this
     * method is called outside of a request, it returns <code>null</code>.
     * Otherwise, it returns a RequestInfo instance, creating one if
     * this is the first time the method has been called for the current
     * request.
     */
    private RequestInfo getRequestInfo() {
        RequestInfo info = null;
        GrailsWebRequest webRequest = getWebRequest();
        if (webRequest != null) {
            // Get the buffer and other info for this request.
            Object infoAttr = webRequest.getAttribute(INFO_ATTR, RequestAttributes.SCOPE_REQUEST);
            if (infoAttr == null) {
                // There is no stored info yet, so create it and add
                // it to the request as an attribute.
                infoAttr = new RequestInfo();
                webRequest.setAttribute(INFO_ATTR, infoAttr, RequestAttributes.SCOPE_REQUEST);
            }

            info = (RequestInfo) infoAttr;
        }

        return info;
    }

    /**
     * Returns an identity string based on a label, class, and element
     * name. This should be unique for any given instance of the appender,
     * but there are no guarantees.
     */
    private String getIdentity(String label, Class clazz, String name) {
        return "[" + label + "] " + clazz.getName() + ":" + name;
    }

    /**
     * Used to store a per-request StringBuilder, indent level, and
     * start times.
     */
    private static class RequestInfo {
        private StringBuilder buffer;
        private int indent;
        private List<Long> startTimes;

        public RequestInfo() {
            this.buffer = new StringBuilder();
            this.startTimes = new ArrayList<Long>();
        }

        /**
         * Appends the given message to the StringBuilder with the
         * appropriate indent. A trailing newline is added too.
         */
        public void log(String message) {
            for (int i = 0; i < this.indent; i++) {
                this.buffer.append("  ");
            }
            this.buffer.append(message).append('\n');
        }

        /**
         * Increases the current indent by one.
         */
        public void incrementIndent() {
            this.indent++;
        }

        /**
         * Decreases the current indent by one.
         */
        public void decrementIndent() {
            this.indent--;
        }

        /**
         * Returns the current indent.
         */
        public int getIndent() {
            return this.indent;
        }

        /**
         * Marks a start time for later retrieval by {@link #getStartTime()}.
         * The time is given in milliseconds.
         */
        public void logStart(long startTime) {
            this.startTimes.add(startTime);
        }

        /**
         * Returns the last start time logged by {@link #logStart(long)},
         * and removes it. The next call to this method will then return
         * the previous start time. {@link #logStart(long)} and {@link
         * #getStartTime()} basically work as the push() and pop()
         * methods of a stack.
         */
        public Long getStartTime() {
            return this.startTimes.remove(this.startTimes.size() - 1);
        }

        /**
         * Returns the currently buffered output as a string.
         */
        public String getOutput() {
            return this.buffer.toString();
        }

        /**
         * Prepends the given text to the output buffer.
         */
        public void prependText(String text) {
            this.buffer.insert(0, text);
        }
    }
}
