package worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient

@SpringBootApplication
@EnableWebFlux
class Application {


    @Bean(name = ["defaultWebClient"])
    fun webClient(): WebClient = WebClient.builder().baseUrl("http://localhost:8080").build()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
