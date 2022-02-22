package simple.web.experimental

class CancellableException(throwable: Throwable) : Throwable(throwable)
class NonCancellableException(throwable: Throwable) : Throwable(throwable)
