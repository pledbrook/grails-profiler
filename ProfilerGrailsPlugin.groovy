import com.linkedin.grails.profiler.DefaultProfilerLog
import com.linkedin.grails.profiler.LoggingAppender
import com.linkedin.grails.profiler.ParamProfilerCondition
import com.linkedin.grails.profiler.ProfilerFilter
import com.linkedin.grails.profiler.ProfilerHandlerInterceptor
import com.linkedin.grails.profiler.ProfilerMethodInterceptor
import com.linkedin.grails.profiler.ProfilingClosureWrapper
import com.linkedin.grails.profiler.RequestBufferedAppender

import org.codehaus.groovy.grails.commons.spring.BeanConfiguration
import org.springframework.aop.framework.ProxyFactoryBean
import org.springframework.transaction.interceptor.TransactionProxyFactoryBean

class ProfilerGrailsPlugin {
	def version = "0.6-SNAPSHOT"
	def grailsVersion = "1.3.3 > *"
	def loadAfter = ["services", "controllers"]
	def title = "Profile Plugin"
	def author = "Peter Ledbrook"
	def authorEmail = "p.ledbrook@cacoethes.co.uk"
	def description = """\
Profile applications on a per-request basis, logging how \
long requests, controller actions and service method calls take."""
	def documentation = "http://grails.org/plugin/profiler"

	def license = 'APACHE'
	def developers = [
			[name: "Burt Beckwith", email: "beckwithb@vmware.com"],
			[name: "Tom Dunstan", email: "grails@tomd.cc"]
	]
	def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/GPPROFILER']
	def scm = [url: 'https://github.com/pledbrook/grails-profiler']

	def getWebXmlFilterOrder() {
		def FilterManager = getClass().getClassLoader().loadClass('grails.plugin.webxml.FilterManager')
		[profilerFilter: FilterManager.URL_MAPPING_POSITION - 100]
	}

	def doWithSpring = {
		def disableProfiling = application.config.grails.profiler.disable
		if (disableProfiling) {
			return
		}
		def scope = serviceClass.getPropertyValue("scope")
		def lazyInit = serviceClass.hasProperty("lazyInit") ? serviceClass.getPropertyValue("lazyInit") : true

		// First set up the appender that logs via Slf4j.
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
			values = ["on", "1", "true"] as Set
		}

		// Now the logger.
		profilerLog(DefaultProfilerLog) {
			appenderNames = ["loggingAppender", "bufferedAppender"]
		}

		// Interceptor for profiling service method invocations.
		profilerMethodInterceptor(ProfilerMethodInterceptor) {
			profiler = profilerLog
		}

		// Spring HandlerInterceptor for profiling controllers and views.
		profilerHandlerInterceptor(ProfilerHandlerInterceptor) {
			profiler = profilerLog
		}

		[annotationHandlerMapping, controllerHandlerMappings]*.interceptors << profilerHandlerInterceptor

		// We do some magic with the service beans: the existing bean
		// definitions are replaced with proxy beans
		if (manager?.hasGrailsPlugin("services")) {
			for (serviceClass in application.serviceClasses) {
				String serviceName = serviceClass.propertyName
				BeanConfiguration beanConfig = springConfig.getBeanConfig(serviceName)
				if (!beanConfig) {
					continue
				}

				// If we're dealing with a TransactionProxyFactoryBean,
				// then we can add the profiler method interceptor directly to it.
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

					// Now create the proxy factory bean and add the method interceptor to it.
					"$serviceName"(ProxyFactoryBean) {bean ->
						if (scope) bean.scope = scope
						bean.lazyInit = lazyInit
						
						// We don't want auto-detection of interfaces,
						// otherwise Spring will just proxy the GroovyObject
						// interface - not what we want!
						autodetectInterfaces = false
						targetName = "${serviceName}Profiled"
						interceptorNames = ["profilerMethodInterceptor"]
					}
				}
			}
		}
	}

	def doWithDynamicMethods = { ctx ->
		def disableProfiling = application.config.grails.profiler.disable
		if (disableProfiling) {
			return
		}

		// Get the access control information from the controllers, if there are any.
		if (!manager?.hasGrailsPlugin("controllers")) {
			return
		}

		// Process each controller.
		for (controllerClass in application.controllerClasses) {
			processController(ctx, controllerClass, log)
		}
	}

	def doWithWebDescriptor = { webXml ->
		def disableProfiling = application.config.grails.profiler.disable
		if (disableProfiling) {
			return
		}

		// Add the profiler filter to the web app.
		def filterDef = webXml.'filter'
		filterDef[filterDef.size() - 1] + {
			'filter' {
				'filter-name'('profilerFilter')
				'filter-class'(ProfilerFilter.name)
			}
		}

		// This filter *must* come before the urlMapping filter, otherwise it will never be executed.
		def filterMapping = webXml.'filter-mapping'.find { it.'filter-name'.text() == "charEncodingFilter" }
		filterMapping + {
			'filter-mapping' {
				'filter-name'("profilerFilter")
				'url-pattern'("/*")
			}
		}
	}

	/**
	 * Wraps all a controller's actions with a special profiling closure wrapper.
	 */
	private void processController(ctx, controllerClass, log) {
		Class controller = controllerClass.clazz

		controller.metaClass.getProperty = { String propName ->
			// Get the property.
			def targetMetaClass = delegate.getClass().metaClass
			def mp = targetMetaClass.getMetaProperty(propName)
			if (!mp) {
				// probably a taglib or other property added via missing property on the metaclass,
				// delegate to that since it won't be an action closure that we want to profile anyway
				return targetMetaClass.invokeMissingProperty(delegate, propName, null, true)
			}

			def result = mp.getProperty(delegate)
			if (result instanceof Closure) {
				result = new ProfilingClosureWrapper(controller, result, ctx.profilerLog, propName)
			}

			result
		}
	}
}
