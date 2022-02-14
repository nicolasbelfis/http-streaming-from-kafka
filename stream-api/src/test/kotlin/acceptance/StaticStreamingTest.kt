package acceptance


import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body
import org.springframework.test.web.reactive.server.returnResult
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import simple.Application


@SpringBootTest(classes = [(Application::class)])
@AutoConfigureWebTestClient(timeout = "30000")
class StaticStreamingTest {


    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `given a client subscribed to flux stream, when 0 messages posted, receive 1 comment for subscription started`() {

        val responseBodyFlux = webTestClient.get().uri("/flux/stream")
            .exchange()
            .expectStatus().isOk
            .returnResult<ServerSentEvent<String>>().responseBody
            .map { it.toString() }
            .take(1)

        StepVerifier.create(responseBodyFlux)
            .expectNext(ServerSentEvent.builder<String>().comment("subscription started").build().toString())
            .verifyComplete()
    }

    @Test
    fun `given a client subscribed to flux stream, when 1 message posted, receive 1 messsage`() {

        val responseBodyFlux = webTestClient.get().uri("/flux/stream")
            .exchange()
            .expectStatus().isOk
            .returnResult<ServerSentEvent<String>>().responseBody
            .map { it.toString() }
            .take(2)

        webTestClient.post().uri("/flux/message")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Flux.just("{\"message\": \"sample data\"}"))
            .exchange()
            .expectStatus().isOk

        StepVerifier.create(responseBodyFlux)
            .expectNext(ServerSentEvent.builder<String>().comment("subscription started").build().toString())
            .expectNext(ServerSentEvent.builder("sample data").build().toString())
            .verifyComplete()
    }
}

