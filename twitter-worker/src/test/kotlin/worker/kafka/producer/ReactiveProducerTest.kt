package worker.kafka.producer

import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test

internal class ReactiveProducerTest {

    private val stringSerializer: StringSerializer = StringSerializer()
    val mockProducer: MockProducer<String, String> = MockProducer(true, stringSerializer, stringSerializer)

    @Test
    fun sendTweetToKafka() {

//        ReactiveProducer("fakeTopic",mockProducer).sendTweetToKafka(SimpleTweet("id"))
    }
}