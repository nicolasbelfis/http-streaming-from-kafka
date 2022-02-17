package worker

import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.signature.TwitterCredentials
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import worker.kafka.producer.ReactiveProducer
import worker.processor.Processor
import worker.twitter.TwitterWorker
import java.util.*
import kotlin.system.exitProcess


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
        twitterWorker.multicastStream().blockFirst()
        return twitterWorker
    }

    @Bean
    fun reactiveProducer(
        @Value("\${kafka.topic}") kafkaTopic: String,
        @Value("\${kafka.host}") kafkaHost: String,
    ) = ReactiveProducer(kafkaTopic, KafkaProducer(getProperties(kafkaHost)))

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

    @Bean
    fun command(
        twitterWorker: TwitterWorker,
        reactiveProducer: ReactiveProducer
    ): CommandLineRunner = CommandLineRunner {


        val stream = Processor(4, 1, 3)
            .run(twitterWorker, reactiveProducer).doOnSubscribe { it.request(10) }
            .subscribe(
                { log.info("message sent to kafka, offset ${it.offset()}") },
                {
                    log.error("error, end of subscribe, app will shutdown", it)
                    exitProcess(1)
                }
            )

        Runtime.getRuntime().addShutdownHook(
            Thread {
                stream.dispose()
                reactiveProducer.close()
            })
    }
}
