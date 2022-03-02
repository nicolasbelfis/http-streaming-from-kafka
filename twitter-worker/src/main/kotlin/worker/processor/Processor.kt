package worker.processor

import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import twitter.TwitterClientAdapter
import worker.kafka.producer.ReactiveProducer
import java.time.Duration

class Processor {
    private val log = LoggerFactory.getLogger("Processor")
    fun createProducerPipeline(
        twitterClientAdapter: TwitterClientAdapter,
        reactiveProducer: ReactiveProducer,
    ): Flux<RecordMetadata> {
        return twitterClientAdapter.stream()
            .flatMap { reactiveProducer.sendTweetToKafka(it) }
            .doOnError { log.error("error while processing", it) }
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .doBeforeRetry { log.info("restarting full stream, will reconnect to twitter, any message not processed is then discarded") }
            )

    }

}


