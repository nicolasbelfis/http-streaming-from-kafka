package worker.kafka.producer

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import twitter.tweet.SimpleTweet
import java.time.Duration

class ReactiveProducer(
    private val kafkaTopic: String,
    private val producer: Producer<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun sendTweetToKafka(tweet: SimpleTweet): Mono<RecordMetadata> {
        val sink = Sinks.one<RecordMetadata>()
        producer.send(
            ProducerRecord(kafkaTopic, tweet.id, tweet.toJson()),
            producerCallback(sink)
        )
        return sink.asMono()
    }

    private fun producerCallback(sink: Sinks.One<RecordMetadata>): (RecordMetadata?, Exception?) -> Unit =
        { r: RecordMetadata?, e: Exception? ->
            if (e == null) sink.tryEmitValue(r)
            else sink.tryEmitError(e)
        }

    fun close() {
        producer.close(Duration.ofSeconds(1))
    }
}
    