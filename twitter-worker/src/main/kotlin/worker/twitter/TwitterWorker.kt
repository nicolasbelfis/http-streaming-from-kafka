package worker.twitter

import com.github.scribejava.core.model.Response
import io.github.redouane59.twitter.IAPIEventListener
import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.dto.tweet.Tweet
import io.github.redouane59.twitter.signature.TwitterCredentials
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.concurrent.Future
import java.util.stream.Stream


class TwitterWorker(
    twitterBearerToken: String,
) {

    private lateinit var connexion: Future<Response>

    private val twitterClient = TwitterClient(
        TwitterCredentials.builder()
            .bearerToken(twitterBearerToken)
            .build()
    )
    private val flux: Flux<Tweet> = Flux.create<Tweet> {
        connexion = twitterClient.startFilteredStream(TwitterStreamingListener(it))
    }.share()

    fun stream() = flux.doOnCancel { this.stop() }

    fun stop() {
        twitterClient.stopFilteredStream(connexion)
    }
}

class TwitterStreamingListener(private val listener: FluxSink<Tweet>) : IAPIEventListener {

    override fun onStreamError(httpCode: Int, error: String) {
        listener.error(TwitterStreamError("onStreamError $httpCode, $error"))
    }

    override fun onTweetStreamed(tweet: Tweet) {
        Thread.sleep(500)
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
