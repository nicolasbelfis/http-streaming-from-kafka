package simple

import kotlinx.coroutines.flow.Flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient
import simple.streaming.FlowStreamService
import simple.streaming.FluxStreamService
import simple.streaming.StreamService

@SpringBootApplication
@EnableWebFlux
class Application {

    @Bean(name = ["flowStream"])
    fun flowStream(): StreamService<String, Flow<String>> = FlowStreamService()

    @Bean(name = ["fluxStream"])
    fun fluxStream() = FluxStreamService()


    @Bean(name = ["defaultWebClient"])
    fun webClient(): WebClient = WebClient.builder().baseUrl("http://localhost:8080").build()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
