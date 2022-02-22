package worker.kafka.producer

import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import twitter.tweet.SimpleTweet
import kotlin.test.assertEquals


internal class ReactiveProducerTest {

    private val stringSerializer: StringSerializer = StringSerializer()
    val mockProducer: MockProducer<String, String> = MockProducer(true, stringSerializer, stringSerializer)

    @Test
    fun sendTweetToKafka() {

        ReactiveProducer("fakeTopic", mockProducer).sendTweetToKafka(SimpleTweet("id", "text")).subscribe()


        val actualList: List<Pair<String, String>> =
            mockProducer.history().map { it.key() to it.value() }

        assertEquals(listOf("id" to SimpleTweet("id", "text").toJson()), actualList)
    }
}