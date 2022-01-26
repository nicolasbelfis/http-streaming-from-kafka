package simple.web

import com.fasterxml.jackson.databind.JsonNode
import org.reactivestreams.Publisher
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
import reactor.util.Metrics
import simple.logger.Loggers
import java.lang.NullPointerException
import java.time.Duration

@RestController
@RequestMapping("flux")
class ControllerFlux(
    @Qualifier("fluxStream")
    private val streamService: StreamService<String, Flux<String>>,

    ) {
    private val keepAliveFreq: Duration = Duration.ofSeconds(20L)

    @GetMapping("/stream", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToStream(): Flux<ServerSentEvent<String>> {
        return Flux.merge(
            Mono.just(ServerSentEvent.builder<String>().comment("subscription started").build()),
            streamService.stream()
                .map { ServerSentEvent.builder(it.toString()).build() },
            keepAliveFluxWithFrequency(keepAliveFreq)
        ).log()
            .onErrorContinue(IllegalArgumentException::class.java) { it, any -> Loggers.print("non problematic error $it") }
            .onErrorResume(NullPointerException::class.java) {
                Mono.just(
                    ServerSentEvent.builder<String>().comment("subscription ended").build()
                )
            }
    }

    private fun keepAliveFluxWithFrequency(duration: Duration) = Flux.interval(duration)
        .map { ServerSentEvent.builder<String>().comment("keepAlive").build() }

    @PostMapping("/message", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun send(@RequestBody messageJson: JsonNode) {
        val message = messageJson.get("message").asText()
        streamService.emit(message)
    }
}

private fun Flux<ServerSentEvent<String>>.manageErrors(): Flux<ServerSentEvent<String>> {
    return this

}
