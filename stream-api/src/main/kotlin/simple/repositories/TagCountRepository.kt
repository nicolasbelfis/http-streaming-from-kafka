package simple.repositories

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.reactivestreams.client.FindPublisher
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.Document
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TagCountRepository(private val database: MongoDatabase) {

    fun findTagCountsByTags(tagFilters: List<String>): FindPublisher<Document> {
        return database.getCollection("tagCounts").find(or(tagFilters.map { eq("_id", it) }))
    }

}
