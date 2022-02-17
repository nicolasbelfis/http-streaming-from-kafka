package worker

import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.signature.TwitterCredentials
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import worker.kafka.producer.ReactiveProducer
import worker.processor.Processor
import worker.twitter.TwitterWorker
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
    ) = ReactiveProducer(kafkaHost, kafkaTopic)

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

