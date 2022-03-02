package worker.processor

import io.github.redouane59.twitter.dto.tweet.Tweet
import org.apache.http.client.NonRepeatableRequestException
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import twitter.TwitterClientAdapter
import twitter.tweet.SimpleTweet
import worker.kafka.producer.ReactiveProducer
import java.time.Duration

class ProcessorExperimental(
    private val backPressureBufferElements: Int,
    private val maxConcurrentProducerRequest: Int,
    private val maxRetries: Int
) {
    private val log = LoggerFactory.getLogger("Processor")
    fun createProducerPipeline(
        twitterClientAdapter: TwitterClientAdapter,
        reactiveProducer: ReactiveProducer
    ): Flux<RecordMetadata> {
        return twitterClientAdapter.stream()
            .log()
            .onBackpressureBuffer(backPressureBufferElements)
            .flatMap({ reactiveProducer.sendTweetToKafka(it) }, maxConcurrentProducerRequest)
            .doOnError { log.error("error while processing", it) }
            .doOnDiscard(SimpleTweet::class.java) { log.info("tweet ${it.id} discarded") }
            .retryWhen(
                retryStrategy(
                    maxRetries.toLong(),
                    "restarting full stream, will reconnect to twitter, any message not processed is then discarded"
                )
            )

    }

    private fun retryStrategy(maxRetries: Long, message: String) = Retry.backoff(maxRetries, Duration.ofSeconds(1))
        .doBeforeRetry { log.info("retrying... $message") }
        .onRetryExhaustedThrow { t, u -> NonRepeatableRequestException("retries exhausted") }

}


