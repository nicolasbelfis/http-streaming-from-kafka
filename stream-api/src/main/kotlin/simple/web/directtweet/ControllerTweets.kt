package simple.web.directtweet

import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import simple.web.sseComment
import simple.web.toSSE
import simple.web.sseEvent
import twitter.TwitterClientAdapter
import twitter.TwitterStreamEnded
import twitter.TwitterStreamError
import twitter.TwitterStreamUnknownData
import twitter.tweet.SimpleTweet

@RestController
@Profile("direct-twitter")
class ControllerTweets(
    private val twitterStreamService: TwitterClientAdapter,
) {

    @CrossOrigin
    @GetMapping("/stream/sseTweets", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToTwitterStreamSse(): Flux<ServerSentEvent<SimpleTweet>> {

        return getTwitterStream()
            .map { tweet -> toSSE(tweet) }
            .onErrorResume(TwitterStreamUnknownData::class.java) {
                Mono.just(sseComment("unknown message ${it?.message}"))
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
        return getTwitterStream().log()

    }

    private fun getTwitterStream(): Flux<SimpleTweet> {
        return twitterStreamService.multicastStream()
    }


}
