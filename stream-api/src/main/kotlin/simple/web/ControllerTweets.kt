package simple.web

import io.github.redouane59.twitter.dto.tweet.Tweet
import org.reactivestreams.Publisher
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import worker.twitter.TwitterStreamEnded
import worker.twitter.TwitterStreamError
import worker.twitter.TwitterStreamUnknownData
import worker.twitter.TwitterWorker
import java.time.Duration

@RestController
@Profile("direct-twitter")
class ControllerTweets(
    private val twitterStreamService: TwitterWorker
) {
    private val keepAliveFreq: Duration = Duration.ofSeconds(20L)


    @GetMapping("/stream/sse/tweets", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToTwitterStreamSse(): Flux<ServerSentEvent<Tweet>> {
        return firstNotification<Tweet>()
            .mergeWith(subscribedTwitterFlux().map { sseData(it) })
            .mergeWith(keepAliveFlux(keepAliveFreq))
            .onErrorContinue(TwitterStreamUnknownData::class.java) { it, any ->
                Mono.just(sseComment<Tweet>("unknown message ${it?.message}"))
            }
            .onErrorResume(TwitterStreamEnded::class.java) {
                Mono.just(sseEvent("subscription ended, because ${it.message}"))
            }
            .onErrorResume(TwitterStreamError::class.java) {
                Mono.just(sseEvent("subscription ended, because ${it.message}"))
            }.log()

    }

    @GetMapping("/stream/tweets", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToTwitterStream(): Flux<Tweet> {
        return subscribedTwitterFlux().log()

    }

    private fun subscribedTwitterFlux(): Flux<Tweet> {
        return twitterStreamService.stream()
    }


}
