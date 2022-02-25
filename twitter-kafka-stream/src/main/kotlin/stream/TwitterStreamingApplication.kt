package stream


import com.mongodb.reactivestreams.client.MongoClients
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.util.*

fun main(args: Array<String>) {
    runApplication<TwitterStreamingApplication>(*args)
}

@SpringBootApplication
class TwitterStreamingApplication {


    @Bean
    fun mongoRepository(): TagCountRepository {

        return TagCountRepository(MongoClients.create("mongodb://127.0.0.1:27017")
            .getDatabase("demoTweet"))
    }

    @Bean
    fun commandLineRunnerTwitterStreaming(
        @Value("\${kafka.host}") kafkaHost: String,
        @Value("\${kafka.topic}") kafkaTopic: String,
        tagCountRepository: TagCountRepository
    ): CommandLineRunner = CommandLineRunner {
        val topology = buildTopology(kafkaTopic,tagCountRepository)
        val kafkaStreams = KafkaStreams(topology, getProperties(kafkaHost))
        kafkaStreams.start()
    }


    private fun getProperties(kafkaHost: String): Properties {
        val settings = Properties()
        settings[StreamsConfig.APPLICATION_ID_CONFIG] = "tweets-streaming"
        settings[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaHost
        settings[StreamsConfig.STATE_DIR_CONFIG] = "state"
        settings[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 2000L
        settings[StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG] = WallclockTimestampExtractor::class.java
        return settings
    }

}
