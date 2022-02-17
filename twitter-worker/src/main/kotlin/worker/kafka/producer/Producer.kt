package worker.kafka.producer

import io.github.redouane59.twitter.dto.tweet.Tweet
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import java.time.Duration
import java.util.*

class ReactiveProducer(kafkaHost: String, private val kafkaTopic: String) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val producer: Producer<String, String> = KafkaProducer(getProperties(kafkaHost))

    fun sendTweetToKafka(tweet: Tweet): Mono<RecordMetadata> {
        return Mono.create<RecordMetadata> {
            callProducer(tweet, it)
        }.doOnError { log.error("error sending message to kafka", it) }

    }

    private fun callProducer(
        tweet: Tweet,
        it: MonoSink<RecordMetadata>
    ) {
        log.info("sending to kafka tweet ${tweet.id}")
        producer.send(
            ProducerRecord(kafkaTopic, tweet.id, tweet.text),
            producerCallback(it)
        )
        log.info("producer called")

    }

    private fun producerCallback(it: MonoSink<RecordMetadata>) =
        { r: RecordMetadata?, e: Exception? ->
            if (e == null) it.success(r)
            else it.error(e)
        }

    private fun getProperties(kafkaHost: String): Properties {
        val settings = Properties()

        settings[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaHost
        org.apache.kafka.common.serialization.StringSerializer::class.java
        settings[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            org.apache.kafka.common.serialization.StringSerializer::class.java
        settings[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            org.apache.kafka.common.serialization.StringSerializer::class.java
        settings[ProducerConfig.RETRIES_CONFIG] = 3
        settings[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 2000
        settings[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = 1
        settings[ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG] = 200
        settings[ProducerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG] = 5000
        settings[ProducerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG] = 2000

        return settings
    }

    fun close() {
        producer.close(Duration.ofSeconds(1))
    }
}
    