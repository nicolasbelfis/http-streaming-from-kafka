package worker


import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Printed
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
    fun commandLineRunnerTwitterStreaming(
        @Value("\${kafka.host}") kafkaHost: String,
        @Value("\${kafka.topic}") kafkaTopic: String,
    ): CommandLineRunner = CommandLineRunner {
        val topology = buildTopology(kafkaTopic)
        val kafkaStreams = KafkaStreams(topology, getProperties(kafkaHost))
        kafkaStreams.start()
    }

    private fun buildTopology(kafkaTopic: String): Topology {
        val stringSerde = Serdes.String()

        val kStreamBuilder = StreamsBuilder()
        val tweetStream: KStream<String, String> =
            kStreamBuilder.stream(kafkaTopic, Consumed.with(stringSerde, stringSerde))

        tweetStream.print(Printed.toSysOut())

        return kStreamBuilder.build()
    }

    private fun getProperties(kafkaHost: String): Properties {
        val settings = Properties()
        settings[StreamsConfig.APPLICATION_ID_CONFIG] = "tweets-streaming"
        settings[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaHost
        settings[StreamsConfig.STATE_DIR_CONFIG] = ""
        settings[StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG] = WallclockTimestampExtractor::class.java
        return settings
    }

}
