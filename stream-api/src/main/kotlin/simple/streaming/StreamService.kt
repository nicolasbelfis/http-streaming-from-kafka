package simple.streaming

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Flux
import java.lang.NullPointerException

interface StreamService<T, STREAMABLE> {
    fun stream(): STREAMABLE
    fun emit(message: T)

}

class FlowStreamService : StreamService<String, Flow<String>> {
    private val sharedFlow: MutableSharedFlow<String> = MutableSharedFlow()
    override fun stream(): Flow<String> {
        return sharedFlow
    }

    override fun emit(message: String) {
        runBlocking {
            sharedFlow.emit(message)
        }
    }

}

class FluxStreamService : StreamService<String, Flux<String>> {
    private var listener: (String) -> Unit = {}
    private var flux: Flux<String> = Flux.create<String> {
        listener = { msg: String -> it.next(msg) }
    }
        .map { if (it == "null") throw NullPointerException(); it }
        .map { if (it == "error") throw IllegalArgumentException(); it }
        .share()

    override fun stream(): Flux<String> {
        return flux
    }

    override fun emit(message: String) {
        listener(message)
    }

}
