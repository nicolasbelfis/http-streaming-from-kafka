package stream

import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.ForeachAction
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.kstream.Produced
import twitter.tweet.ObjectMapperKotlin
import twitter.tweet.SimpleTweet

fun buildTopology(kafkaTopic: String, tagCountRepository: TagCountRepository): Topology {
    val keyDeserialiser = Serdes.String()

    val kStreamBuilder = StreamsBuilder()
    val tweetStream: KStream<String, SimpleTweet> =
        kStreamBuilder.stream(kafkaTopic, Consumed.with(keyDeserialiser, TweetSerde()))

    val kTable: KTable<String, Long> = tweetStream
        .flatMap { key, value -> Iterable { value.hashTags.map { hTag -> KeyValue(key, hTag) }.listIterator() } }
        .groupBy({ k, hashTag -> hashTag }, Grouped.with(Serdes.String(), Serdes.String())).count()

    val countStream: KStream<String, Long> = kTable.toStream()
    countStream.to("topic-count", Produced.with(Serdes.String(), Serdes.Long()))
    countStream.print(Printed.toSysOut())
    countStream.peek { k, v -> tagCountRepository.update(k, v.toString()) }

//        val aggregate: KTable<String, Top3Tags> = countStream.groupByKey()
//            .aggregate(
//                { Top3Tags(listOf()) },
//                { tag, count, top3Map ->
//                    if (top3Map.count.size < 3) Top3Tags(top3Map.count + (tag to count))
//                    else Top3Tags(
//                        top3Map.count
//                            .sortedByDescending { it.second }
//                            .take(3)
//                            .map { p -> if (p.second < count) tag to count else p }
//                    )
//                }, Materialized.with(Serdes.String(), Top3TagsSerde())
//            )
//
//        aggregate.toStream().print(Printed.toSysOut())
    return kStreamBuilder.build()
}


class TweetSerde : Serde<SimpleTweet> {
    override fun serializer(): Serializer<SimpleTweet> =
        Serializer<SimpleTweet> { s, t -> ObjectMapperKotlin.writeValueAsBytes(t) }

    override fun deserializer(): Deserializer<SimpleTweet> =
        Deserializer { topic, data -> ObjectMapperKotlin.readValue(data, SimpleTweet::class.java) }

}
//
//class Top3TagsSerde : Serde<Top3Tags> {
//    override fun serializer(): Serializer<Top3Tags> =
//        Serializer<Top3Tags> { s, t -> ObjectMapperKotlin.writeValueAsBytes(t) }
//
//    override fun deserializer(): Deserializer<Top3Tags> =
//        Deserializer { topic, data -> ObjectMapperKotlin.readValue(data, Top3Tags::class.java) }
//
//}
//
//data class Top3Tags(val count: List<Pair<String, Long>>)
