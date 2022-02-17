package worker.processor

import io.github.redouane59.twitter.dto.tweet.Tweet
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
import worker.FakeTweet
import worker.kafka.producer.ReactiveProducer
import java.time.Duration

internal class ProcessorKtTest {

    private val twitterClientAdapter = mockk<TwitterClientAdapter>()
    private val mockProducer = mockk<ReactiveProducer>()

    @Test
    fun `should process all element when producer works`() {

        every { twitterClientAdapter.stream() } returns Flux.just(FakeTweet("1"), FakeTweet("2"))
        every { mockProducer.sendTweetToKafka(FakeTweet("1")) } returns Mono.just(fakeRecordWithOffset(1L))
        every { mockProducer.sendTweetToKafka(FakeTweet("2")) } returns Mono.just(fakeRecordWithOffset(2L))
        val fluxToTest = Processor(1, 1, 1).run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectNextMatches { it.offset() == 1L }
            .expectNextMatches { it.offset() == 2L }
            .verifyComplete()
    }


    @Test
    fun `should discard elements that were pushed down stream but not subscribed`() {

        every { twitterClientAdapter.stream() } returns Flux.just(FakeTweet("1"), FakeTweet("2"))
        every { mockProducer.sendTweetToKafka(FakeTweet("1")) } returns Mono.defer<RecordMetadata?> {
            Mono.error(
                Exception()
            )
        }.subscribeOn(Schedulers.single())
        val fluxToTest = Processor(1, 1, 1).run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectError()
            .verifyThenAssertThat()
            .hasDiscarded(FakeTweet("2"))
    }

    @Test
    fun `should finish in error if all retries failed`() {

        every { twitterClientAdapter.stream() } returns Flux.just(FakeTweet("1"), FakeTweet("2"))
        every { mockProducer.sendTweetToKafka(FakeTweet("1")) } returns Mono.defer { Mono.error(Exception()) }
        val fluxToTest = Processor(1, 1, 1).run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectError(NonRepeatableRequestException::class.java)
            .verify()
    }

    @Test
    fun `should discard on backpressure`() {

        every { twitterClientAdapter.stream() } returns sevenTweets()
        every { mockProducer.sendTweetToKafka(FakeTweet("1")) } returns Mono.delay(Duration.ofSeconds(2))
            .map { println("tweet $it");fakeRecordWithOffset(1L) }
        every { mockProducer.sendTweetToKafka(FakeTweet("2")) } returns Mono.delay(Duration.ofSeconds(10))
            .map { println("tweet $it");fakeRecordWithOffset(2L) }
        every { mockProducer.sendTweetToKafka(FakeTweet("3")) } returns Mono.delay(Duration.ofSeconds(2))
            .map { fakeRecordWithOffset(3L) }
        val fluxToTest = Processor(1, 1, 0)
            .run(twitterClientAdapter, mockProducer)

        StepVerifier.create(fluxToTest)
            .expectNextMatches { it.offset() == 1L }
            .expectError()
            .verifyThenAssertThat(Duration.ofSeconds(8))
            .hasDiscardedElements()
    }

    private fun sevenTweets(): Flux<Tweet> =
        Flux.just(
            FakeTweet("1"),
            FakeTweet("2"),
            FakeTweet("3"),
            FakeTweet("4"),
            FakeTweet("5"),
            FakeTweet("6"),
            FakeTweet("7"),
        )

    private fun fakeRecordWithOffset(offSet: Long) = RecordMetadata(
        TopicPartition("", 1),
        0L,
        offSet,
        1L,
        1L,
        1,
        1
    )
}