package worker.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import twitter.tweet.SimpleTweet
import java.time.Duration

class ReactiveProducer(
    private val kafkaTopic: String,
    private val producer: Producer<String, String>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendTweetToKafka(tweet: SimpleTweet): Mono<RecordMetadata> {
        return Mono.create<RecordMetadata> {
            callProducer(tweet, it)
        }.doOnError { log.error("error sending message to kafka", it) }

    }

    private fun callProducer(
        tweet: SimpleTweet,
        it: MonoSink<RecordMetadata>
    ) {
        log.info("sending to kafka tweet ${tweet.id}")
        producer.send(
            ProducerRecord(kafkaTopic, tweet.id, ObjectMapper().writeValueAsString(tweet)),
            producerCallback(it)
        )
        log.info("producer called")

    }

    private fun producerCallback(it: MonoSink<RecordMetadata>) =
        { r: RecordMetadata?, e: Exception? ->
            if (e == null) it.success(r)
            else it.error(e)
        }

    fun close() {
        producer.close(Duration.ofSeconds(1))
    }
}
    