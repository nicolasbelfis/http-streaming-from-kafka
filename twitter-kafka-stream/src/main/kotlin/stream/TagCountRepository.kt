package stream

import com.mongodb.client.model.FindOneAndReplaceOptions
import com.mongodb.reactivestreams.client.MongoDatabase
import org.bson.Document
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CountDownLatch

class TagCountRepository(private val database: MongoDatabase) {


    fun update(tag: String, count: String) {
        database.getCollection("tagCounts").findOneAndReplace(
            Document("_id", tag), Document("_id", tag).append("count", count), FindOneAndReplaceOptions().upsert(true))
            .subscribe(InsertOneResultSubscriber())
    }


    private class InsertOneResultSubscriber<T> : Subscriber<T> {
        override fun onSubscribe(s: Subscription) {
            s.request(1)
        }

        override fun onNext(r: T) {
            println(r)
        }

        override fun onError(e: Throwable) {
            throw e
        }

        override fun onComplete() {
        }
    }
}
