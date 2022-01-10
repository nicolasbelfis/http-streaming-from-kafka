package simple.web

import com.fasterxml.jackson.databind.JsonNode
import simple.logger.Loggers
import simple.streaming.StreamService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@RestController
@RequestMapping("flux")
class ControllerFlux(
    @Qualifier("fluxStream")
    private val streamService: StreamService<String, Flux<String>>,

) {
    private val keepAliveFreq: Int = 20

    @GetMapping("/stream", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToStream(): Flux<ServerSentEvent<String>> {
        val messages = streamService.stream()
            .map { ServerSentEvent.builder(it.toString()).build() }
            .onErrorContinue { cause, msg -> Loggers.print("error in stream :$cause, msg : $msg") }
        return startSubscription(messages)
    }

    private fun startSubscription(messages: Flux<ServerSentEvent<String>>): Flux<ServerSentEvent<String>> {
        val messageHeader = Mono.just(ServerSentEvent.builder<String>().comment("subscription started").build())
        val keepAliveEmitter = keepAliveFluxWithFrequency(Duration.ofSeconds(keepAliveFreq.toLong()))
        return Flux.merge(
            messageHeader,
            messages,
            keepAliveEmitter
        ).log()
    }

    private fun keepAliveFluxWithFrequency(duration: Duration) = Flux.interval(duration)
        .map { ServerSentEvent.builder<String>().comment("keepAlive").build() }

    @PostMapping("/message", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun send(@RequestBody messageJson: JsonNode) {
        val message = messageJson.get("message").asText()
        streamService.emit(message)
    }
}
