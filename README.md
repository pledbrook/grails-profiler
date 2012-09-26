# Profiler Plugin

Investigating the performance of an application is an important step in improving its usability. The Profiler Plugin makes it easy to collect timing information for several aspects of an application:

* Requests
* Controller actions
* Service methods
* View generation

## Installation

grails install-plugin profiler

## How to use it

Once the plugin is installed, all you need to do to profile a request is add the "profiler=on" or "profiler=1" parameter to it. For example, to profile a simple GET request:

http://localhost:8080/myapp/book/list?profiler=1

You may not see any profiling information yet because it depends on your Log4J configuration. All profiling information is logged at the INFO level to the "com.linkedin.grails.ProfilerPlugin" logger, so this simple configuration will have the profiling messages appear in your console:

    log4j {
        ...
        logger {
            ...
            com.linkedin.grails = "info"
        }
        ...
    }

It is also possible to place the profiling information in your HTML pages by using this GSP tag:

    <g:profilerOutput />

This will simply write out the profiling information collected so far for the current request. For best results it should be included in the layout inside an HTML comment. If you use it directly within a view, the profiling information will lack the timing for the view generation itself.

For more information, go to the [plugin portal page](http://grails.org/plugin/profiler).

### Disabling the profiler

You can set a configuration option to completely disable the plugin:

	grails.profiler.disable = true

This is particularly useful on a per-environment basis, in case you don't want the impact of profiling in production or even development.

## Advanced usage

The plugin is designed around a set of Spring beans that can be used directly from within your code if you require.  The most useful beans are documented here with their bean names.

### profilerCondition

This bean implements the `com.linkedin.grails.profiler.ProfilerCondition` interface and determines whether profiling is active for the current request or not. The default implementation checks the "profiler" parameter, but you can provide your own implementation in resource.groovy for example.

<table>
 <tr><th>Method</th><th>Description</th></tr>
 <tr><td><tt>doProfiling()</tt></td><td>Returns <tt>true</tt> if profiling should be enabled for the current request, otherwise <tt>false</tt>.</td></tr>
</table>

### profilerLog

This is the main bean. It sends log messages to all registered appenders while profiling is active. Otherwise it does nothing.

<table>
 <tr><th>Method</th><th>Description</th></tr>
 <tr>
  <td><tt>startProfiling(String)</tt></td>
  <td>Starts the profiler and assigns the given label to the log messages.
 <tr>
  <td><tt>stopProfiling()</tt></td>
  <td>Stops the profiler.</td>
 </tr>
 <tr>
  <td><tt>isProfiling()</tt></td>
  <td>Returns <tt>true</tt> if the profiler is currently active, otherwise <tt>false</tt>.</td>
 </tr>
 <tr>
  <td><tt>logEntry(Class, String)</tt></td>
  <td>Logs entry to an action/method/whatever.</td>
 </tr>
 <tr>
  <td><tt>logExit(Class, String)</tt></td>
  <td>Logs exit from an action/method/whatever. There should be one call to <tt>logExit()</tt> for every <tt>logEntry()</tt>.</td>
 </tr>
</table>

### bufferedAppender

A special appender that stores its log messages in a buffer so that it can be retrieved from code. Inject the bean into your own and get the output whenever you like!

<table>
 <tr><th>Method</th><th>Description</th></tr>
 <tr><td><tt>getOutput()</tt></td><td>Returns the log output currently stored in this appender's buffer.</td></tr>
</table>

