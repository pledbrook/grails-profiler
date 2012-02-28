class ProfilerTagLib {

	def bufferedAppender

	def profilerOutput = { attrs ->
		if (!bufferedAppender) {
			return
		}

		out << bufferedAppender.output
	}
}
