package twitter

import com.github.scribejava.core.model.Response
import io.github.redouane59.twitter.IAPIEventListener
import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.dto.tweet.Tweet
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import twitter.tweet.SimpleTweet
import java.util.concurrent.Future


class TwitterClientAdapter(
    private val twitterClient: TwitterClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var connexion: Future<Response>
    private val flux: Flux<SimpleTweet> = Flux.push { sink ->
        sink.onDispose { disconnect() }
        log.info("connecting to twitter...")
        connexion = twitterClient.startFilteredStream(TwitterStreamingListener(sink))
    }
    private val multicastFlux = flux.share()

    fun stream(): Flux<SimpleTweet> = flux
    fun multicastStream(): Flux<SimpleTweet> = multicastFlux

    private fun disconnect() {
        log.info("stopping twitter connection")
        if (connexion.get().isSuccessful)
            twitterClient.stopFilteredStream(connexion)
    }
}

class TwitterStreamingListener(private val listener: FluxSink<SimpleTweet>) : IAPIEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun onStreamError(httpCode: Int, error: String) {
        log.error("onStreamError")
        listener.error(TwitterStreamError("onStreamError $httpCode, $error"))
    }

    override fun onTweetStreamed(tweet: Tweet) {
        listener.next(SimpleTweet(tweet.id, tweet.text, tweet.entities.hashtags?.map{ it.text} ?: emptyList(),tweet.retweetCount,tweet.lang))
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
