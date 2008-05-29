class ProfilerTagLib {
    def bufferedAppender

    def profilerOutput = { attrs ->
        if (bufferedAppender) {
            out << bufferedAppender.output
        }
    }
}
