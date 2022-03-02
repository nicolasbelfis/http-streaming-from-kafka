package simple.web

import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

fun <T> toSSE(message: T): ServerSentEvent<T> = ServerSentEvent.builder(message).build()
fun <T> sseComment(comment: String): ServerSentEvent<T> = ServerSentEvent.builder<T>().comment(comment).build()
fun <T> sseEvent(event: String): ServerSentEvent<T> = ServerSentEvent.builder<T>().event(event).build()

fun <T> keepAliveFlux(keepAliveFreq: Duration): Flux<ServerSentEvent<T>> =
    keepAliveFluxWithFrequency(sseComment("keepAlive"), keepAliveFreq)
fun <T> keepAliveFluxWithFrequency(comment: ServerSentEvent<T>, keepAliveFreq: Duration): Flux<ServerSentEvent<T>> =
    Flux.interval(keepAliveFreq)
        .map { comment }
fun <T> firstNotification(): Mono<ServerSentEvent<T>> = Mono.just(sseComment("subscription started"))
