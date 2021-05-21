package com.pcxg.fitools.utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class ElasticsearchClientConfig extends AbstractElasticsearchConfiguration{
    private static final Logger logger = LogManager.getLogger(SSHConnection.class);
    @Value("${store.es.url}")
    private String esURL;

    @Override
    @Bean(name = "restClient")
    public RestHighLevelClient elasticsearchClient() {
//        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
//                .connectedTo(esURL)
//                .withConnectTimeout(Duration.ofSeconds(7200))
//                .withSocketTimeout(Duration.ofSeconds(7200))
//                .build();
//
//        return RestClients.create(clientConfiguration).rest();
        String[] info = esURL.split(":");
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(info[0], Integer.parseInt(info[1]), "http"))
                        .setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
                            // 该方法接收一个RequestConfig.Builder对象，对该对象进行修改后然后返回。
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(
                                    RequestConfig.Builder requestConfigBuilder) {
                                return requestConfigBuilder
                                        .setConnectTimeout(6000 * 1000) // 连接超时（默认为1秒）
                                        .setSocketTimeout(6000 * 1000)
                                        .setConnectionRequestTimeout(6000*1000);
                            }
                        }).setHttpClientConfigCallback(httpAsyncClientBuilder -> {
                            httpAsyncClientBuilder.disableAuthCaching();
                            httpAsyncClientBuilder.setKeepAliveStrategy(CustomConnectionKeepAliveStrategy.INSTANCE);
                            return httpAsyncClientBuilder;
                })
        );
    }

}
class CustomConnectionKeepAliveStrategy extends DefaultConnectionKeepAliveStrategy {
    /**
     * 最大keep alive的时间（分钟）
     * 这里默认为5分钟，可以根据实际情况设置。可以观察客户端机器状态为TIME_WAIT的TCP连接数，如果太多，可以增大此值。
     */
    private final long MAX_KEEP_ALIVE_MINUTES = 5;

    public static final CustomConnectionKeepAliveStrategy INSTANCE = new CustomConnectionKeepAliveStrategy();

    private CustomConnectionKeepAliveStrategy() {
        super();
    }

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long keepAliveDuration = super.getKeepAliveDuration(response, context);
        // <0 为无限期keepalive
        // 将无限期替换成一个默认的时间
        if (keepAliveDuration < 0) {
            return TimeUnit.MINUTES.toMillis(MAX_KEEP_ALIVE_MINUTES);
        }
        return keepAliveDuration;
    }
}