class MainFunctionalTests extends functionaltestplugin.FunctionalTestCase {
    void testSomeWebsiteFeature() {
        // Here call get(uri) or post(uri) to start the session
        // and then use the custom assertXXXX calls etc to check the response
        //
        // get('/something')
        // assertStatus 200
        // assertContentContains 'the expected text'
        // This is the current time to the nearest 100s.
        get "/time"
        assertStatus 200
        checkTimeOutput()
        
        // Check that there is no profiler output because the "profiler"
        // URL parameter has not been specified.
        def profileOutput = byId("profile").textContent
        assertEquals "", profileOutput

        // Now try with the profiler on.
        get "/time?profiler=on"
        assertStatus 200
        checkTimeOutput()
		// TODO: at the moment the profiler doesn't get the controller info correct, just a generic controller started / ended here
        assertContentContains "Entering [uri: /default22/time] com.linkedin.grails.profiler.ProfilerHandlerInterceptor:Controller"
        assertContentContains "Exiting [uri: /default22/time] com.linkedin.grails.profiler.ProfilerHandlerInterceptor:Controller"
        assertContentContains "Entering [uri: /default22/time] DateService:currentTime"
        assertContentContains "Exiting [uri: /default22/time] DateService:currentTime"
    }

    private checkTimeOutput() {
        def time = System.currentTimeMillis().toString()
        def content = byId("time").textContent
        assertTrue "Unexpected content in the page.", (content =~ /Current time: \d{${time.size() - 1}}/) as Boolean
    }
}
