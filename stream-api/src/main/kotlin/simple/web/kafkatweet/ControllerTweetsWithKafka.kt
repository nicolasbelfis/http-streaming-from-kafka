package simple.web.kafkatweet

import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import simple.streaming.TweetConsumer
import simple.web.firstNotification
import simple.web.sseData
import simple.web.sseEvent
import twitter.tweet.ObjectMapperKotlin
import twitter.tweet.SimpleTweet

@RestController
@Profile("kafka-twitter")
class ControllerTweetsWithKafka(
    private val tweetConsumer: TweetConsumer,
    private val countTagConsumer: TweetConsumer,
) {

    @CrossOrigin
    @GetMapping("/stream/sseTweets", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToTwitterStreamSse(): Flux<ServerSentEvent<SimpleTweet>> {
        return Flux.merge(
            firstNotification(),
            tweetConsumer.stream { ObjectMapperKotlin.readValue(it.second, SimpleTweet::class.java) }
                .map { sseData(it) }
                .onErrorResume {
                    Mono.just(sseEvent("subscription ended, because ${it.message}"))
                }
        )
    }

    @CrossOrigin
    @GetMapping("/stream/countTags", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToCountTag(@RequestParam filters: String): Flux<ServerSentEvent<String>> {
        return countTagConsumer.stream {
            ObjectMapperKotlin.writeValueAsString(it)
        }.filter { message -> filters.trim().split(",").any { message.contains(it) } }
            .map { sseData(it) }
            .onErrorResume {
                Mono.just(sseEvent("subscription ended, because ${it.message}"))
            }
    }
}
