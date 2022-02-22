package simple.web

import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import simple.streaming.TweetConsumer
import twitter.tweet.SimpleTweet

@RestController
@Profile("kafka-twitter")
class ControllerTweetsWithKafka(
    private val tweetConsumer: TweetConsumer,
) {

    @CrossOrigin
    @GetMapping("/stream/sseTweets", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToTwitterStreamSse(): Flux<ServerSentEvent<SimpleTweet>> {
        return tweetConsumer.stream().map { sseData(it) }
            .onErrorResume {
                Mono.just(sseEvent("subscription ended, because ${it.message}"))
            }
    }
}
