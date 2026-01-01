package com.moona.productsmanager.moonaproductsmanager.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient apiWebClient(@Autowired ApiProperties apiProperties) {
        TcpClient tcpClient = TcpClient.create()
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, apiProperties.getTimeout().getConnectMs())
            .doOnConnected(conn -> conn
                .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(apiProperties.getTimeout().getReadMs(), java.util.concurrent.TimeUnit.MILLISECONDS))
                .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(apiProperties.getTimeout().getReadMs(), java.util.concurrent.TimeUnit.MILLISECONDS))
            );

        HttpClient httpClient = HttpClient.from(tcpClient)
            .responseTimeout(Duration.ofMillis(apiProperties.getTimeout().getReadMs()));

        return WebClient.builder()
            .baseUrl(apiProperties.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build())
            .defaultHeader("Authorization", "Bearer " + apiProperties.getToken())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}

