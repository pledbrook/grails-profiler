import com.linkedin.grails.profiler.LoggingAppender
import com.linkedin.grails.profiler.DefaultProfilerLog
import com.linkedin.grails.profiler.ProfilingClosureWrapper
import com.linkedin.grails.profiler.ProfilerHandlerInterceptor
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean
import com.linkedin.grails.profiler.ProfilerMethodInterceptor
import org.codehaus.groovy.grails.commons.spring.BeanConfiguration
import org.springframework.aop.framework.ProxyFactoryBean
import com.linkedin.grails.profiler.LoggingAppender
import com.linkedin.grails.profiler.RequestBufferedAppender
import com.linkedin.grails.profiler.RequestBufferedAppender

class ProfilerGrailsPlugin {
    def version = 0.1
    def dependsOn = [ controllers: "1.0 > *" ]
    def loadAfter = [ "services" ]
    def title = "Profiles a Grails application on demand."
    def author = "Peter Ledbrook"
    def authorEmail = "peter at g2one dot com"
    def description = """\
This plugin allows users to profile their applications on a per-request \
basis, logging how long a request takes, controller actions, service method \
calls, and others.
"""

    def doWithSpring = {
        // First set up the test appender
        testAppender(LoggingAppender) { bean ->
            bean.scope = "prototype"
        }

        htmlAppender(RequestBufferedAppender)

        // Now the logger.
        profilerLog(DefaultProfilerLog) {
            appenderNames = [ "testAppender", "htmlAppender" ]
        }

        profilerMethodInterceptor(ProfilerMethodInterceptor) {
            profiler = profilerLog
        }

        profilerHandlerInterceptor(ProfilerHandlerInterceptor) {
            profiler = profilerLog
        }

        grailsUrlHandlerMapping.interceptors << profilerHandlerInterceptor

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

    def doWithDynamicMethods = { ctx ->
        // Get the access control information from the controllers, if
        // there are any.
        if (manager?.hasGrailsPlugin("controllers")) {
            // Process each controller.
            application.controllerClasses.each { controllerClass ->
                processController(ctx, controllerClass, log)
            }
        }
    }

    def doWithWebDescriptor = { webXml ->
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
                'filter-name'('profilerFilter')
                'url-pattern'("/*")
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
