package stream

import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.kstream.Produced
import twitter.tweet.ObjectMapperKotlin
import twitter.tweet.SimpleTweet

fun buildTopology(tweetsTopic: String, tagCountRepository: TagCountRepository): Topology {
    val keyDeserialiser = Serdes.String()

    val kStreamBuilder = StreamsBuilder()
    //consume tweets
    val tweetStream: KStream<String, SimpleTweet> =
        kStreamBuilder.stream(tweetsTopic, Consumed.with(keyDeserialiser, genericSerde(SimpleTweet::class.java)))

    //build state of aggregations
    val kTable: KTable<String, Long> = tweetStream
        .flatMap { tweetId, tweet ->
            Iterable {
                tweet.hashTags.map { hTag -> KeyValue(tweetId, hTag) }.listIterator()
            }
        }
        .groupBy({ _, hashTag -> hashTag }, Grouped.with(Serdes.String(), Serdes.String())).count()

    // send change log of table
    val tagCountChangeLog: KStream<String, Long> = kTable.toStream()
    tagCountChangeLog.to("topic-count", Produced.with(Serdes.String(), Serdes.Long()))
    tagCountChangeLog.print(Printed.toSysOut())
    tagCountChangeLog.peek { k, v -> tagCountRepository.update(k, v.toString()) }

    val aggregate: KTable<String, Top3Tags> = tweetStream
        //extract list of tag with lang
        .flatMap { tweetId, tweet ->
            Iterable {
                tweet.hashTags.map { hTag ->
                    KeyValue(tweet.lang, hTag)
                }.listIterator()
            }
            //group by tag,lang then count
        }.groupBy { lang, tag -> HtagWithLang(lang, tag) }
        .count()
        //emit groups of tag,lang with count
        .toStream()
        //re key messages by lang
        .map { htagWithLang, count -> KeyValue(htagWithLang.lang, htagWithLang.hTag to count) }
        .groupByKey()
        //for each lang,compute top3 tags by count
        .aggregate(
            { Top3Tags(null, null, null) },
            { lang, tagCount, top3 -> computeTop3(tagCount.first, tagCount.second, top3) }
        )

    return kStreamBuilder.build()
}

fun computeTop3(tag: String, count: Long, top3: Top3Tags): Top3Tags {
    return Top3Tags(
        compareWithTop(top3.top1, tag, count),
        if (top3.top2 != null) compareWithTop(top3.top2, tag, count) else null,
        if (top3.top3 != null) compareWithTop(top3.top3, tag, count) else null,
    )
}

private fun compareWithTop(top: CountTag?, tag: String, count: Long): CountTag =
    if (top == null) CountTag(tag, count)
    else if (top.count < count)
        CountTag(tag, count)
    else top

data class Top3Tags(
    val top1: CountTag?,
    val top2: CountTag?,
    val top3: CountTag?,
)

data class CountTag(val tag: String, val count: Long)

data class HtagWithLang(val hTag: String, val lang: String)


class genericSerde<T>(val clazz: Class<T>) : Serde<T> {
    override fun serializer(): Serializer<T> =
        Serializer<T> { s, t -> ObjectMapperKotlin.writeValueAsBytes(t) }

    override fun deserializer(): Deserializer<T> =
        Deserializer { topic, data -> ObjectMapperKotlin.readValue(data, clazz) }

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
