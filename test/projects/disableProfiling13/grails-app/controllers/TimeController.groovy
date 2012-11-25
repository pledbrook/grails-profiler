class TimeController {
    def dateService

    def index = {
        [ time: dateService.currentTime() ]
    }
}
