package simple.web.experimental

import com.fasterxml.jackson.databind.JsonNode
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
import simple.logger.Loggers
import simple.streaming.StreamService
import simple.web.firstNotification
import simple.web.keepAliveFlux
import simple.web.toSSE
import simple.web.sseEvent
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

@RestController
@RequestMapping("flux")
class ControllerFlux(
    @Qualifier("fluxStream")
    private val streamService: StreamService<String, Flux<String>>,
) {
    private val keepAliveFreq: Duration = Duration.ofSeconds(20L)

    //mediatype automatically set to event stream
    @GetMapping("/stream")
    fun subscribeToStream(): Flux<ServerSentEvent<String>> {
        return firstNotification<String>()
            .mergeWith(subscribedFlux())
            .mergeWith(keepAliveFlux(keepAliveFreq))
            .log()
            .map { Thread.sleep(1000);it }
            .onErrorContinue(NonCancellableException::class.java) { it, any ->
                Loggers.print("${it.message} error on stream but continue subscribing")
            }
            .onErrorResume(CancellableException::class.java) {
                Mono.just(sseEvent("subscription ended, because ${it.message}"))
            }
    }

    //mediatype automatically set to event stream
    @GetMapping("/stream/objects")
    fun subscribeToStreamObjects(): Flux<ServerSentEvent<MyMessage>> {
        val atomicInteger = AtomicInteger(0)
        return subscribedFlux().map { Thread.sleep(1000);ServerSentEvent.builder(MyMessage(atomicInteger.incrementAndGet(),it.data())).build() }
    }

    @GetMapping("/stream/json")
    fun subscribeToStreamJson(): Flux<MyMessage> {
        return subscribedFlux().map { Thread.sleep(1000);MyMessage(1,it.data()) }
    }

    private fun subscribedFlux() = streamService.stream().map { toSSE(it.toString()) }
    private fun subscribedStaticJsonFlux() = Flux.just(MyMessage(1, "hello"), MyMessage(2, "hello2"))


    @PostMapping("/message", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun send(@RequestBody messageJson: JsonNode) {
        val message = messageJson.get("message").asText()
        streamService.emit(message)
    }

}

data class MyMessage(val i: Int, val s: String)