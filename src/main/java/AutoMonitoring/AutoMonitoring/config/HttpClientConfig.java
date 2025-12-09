package AutoMonitoring.AutoMonitoring.config;


import AutoMonitoring.AutoMonitoring.domain.monitoringQueue.util.MonitoringConfigHolder;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final MonitoringConfigHolder monitoringConfigHolder;


    @Bean(destroyMethod = "shutdown")
    public ExecutorService httpExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor(); // JDK 21
    }

    @Bean
    public HttpClient httpClient(ExecutorService httpExecutor){
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)              // HTTP/2 멀티플렉싱
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(monitoringConfigHolder.getHttpRequestTimeoutMillis().get()))
                .executor(httpExecutor)
                .build();
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder){
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(Duration.ofMillis(monitoringConfigHolder.getHttpRequestTimeoutMillis().get()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(12))   // 읽는 중 12s 무응답이면 타임아웃
                        .addHandlerLast(new WriteTimeoutHandler(10))  // 쓰는 중 10s 무응답이면 타임아웃
                );

        return builder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }


}
