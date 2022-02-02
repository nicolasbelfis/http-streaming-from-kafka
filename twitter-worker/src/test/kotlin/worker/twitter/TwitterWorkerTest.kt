package worker.twitter

import org.junit.jupiter.api.Test
import reactor.test.StepVerifier
import reactor.util.retry.Retry
import java.lang.NullPointerException

internal class TwitterWorkerTest {
    private val invalidToken = "AAA0GTeg3AGtM%3DKGf1"

    @Test
    fun `retry flux example,fails then retries then rethrow a new exception on exhaust`() {

        val retryWhen = TwitterWorker(invalidToken).stream()
            .retryWhen(
                Retry.indefinitely()
                    .maxAttempts(3L)
                    .filter { it is TwitterStreamError }
                    .onRetryExhaustedThrow { t, u -> Exhausted("retries exausted") }
            )

        StepVerifier.create(retryWhen).expectError(Exhausted::class.java).verify()
    }

    @Test
    fun `retry flux example,fails then doesnt retry then rethrow the non retryable exception`() {

        val retryWhen = TwitterWorker(invalidToken).stream()
            .retryWhen(
                Retry.indefinitely()
                    .maxAttempts(3L)
                    .filter { it is NullPointerException }
                    .onRetryExhaustedThrow { t, u -> Exhausted("retries exausted") }
            )

        StepVerifier.create(retryWhen).expectError(TwitterStreamError::class.java).verify()
    }
}

class Exhausted(s: String) : Throwable(s)
