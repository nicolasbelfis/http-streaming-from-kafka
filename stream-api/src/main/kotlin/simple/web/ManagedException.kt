package simple.web

class CancellableException(throwable: Throwable) : Throwable(throwable)
class NonCancellableException(throwable: Throwable) : Throwable(throwable)
