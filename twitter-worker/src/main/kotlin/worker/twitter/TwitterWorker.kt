package worker.twitter

import com.github.scribejava.core.model.Response
import io.github.redouane59.twitter.IAPIEventListener
import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.dto.tweet.Tweet
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.Future
import java.util.logging.Logger


class TwitterWorker(
    private val twitterClient: TwitterClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var connexion: Future<Response>
    private val flux: Flux<Tweet> = Flux.create {
        it.onDispose { stop() }
        log.info("connecting to twitter...")
        connexion = twitterClient.startFilteredStream(TwitterStreamingListener(it))
    }

    fun stream(): Flux<Tweet> = flux
    fun multicastStream(): Flux<Tweet> = flux.share()

    private fun stop() {
        log.info("stopping twitter connection")
        twitterClient.stopFilteredStream(connexion)
    }
}

class TwitterStreamingListener(private val listener: FluxSink<Tweet>) : IAPIEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onStreamError(httpCode: Int, error: String) {
        log.error("onStreamError")
        listener.error(TwitterStreamError("onStreamError $httpCode, $error"))
    }

    override fun onTweetStreamed(tweet: Tweet) {
        log.info("tweet received ${tweet.id}")
        listener.next(tweet)
    }

    override fun onUnknownDataStreamed(json: String) {
        listener.error(TwitterStreamUnknownData(json))
    }

    override fun onStreamEnded(e: Exception) {
        listener.error(TwitterStreamEnded(e))
    }

}

class TwitterStreamError(s: String) : Throwable(s)
class TwitterStreamUnknownData(s: String) : Throwable(s)
class TwitterStreamEnded(e: Throwable) : Throwable(e)
