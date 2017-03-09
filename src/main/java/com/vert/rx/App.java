package com.vert.rx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class App extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(App.class);
    private HttpClient httpClient;

    @Override
    public void start(Future<Void> fut){
        vertx
                .createHttpServer()
                .requestHandler(setup()::accept)
                .listen(9090, result -> {
                    if (result.succeeded()) {
                        logger.info("Verticle Started successfully.");
                        httpClientSetup();
                        fut.complete();
                    } else {
                        logger.error("Failed starting server: " + result.cause().getMessage(),result.cause());
                        fut.fail(result.cause());
                    }
                });


    }

    private Router setup(){
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end("{}");
        });
        router.route("/get").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            get("https://jsonplaceholder.typicode.com/users/")
                    .concatMap(RxHelper::toObservable)
                    .reduce(Buffer.buffer(), Buffer::appendBuffer)
                    .map(buffer -> buffer.toString("UTF-8"))
                    .subscribe(r ->
                        response
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(r)
                    );
        });

        return router;
    }

    public Observable<HttpClientResponse> get(String url) {
        return Observable.create(subscriber -> {
            HttpClientRequest httpClientRequest = httpClient.requestAbs(HttpMethod.GET, url);
            httpClientRequest.exceptionHandler(e -> logger.error(e.getCause().getMessage()) );
            Observable<HttpClientResponse> resp = RxHelper.toObservable(httpClientRequest);
            resp.subscribe(subscriber);
            httpClientRequest.end();
        });
    }

    private void httpClientSetup(){
        HttpClientOptions httpClientOptions = new HttpClientOptions();
        httpClientOptions.setSsl(true);
        httpClientOptions.setTrustAll(true);
        httpClientOptions.setVerifyHost(false);
        httpClient = vertx.createHttpClient(httpClientOptions);
    }
}
