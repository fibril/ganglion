import io.vertx.core.Vertx
import io.vertx.ext.web.Router

internal abstract class Controller(vertx: Vertx) {
    internal val router: Router = Router.router(vertx)

    abstract fun mountRoutes(): Router
}