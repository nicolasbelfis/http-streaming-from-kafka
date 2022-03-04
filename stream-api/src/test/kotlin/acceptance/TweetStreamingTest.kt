package acceptance


import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import simple.Application
import twitter.TwitterClientAdapter
import twitter.TwitterStreamError
import twitter.tweet.SimpleTweet


@SpringBootTest(classes = [(Application::class)])
@AutoConfigureWebTestClient(timeout = "30000")
@ActiveProfiles("direct-twitter")
class TweetStreamingTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var twitterClientAdapter: TwitterClientAdapter

    @Test
    fun `given a client subscribed to tweets sse, should receive 1 tweet`() {

        val simpleTweet = SimpleTweet("id1",
            "text",
            emptyList(),
            1,
            "en")
        every { twitterClientAdapter.multicastStream() } returns Flux.just(simpleTweet).share()
        val responseBodyFlux = webTestClient.get().uri("/stream/sseTweets")
            .exchange()
            .expectStatus().isOk
            .returnResult<ServerSentEvent<String>>().responseBody
            .map { it.toString() }
            .take(1)

        StepVerifier.create(responseBodyFlux)
            .expectNext(ServerSentEvent.builder(simpleTweet.toJson()).build().toString())
            .verifyComplete()
    }

    @Test
    fun `should receive notification and end notification where error`() {

        every { twitterClientAdapter.multicastStream() } returns
            Flux.error(TwitterStreamError("err"))

        val responseBodyFlux = webTestClient.get().uri("/stream/sseTweets")
            .exchange()
            .expectStatus().isOk
            .returnResult<ServerSentEvent<String>>().responseBody
            .map { it.toString() }
            .take(3)

        StepVerifier.create(responseBodyFlux)
            .expectNext(ServerSentEvent.builder<String>().event("subscription ended, because err").build().toString())
            .verifyComplete()
    }
}



