grails.project.work.dir = 'work'
grails.servlet.version = "3.0" // Change depending on target container compliance (2.5 or 3.0)
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.target.level = 1.6
grails.project.source.level = 1.6

grails.plugin.location.profiler = "../../.."


grails.project.dependency.resolution = {
    inherits("global")
    log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    checksums true // Whether to verify checksums on resolve
    legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility
    repositories {
        inherits true // Whether to inherit repository definitions from plugins

        grailsPlugins()
        grailsHome()
        grailsCentral()

        mavenLocal()
        mavenCentral()
    }

    plugins {
        runtime ":hibernate:$grailsVersion"
        build ":tomcat:$grailsVersion"

		// this should get picked up as a dependency of the profiler plugin,
		// but doesn't seem to be for some reason. maybe a bug when loading
		// via relative path?
		compile ':webxml:1.4.1'

		test ":functional-test:2.0.RC1"
//		test ":functional-test:1.2.7"
	}
}
