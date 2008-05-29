package com.linkedin.grails.profiler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Appender for the profiler log that writes the entry and exit messages
 * to a named logger. This is not a thread-safe class, and so it should
 * be scoped "prototype" when used with {@link DefaultProfilerLog}.
 */
public class LoggingAppender implements ProfilerAppender {
    public static final String LOGGER_NAME = "com.linkedin.grails.ProfilerPlugin";

    /** Map of identities (such as methods) to start times. */
    private Map<String, Long> startTimes = new HashMap<String, Long>();

    /** Current indent level for the log messages. */
    private int indentLevel;

    /** Padding string to use for indent. */
    private String padding = "  ";

    /**
     * Sets the padding string to use when building the indent for a
     * log message.
     */
    public void setPadding(String padding) {
        this.padding = padding;
    }

    /**
     * Writes an INFO message to the logger, but does not include the
     * entry time.
     * @param label An identifier for the current profile hierarchy.
     * @param clazz The class that the "entry" refers to.
     * @param name A name associated with the class that identifies
     * what is being entered, e.g. a method or action name.
     * @param entryTime The entry time in milliseconds since the epoch,
     * i.e. what System.currentTimeMillis() returns.
     */
    public void logEntry(String label, Class clazz, String name, long entryTime) {
        Log log = LogFactory.getLog(LOGGER_NAME);

        // Get the identifier for this log entry.
        String identity = getIdentity(label, clazz, name);

        // Save the start time against the identity. The identity should
        // be unique for the appender.
        this.startTimes.put(identity, entryTime);

        // Log the message.
        log.info(getIndent() + "Entering " + identity);

        // Increase the indent of the next entry log message.
        this.indentLevel++;
    }

    /**
     * Writes an INFO message to the logger that includes the total
     * time taken for execution of the element (method, action, or
     * whatever).
     * @param label An identifier for the current profile hierarchy.
     * @param clazz The class that the "exit" refers to.
     * @param name A name associated with the class that identifies
     * what is being exited, e.g. a method or action name.
     * @param exitTime The exit time in milliseconds since the epoch,
     * i.e. what System.currentTimeMillis() returns.
     */
    public void logExit(String label, Class clazz, String name, long exitTime) {
        Log log = LogFactory.getLog(LOGGER_NAME);

        // Descrease the indent for this log message.
        this.indentLevel--;

        // Get the identifier for this log entry.
        String identity = getIdentity(label, clazz, name);

        // Calculate the total time taken.
        long startTime = this.startTimes.get(identity);
        long totalTime = exitTime - startTime;

        // Log the message.
        log.info(getIndent() + "Exiting " + identity + "   (Time: " + totalTime + ")");
    }

    /**
     * Returns the current indent string to use, based on the current
     * indent level. The string returned is the padding * indent level.
     */
    private String getIndent() {
        // Create a buffer big enough to hold the whole indent.
        StringBuffer buffer = new StringBuffer(this.indentLevel * this.padding.length());

        // Repeatedly append the padding to the buffer, "indentLevel"
        // number of times.
        for (int i = 0; i < this.indentLevel; i++) {
            buffer.append(this.padding);
        }

        return buffer.toString();
    }

    /**
     * Returns an identity string based on a label, class, and element
     * name. This should be unique for any given instance of the appender,
     * but there are no guarantees.
     */
    private String getIdentity(String label, Class clazz, String name) {
        return "[" + label + "] " + clazz.getName() + ":" + name;
    }
}
