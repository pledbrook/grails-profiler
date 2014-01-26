import org.codehaus.groovy.grails.plugins.web.taglib.RenderTagLib

class ProfiledTemplateTagLib {
   
   def grailsApplication, profilerLog, bufferedAppender, profilerCondition
   
   private shouldProfileRenderInvokation(attrs) {
      bufferedAppender && profilerCondition?.doProfiling() && attrs.template
   }
   
   Closure render = { attrs, body ->
      if (shouldProfileRenderInvokation(attrs)) {
         profilerLog.logEntry(originalRenderBeanClass, "template ${attrs.template}")
         invokeOriginalRenderTagLib(attrs, body)
         profilerLog.logExit(originalRenderBeanClass, "template ${attrs.template}")
      }
      else {
         invokeOriginalRenderTagLib(attrs, body)
      }
   }

   private invokeOriginalRenderTagLib(attrs, body) {
      grailsApplication.mainContext.getBean(originalRenderBeanClass.name).render.call(attrs, body)
   }
   
   private static Class originalRenderBeanClass = RenderTagLib
   
}
