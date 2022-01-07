package simple.logger

object Loggers {

    fun print(value: String) {
        println(Thread.currentThread().name + " : " + value)
    }
}
