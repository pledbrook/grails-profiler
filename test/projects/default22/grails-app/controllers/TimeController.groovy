class TimeController {
    def dateService

    def index = {
        [ time: dateService.currentTime(), message: g.message([code: 'default.home.label']) ]
    }
    
    def indexWithTemplate = {
       index.call()
    }
}
