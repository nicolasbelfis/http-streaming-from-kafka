package simple.web

import com.fasterxml.jackson.databind.JsonNode
import simple.logger.Loggers
import simple.streaming.StreamService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val MILLISECONDS = 1000

@RestController
@RequestMapping("flow")
class ControllerFlow(
    @Qualifier("flowStream")
    private val streamService: StreamService<String, Flow<String>>,
    private val keepAliveFreq: Int = 20

) {

    @GetMapping("/stream", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToStream(): Flow<ServerSentEvent<String>> {
        val messages = streamService.stream()
            .map { ServerSentEvent.builder(it).build() }
            .catch { cause -> Loggers.print("error in stream :$cause") }
        return startSubscription(messages)

    }

    private fun startSubscription(messages: Flow<ServerSentEvent<String>>): Flow<ServerSentEvent<String>> {
        val keepAliveFlow = keepAliveFluxWithFrequency(keepAliveFreq.toLong())

        return merge(
            keepAliveFlow,
            messages.onStart { emit(ServerSentEvent.builder<String>().comment("subscription started").build()) }
        )
    }

    private fun keepAliveFluxWithFrequency(timeSeconds: Long) = flow {
        while (true) {
            delay(timeSeconds * MILLISECONDS)
            emit(ServerSentEvent.builder<String>().comment("keep-alive").build())
        }
    }

    @PostMapping("/message", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun send(@RequestBody messageJson: JsonNode) {
        val message = messageJson.get("message").asText()
        streamService.emit(message)
    }
}
