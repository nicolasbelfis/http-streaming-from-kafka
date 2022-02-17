package acceptance


import FakeTweet
import com.ninjasquad.springmockk.MockkBean
import io.github.redouane59.twitter.dto.tweet.Tweet
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
import worker.twitter.TwitterStreamError
import worker.twitter.TwitterWorker


@SpringBootTest(classes = [(Application::class)])
@AutoConfigureWebTestClient(timeout = "30000")
@ActiveProfiles("direct-twitter")
class TweetStreamingTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @MockkBean
    private lateinit var twitterWorker: TwitterWorker

    @Test
    fun `given a client subscribed to tweets sse, should receive notification and 1 tweet`() {

        every { twitterWorker.multicastStream() } returns Flux.just<Tweet>(FakeTweet("text")).share()
        val responseBodyFlux = webTestClient.get().uri("/stream/tweets/sse")
            .exchange()
            .expectStatus().isOk
            .returnResult<ServerSentEvent<String>>().responseBody
            .map { it.toString() }
            .take(2)

        StepVerifier.create(responseBodyFlux)
            .expectNext(ServerSentEvent.builder<String>().comment("subscription started").build().toString())
            .expectNext(ServerSentEvent.builder("text").build().toString())
            .verifyComplete()
    }

    @Test
    fun `should receive notification and end notification where error`() {

        every { twitterWorker.multicastStream() } returns
            Flux.error(TwitterStreamError("err"))

        val responseBodyFlux = webTestClient.get().uri("/stream/tweets/sse")
            .exchange()
            .expectStatus().isOk
            .returnResult<ServerSentEvent<String>>().responseBody
            .map { it.toString() }
            .take(3)

        StepVerifier.create(responseBodyFlux)
            .expectNext(ServerSentEvent.builder<String>().comment("subscription started").build().toString())
            .expectNext(ServerSentEvent.builder<String>().event("subscription ended, because err").build().toString())
            .verifyComplete()
    }
}



