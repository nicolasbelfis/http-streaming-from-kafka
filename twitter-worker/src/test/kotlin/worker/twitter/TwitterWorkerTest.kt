package worker.twitter

import com.github.scribejava.core.model.Response
import io.github.redouane59.twitter.IAPIEventListener
import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.signature.TwitterCredentials
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import reactor.util.retry.Retry
import java.util.concurrent.CompletableFuture

internal class TwitterWorkerTest {
    private val twitterClientMock = mockk<TwitterClient>()
    private val invalidToken = "AAA0GTeg3AGtM%3DKGf1"

    @Test
    fun `retry flux example,fails then retries then rethrow a new exception on exhaust`() {

        val twitterClient = TwitterClient(
            TwitterCredentials.builder()
                .bearerToken(invalidToken)
                .build()
        )
        val retryWhen = TwitterWorker(twitterClient).multicastStream()
            .retryWhen(
                Retry.indefinitely()
                    .maxAttempts(1L)
                    .filter { it is TwitterStreamError }
                    .onRetryExhaustedThrow { t, u -> Exhausted("retries exhausted") }
            )

        StepVerifier.create(retryWhen,1).expectError(Exhausted::class.java).verify()
    }

    @Test
    fun `retry flux example,fails then doesnt retry then rethrow the non retryable exception`() {
        val twitterClient = TwitterClient(
            TwitterCredentials.builder()
                .bearerToken(invalidToken)
                .build()
        )
        val retryWhen = TwitterWorker(twitterClient).multicastStream()
            .retryWhen(
                Retry.indefinitely()
                    .maxAttempts(1L)
                    .filter { it is NullPointerException }
                    .onRetryExhaustedThrow { t, u -> Exhausted("retries exhausted") }
            )

        StepVerifier.create(retryWhen).expectError(TwitterStreamError::class.java).verify()
    }

    @Test
    fun `should open only one connexion at first subscription`() {

        every { twitterClientMock.startFilteredStream(any<IAPIEventListener>()) } returns mockk()
        every { twitterClientMock.stopFilteredStream(any()) } returns true
        val flux = TwitterWorker(twitterClientMock).multicastStream().log()
        flux.subscribe()
        verify(exactly = 1) { twitterClientMock.startFilteredStream(any<IAPIEventListener>()) }
        flux.subscribe()
        verify(exactly = 1) { twitterClientMock.startFilteredStream(any<IAPIEventListener>()) }
    }

    @Test
    fun `should close connexion when all subcribers cancelled`() {
        every { twitterClientMock.startFilteredStream(any<IAPIEventListener>()) } returns successfulResponse()
        every { twitterClientMock.stopFilteredStream(any()) } returns true
        val flux = TwitterWorker(twitterClientMock).multicastStream().log()

        val disposable1 = flux.subscribe()
        val disposable2 = flux.subscribe()

        disposable1.dispose()
        verify(exactly = 0) { twitterClientMock.stopFilteredStream(any()) }
        disposable2.dispose()
        verify(exactly = 1) { twitterClientMock.stopFilteredStream(any()) }

    }

    private fun successfulResponse() = CompletableFuture.completedFuture(
        Response(200, "", emptyMap(), "")
    )
}


class Exhausted(s: String) : Throwable(s)
