package simple.web

import io.github.redouane59.twitter.dto.tweet.Tweet
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import twitter.TwitterClientAdapter
import twitter.TwitterStreamEnded
import twitter.TwitterStreamError
import twitter.TwitterStreamUnknownData
import twitter.tweet.SimpleTweet
import java.time.Duration

@RestController
@Profile("direct-twitter")
class ControllerTweets(
    private val twitterStreamService: TwitterClientAdapter
) {
    private val keepAliveFreq: Duration = Duration.ofSeconds(20L)


    @CrossOrigin
    @GetMapping("/stream/sseTweets", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToTwitterStreamSse(): Flux<ServerSentEvent<SimpleTweet>> {
        return firstNotification<SimpleTweet>()
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

    @GetMapping("/stream/tweets")
    fun subscribeToTwitterStream(): Flux<SimpleTweet> {
        return subscribedTwitterFlux().log()

    }

    private fun subscribedTwitterFlux(): Flux<SimpleTweet> {
        return twitterStreamService.multicastStream()
//            .publishOn(Schedulers.boundedElastic())
//            .flatMap {
//            Thread.sleep(5000)
//            Mono.just(it)
//        }
    }


}
