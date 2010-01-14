import com.linkedin.grails.profiler.*

import org.codehaus.groovy.grails.commons.spring.BeanConfiguration
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean

class ProfilerGrailsPlugin {
    def version = "0.2"
    def dependsOn = [ controllers: "1.0 > *" ]
    def loadAfter = [ "services" ]
    def title = "Profiles a Grails application on demand."
    def author = "Peter Ledbrook"
    def authorEmail = "peter@g2one.com"
    def description = """\
This plugin allows users to profile their applications on a per-request \
basis, logging how long requests, controller actions, service method \
calls, and others take.
"""

    def doWithSpring = {
        def disableProfiling = application.config.grails.profiler.disable

        if (!disableProfiling) {
            // First set up the appender that logs via Commons Logging.
            loggingAppender(LoggingAppender) { bean ->
                bean.scope = "prototype"
            }

            // This stores logs in a buffer that can be retrieved at
            // any time. It only works if used within a web request,
            // but it is safe to use outside of a request - it just
            // does nothing in that case.
            bufferedAppender(RequestBufferedAppender)

            // This is the condition bean that determines whether
            // profiling should occur or not. The default bean used
            // here simply checks the "profiler" request parameter.
            profilerCondition(ParamProfilerCondition) {
                paramName = "profiler"
                values = [ "on", "1", "true" ] as Set
            }

            // Now the logger.
            profilerLog(DefaultProfilerLog) {
                appenderNames = [ "loggingAppender", "bufferedAppender" ]
            }

            // Interceptor for profiling service method invocations.
            profilerMethodInterceptor(ProfilerMethodInterceptor) {
                profiler = profilerLog
            }

            // Spring HandlerInterceptor for profiling controllers and views.
            profilerHandlerInterceptor(ProfilerHandlerInterceptor) {
                profiler = profilerLog
            }

            if (springConfig.containsBean("grailsUrlHandlerMapping")) {
                // Grails 1.1 and below
                grailsUrlHandlerMapping.interceptors << profilerHandlerInterceptor
            }
            else {
                // Grails 1.2 and above
                [annotationHandlerMapping, controllerHandlerMappings]*.interceptors << profilerHandlerInterceptor
            }

            // We do some magic with the service beans: the existing bean
            // definitions are replaced with proxy beans
            if (manager?.hasGrailsPlugin("services")) {
                application.serviceClasses.each { serviceClass ->
                    def serviceName = serviceClass.propertyName
                    BeanConfiguration beanConfig = springConfig.getBeanConfig(serviceName)

                    // If we're dealing with a TransactionProxyFactoryBean,
                    // then we can add the profiler method interceptor
                    // directly to it.
                    if (beanConfig.beanDefinition.beanClassName == TransactionProxyFactoryBean.name) {
                        if (!beanConfig.hasProperty("preInterceptors")) {
                            beanConfig.addProperty("preInterceptors", [])
                        }

                        delegate."$serviceName".preInterceptors << ref("profilerMethodInterceptor")
                    }
                    // Otherwise, we need to repace the existing bean
                    // definition with a proxy factory bean that calls
                    // back to the original service bean.
                    else {
                        // First store the current service bean configuration
                        // under a different bean name.
                        springConfig.addBeanConfiguration("${serviceName}Profiled", beanConfig)

                        // Now create the proxy factory bean and add the
                        // method interceptor to it.
                        "$serviceName"(ProxyFactoryBean) {
                            // We don't want auto-detection of interfaces,
                            // otherwise Spring will just proxy the GroovyObject
                            // interface - not what we want!
                            autodetectInterfaces = false
                            targetName = "${serviceName}Profiled"
                            interceptorNames = [ "profilerMethodInterceptor" ]
                        }
                    }
                }
            }
        }
    }

    def doWithDynamicMethods = { ctx ->
        def disableProfiling = application.config.grails.profiler.disable

        if (!disableProfiling) {
            // Get the access control information from the controllers, if
            // there are any.
            if (manager?.hasGrailsPlugin("controllers")) {
                // Process each controller.
                application.controllerClasses.each { controllerClass ->
                    processController(ctx, controllerClass, log)
                }
            }
        }
    }

    def doWithWebDescriptor = { webXml ->
        def disableProfiling = application.config.grails.profiler.disable

        if (!disableProfiling) {
            // Add the profiler filter to the web app.
            def filterDef = webXml.'filter'
            filterDef[filterDef.size() - 1] + {
                'filter' {
                    'filter-name'('profilerFilter')
                    'filter-class'('com.linkedin.grails.profiler.ProfilerFilter')
                }
            }

            // This filter *must* come before the urlMapping filter, otherwise
            // it will never be executed.
            def filterMapping = webXml.'filter-mapping'.find { it.'filter-name'.text() == "charEncodingFilter" }
            filterMapping + {
                'filter-mapping' {
                    'filter-name'("profilerFilter")
                    'url-pattern'("/*")
                }
            }
        }
    }

    /**
     * Wraps all a controller's actions with a special profiling closure
     * wrapper.
     */
    def processController(ctx, controllerClass, log) {
        def controller = controllerClass.clazz

        controller.metaClass.getProperty = { String propName ->
            // Get the property.
            def mp = delegate.class.metaClass.getMetaProperty(propName)
            if (mp) {
                def result = mp.getProperty(delegate)
                if (result instanceof Closure) {
                    result = new ProfilingClosureWrapper(result, ctx.getBean("profilerLog"), propName)
                }

                return result
            }
            else {
                throw new MissingPropertyException(propName)
            }
        }
    }
}
