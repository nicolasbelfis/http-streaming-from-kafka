package simple

import io.github.redouane59.twitter.TwitterClient
import io.github.redouane59.twitter.signature.TwitterCredentials
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorResourceFactory
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.LoopResources
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

    @Profile("single-thread-event-loop")
    @Bean
    fun reactorResourceFactory(): ReactorResourceFactory {
        val factory = ReactorResourceFactory()
        factory.isUseGlobalResources = false
        factory.loopResources = LoopResources.create("event-loop", 1, 1, true);
        return factory
    }

    @Profile("direct-twitter")
    @Bean
    fun twitterWorker(
        @Value("\${twitter.bearer}") bearerToken: String
    ): TwitterWorker {
        val twitterClient = TwitterClient(
            TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build()
        )
        val twitterWorker = TwitterWorker(twitterClient)
        try {
            twitterWorker.stream().blockFirst()
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
