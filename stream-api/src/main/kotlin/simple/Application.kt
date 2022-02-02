package simple

import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient
import simple.logger.Loggers
import simple.streaming.FlowStreamService
import simple.streaming.FluxStreamService
import simple.streaming.StreamService
import worker.twitter.TwitterWorker

@SpringBootApplication
@EnableWebFlux
class Application {

    @Bean(name = ["flowStream"])
    fun flowStream(): StreamService<String, Flow<String>> = FlowStreamService()

    @Bean(name = ["fluxStream"])
    fun fluxStream() = FluxStreamService()


    @Bean(name = ["defaultWebClient"])
    fun webClient(): WebClient = WebClient.builder().baseUrl("http://localhost:8080").build()

    @Profile("direct-twitter")
    @Bean
    fun startTwitterWorker(
        @Value("\${twitter.bearer}") bearerToken: String
    ): TwitterWorker {
        val twitterWorker = TwitterWorker(bearerToken)
        try {
            twitterWorker.stream().blockFirst()
            twitterWorker.stop()
        } catch (e: Exception) {
            Loggers.print("cannot start twitter worker, app will close")
            throw e
        }
        return twitterWorker
    }
}

fun main(args: Array<String>) {
    try {
        val runApplication: ConfigurableApplicationContext = runApplication<Application>(*args)
    } catch (e: Exception) {
        Loggers.error(e, "app failed to start")
    }

}
