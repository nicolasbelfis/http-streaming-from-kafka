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
import simple.repositories.TagCountRepository
import simple.streaming.TweetConsumer
import simple.web.firstNotification
import simple.web.sseComment
import simple.web.sseData
import simple.web.sseEvent
import twitter.tweet.ObjectMapperKotlin
import twitter.tweet.SimpleTweet

@RestController
@Profile("kafka-twitter")
class ControllerTweetsWithKafka(
    private val tweetConsumer: TweetConsumer,
    private val countTagConsumer: TweetConsumer,
    private val tagCountRepository: TagCountRepository,
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

    @CrossOrigin
    @GetMapping("/streamState/countTags", produces = [(MediaType.TEXT_EVENT_STREAM_VALUE)])
    fun subscribeToCountTagWithState(@RequestParam filters: String): Flux<ServerSentEvent<String>> {
        val tagFilters: List<String> = filters.trim().split(",")

        return Flux.from(tagCountRepository.findTagCountsByTags(tagFilters))
            .map { it["tag"] to it["count"] }
            .map { pair -> ObjectMapperKotlin.writeValueAsString(pair) }
            .map { sseData(it) }
            .defaultIfEmpty(sseComment("no state"))
            .flatMap {
                Flux.merge(
                    Mono.just(it),
                    countTagConsumer.stream { pair ->
                        ObjectMapperKotlin.writeValueAsString(pair)
                    }.filter { message -> tagFilters.any { message.contains(it) } }
                        .map { sseData(it) }
                )
            }
            .onErrorResume {
                Mono.just(sseEvent("subscription ended, because ${it.message}"))
            }
    }
}
