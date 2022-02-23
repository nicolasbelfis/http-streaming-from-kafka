package worker.processor

import io.mockk.every
import io.mockk.mockk
import org.apache.http.client.NonRepeatableRequestException
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.test.StepVerifier
import twitter.TwitterClientAdapter
import twitter.tweet.SimpleTweet
import worker.kafka.producer.ReactiveProducer
import java.time.Duration

internal class ProcessorExperimentalKtTest {
    private val tweet1 = simpleTweet("id1")

    private val tweet2 = simpleTweet("id2")
    private val tweet3 = simpleTweet("id3")

    private val twitterClientAdapter = mockk<TwitterClientAdapter>()
    private val mockProducer = mockk<ReactiveProducer>()

    @Test
    fun `should process all element when producer works`() {

        every { twitterClientAdapter.stream() } returns Flux.just(tweet1, tweet2)
        every { mockProducer.sendTweetToKafka(tweet1) } returns Mono.just(fakeRecordWithOffset(1L))
        every { mockProducer.sendTweetToKafka(tweet2) } returns Mono.just(fakeRecordWithOffset(2L))
        val fluxToTest = ProcessorExperimental(1, 1, 1).run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectNextMatches { it.offset() == 1L }
            .expectNextMatches { it.offset() == 2L }
            .verifyComplete()
    }


    @Test
    fun `should discard elements that were pushed down stream but not subscribed`() {

        every { twitterClientAdapter.stream() } returns Flux.just(tweet1, tweet2)
        every { mockProducer.sendTweetToKafka(tweet1) } returns Mono.defer<RecordMetadata?> {
            Mono.error(
                Exception()
            )
        }.subscribeOn(Schedulers.single())
        val fluxToTest = ProcessorExperimental(1, 1, 1).run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectError()
            .verifyThenAssertThat()
            .hasDiscarded(tweet2)
    }

    @Test
    fun `should finish in error if all retries failed, but not discard elements when flow is synchronous`() {

        every { twitterClientAdapter.stream() } returns Flux.just(tweet1, tweet2, tweet3)
        every { mockProducer.sendTweetToKafka(tweet1) } returns Mono.defer { Mono.error(Exception()) }
        val fluxToTest = ProcessorExperimental(1, 1, 1).run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectError(NonRepeatableRequestException::class.java)
            .verifyThenAssertThat(Duration.ofSeconds(8))
            .hasNotDiscardedElements()
    }

    @Test
    fun `should discard on backpressure`() {

        every { twitterClientAdapter.stream() } returns threeTweets()
        every { mockProducer.sendTweetToKafka(tweet1) } returns Mono.delay(Duration.ofSeconds(2))
            .map { println("tweet $it");fakeRecordWithOffset(1L) }
        every { mockProducer.sendTweetToKafka(tweet2) } returns Mono.delay(Duration.ofSeconds(10))
            .map { println("tweet $it");fakeRecordWithOffset(2L) }
//            .map { fakeRecordWithOffset(3L) }
        val fluxToTest = ProcessorExperimental(1, 1, 0)
            .run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectNextMatches { it.offset() == 1L }
            .expectError()
            .verifyThenAssertThat(Duration.ofSeconds(8))
            .hasDiscardedElements()
    }

    private fun threeTweets(): Flux<SimpleTweet> =
        Flux.just(tweet1, tweet2, tweet3)

    private fun fakeRecordWithOffset(offSet: Long) = RecordMetadata(
        TopicPartition("", 1),
        0L,
        offSet,
        1L,
        1L,
        1,
        1
    )

    private fun simpleTweet(id: String) = SimpleTweet(id,
        "text",
        emptyList(),
        1,
        "en")

}