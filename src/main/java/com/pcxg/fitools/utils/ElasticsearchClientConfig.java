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
                            // ?????????????????????RequestConfig.Builder???????????????????????????????????????????????????
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(
                                    RequestConfig.Builder requestConfigBuilder) {
                                return requestConfigBuilder
                                        .setConnectTimeout(6000 * 1000) // ????????????????????????1??????
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
     * ??????keep alive?????????????????????
     * ???????????????5??????????????????????????????????????????????????????????????????????????????TIME_WAIT???TCP????????????????????????????????????????????????
     */
    private final long MAX_KEEP_ALIVE_MINUTES = 5;

    public static final CustomConnectionKeepAliveStrategy INSTANCE = new CustomConnectionKeepAliveStrategy();

    private CustomConnectionKeepAliveStrategy() {
        super();
    }

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        long keepAliveDuration = super.getKeepAliveDuration(response, context);
        // <0 ????????????keepalive
        // ??????????????????????????????????????????
        if (keepAliveDuration < 0) {
            return TimeUnit.MINUTES.toMillis(MAX_KEEP_ALIVE_MINUTES);
        }
        return keepAliveDuration;
    }
}