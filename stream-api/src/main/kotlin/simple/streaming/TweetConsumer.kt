package simple.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import simple.ObjectMapperKotlin
import twitter.tweet.SimpleTweet
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean


class TweetConsumer(private val consumerProperties: Properties, private val kafkaTopic: String) {

    private val closingRequest: AtomicBoolean = AtomicBoolean(false)
    private val log = LoggerFactory.getLogger(javaClass)
    private val sink: Sinks.Many<String> = Sinks.many().multicast().directBestEffort()

    private val executorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    fun stream(): Flux<SimpleTweet> {
        return Mono.fromCallable {
            if (sink.currentSubscriberCount() == 0)
                startConsumerAsync()
        }.flatMapMany { sink.asFlux() }
            .map { ObjectMapperKotlin.readValue(it, SimpleTweet::class.java) }
            .doOnError { log.error("error", it) }
            .doAfterTerminate { closingRequest.set(true) }
            .doOnCancel {
                if (sink.currentSubscriberCount() == 1)
                    closingRequest.set(true)
            }
    }

    private fun startConsumerAsync() {
        log.info("connecting to kafka")
        closingRequest.set(false)
        val kafkaConsumer = KafkaConsumer<String, String>(consumerProperties)
        kafkaConsumer.subscribe(listOf(kafkaTopic))
        CoroutineScope(executorCoroutineDispatcher).launch {
            try {
                while (!closingRequest.get()) {
                    val consumerRecords: ConsumerRecords<String, String> = kafkaConsumer.poll(Duration.ofSeconds(1))

                    consumerRecords.forEach {
                        sink.tryEmitNext(it.value())
                    }
                }
            } catch (e: Exception) {
                sink.tryEmitError(e)
            } finally {
                log.info("closing connexion to kafka")
                kafkaConsumer.close(Duration.ofSeconds(3))
            }
        }

    }

}