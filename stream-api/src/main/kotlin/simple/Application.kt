package simple

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.signature.TwitterCredentials
import kotlinx.coroutines.flow.Flow
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.LoopResources
import simple.logger.Loggers
import simple.streaming.FlowStreamService
import simple.streaming.FluxStreamService
import simple.streaming.StreamService
import simple.streaming.TweetConsumer
import twitter.TwitterClientAdapter
import java.util.*


@SpringBootApplication
@EnableWebFlux
class Application {

    @Bean(name = ["flowStream"])
    fun flowStream(): StreamService<String, Flow<String>> = FlowStreamService()

    @Bean(name = ["fluxStream"])
    fun fluxStream() = FluxStreamService()


    @Bean(name = ["defaultWebClient"])
    fun webClient(): WebClient = WebClient.builder().baseUrl("http://localhost:8080").build()

    @Profile("single-thread-event-loop")
    @Bean
    fun reactorResourceFactory(): ReactorResourceFactory {
        val factory = ReactorResourceFactory()
        factory.isUseGlobalResources = false
        factory.loopResources = LoopResources.create("event-loop", 1, 1, true);
        return factory
    }

    @Profile("direct-twitter")
    @Bean
    fun twitterWorker(
        @Value("\${twitter.bearer}") bearerToken: String,
    ): TwitterClientAdapter {
        val twitterClient = TwitterClient(
            TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build()
        )
        val twitterClientAdapter = TwitterClientAdapter(twitterClient)
        try {
            twitterClientAdapter.multicastStream().blockFirst()
        } catch (e: Exception) {
            Loggers.print("cannot start twitter worker, app will close")
            throw e
        }
        return twitterClientAdapter
    }


    @Profile("kafka-twitter")
    @Bean
    fun tweetConsumer(
        @Value("\${kafka-source.topic}") kafkaTopic: String,
        @Value("\${kafka-source.host}") kafkaHost: String,
        objectMapper: ObjectMapper
    ): TweetConsumer {
        return TweetConsumer(getProperties(kafkaHost, StringDeserializer::class.java), kafkaTopic)

    }

    @Profile("kafka-twitter")
    @Bean
    fun countTagConsumer(
        @Value("\${kafka-count-tags.topic}") kafkaTopic: String,
        @Value("\${kafka-count-tags.host}") kafkaHost: String,
        objectMapper: ObjectMapper
    ): TweetConsumer {
        return TweetConsumer(getProperties(kafkaHost, LongDeserializer::class.java), kafkaTopic)

    }

    private fun getProperties(kafkaHost: String, valueDeserialiser: Class<out Deserializer<*>>): Properties {
        val settings = Properties()

        settings[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaHost
        settings[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] =
            StringDeserializer::class.java
        settings[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
            valueDeserialiser
        settings[ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG] = 200
        settings[ConsumerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG] = 5000
        settings[ConsumerConfig.SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG] = 2000
        settings[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 10000
        settings[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        settings[ConsumerConfig.GROUP_ID_CONFIG] = "twitter-worker-group"

        return settings
    }
}

fun main(args: Array<String>) {
    try {
        val runApplication: ConfigurableApplicationContext = runApplication<Application>(*args)
    } catch (e: Exception) {
        Loggers.error(e, "app failed to start")
    }

}
