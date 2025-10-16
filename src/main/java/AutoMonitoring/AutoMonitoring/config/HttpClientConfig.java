package AutoMonitoring.AutoMonitoring.config;


import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HttpClientConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService httpExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor(); // JDK 21
    }

    @Bean
    public HttpClient httpClient(ExecutorService httpExecutor){
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)              // HTTP/2 멀티플렉싱
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(3))           // 연결 타임아웃은 짧게(권장 2~3s)
                .executor(httpExecutor)
                .build();
    }

    @Bean
    public WebClient webClient(WebClient.Builder builder){
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

        return builder.clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }


}
