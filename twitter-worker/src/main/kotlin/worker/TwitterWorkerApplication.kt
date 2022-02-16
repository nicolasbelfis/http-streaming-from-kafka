package worker

import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.dto.tweet.Tweet
import io.github.redouane59.twitter.signature.TwitterCredentials
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import reactor.core.CoreSubscriber
import reactor.core.Disposable
import reactor.core.publisher.Mono
import worker.twitter.TwitterWorker
import java.util.*
import java.util.concurrent.CompletableFuture


fun main(args: Array<String>) {
    runApplication<TwitterWorkerApplication>(*args)
}

@SpringBootApplication
class TwitterWorkerApplication {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun twitterWorker(
        @Value("\${twitter.bearer}") bearerToken: String
    ): TwitterWorker {
        val twitterClient = TwitterClient(
            TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build()
        )
        val twitterWorker = TwitterWorker(twitterClient)
        twitterWorker.stream().blockFirst()
        return twitterWorker
    }

    @Bean
    fun command(
        @Value("\${kafka.host}") kafkaHost: String,
        @Value("\${kafka.topic}") kafkaTopic: String,
        twitterWorker: TwitterWorker
    ): CommandLineRunner = CommandLineRunner {

        val producer: Producer<String, String> = KafkaProducer(getProperties(kafkaHost))

        val subscribe: Disposable = twitterWorker.stream()
            .onBackpressureBuffer(4)
            .log()
            .flatMap({ sendTweetToKafka(it, producer, kafkaTopic) }, 3)
            .subscribe(
                { log.info("message sent to kafka, offset ${it.offset()}") },
                { log.error("error ", it) }
            )

        Runtime.getRuntime().addShutdownHook(
            Thread {
                subscribe.dispose()
                producer.close()
            })
    }

    private fun sendTweetToKafka(
        tweet: Tweet,
        producer: Producer<String, String>,
        kafkaTopic: String
    ): Mono<RecordMetadata> {
        val producerRecord: ProducerRecord<String, String> = ProducerRecord(kafkaTopic, tweet.id, tweet.text)
        return Mono.create<RecordMetadata> {
            log.info("sending to kafka tweet ${tweet.id}")
            producer.send(producerRecord) { r, e: Exception? ->
                if (e == null) it.success(r)
                else
                    it.error(e)
            }
        }.retry(3)
    }


    private fun getProperties(kafkaHost: String): Properties {
        val settings = Properties()

        settings[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaHost
        org.apache.kafka.common.serialization.StringSerializer::class.java
        settings[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] =
            org.apache.kafka.common.serialization.StringSerializer::class.java
        settings[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
            org.apache.kafka.common.serialization.StringSerializer::class.java
        return settings
    }

}
