package simple.logger

object Loggers {

    fun print(value: String) {
        println(Thread.currentThread().name + " : " + value)
    }

    fun error(throwable: Throwable, value: String) {
        throwable.printStackTrace()
        println(Thread.currentThread().name + " : " + value)
    }
}
