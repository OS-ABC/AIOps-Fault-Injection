package com.pcxg.fitools.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.pcxg.fitools.entity.mysql.Event;
import com.pcxg.fitools.entity.FaultInjectInfo;
import com.pcxg.fitools.entity.SSHConf;
import com.pcxg.fitools.entity.mysql.Item;
import com.pcxg.fitools.env.HadoopDockerEnv;
import com.pcxg.fitools.env.SockShopEnv;
import com.pcxg.fitools.tools.FaultInjection;
import com.pcxg.fitools.tools.chaosblade.ChaosBladeInjection;
import com.pcxg.fitools.tools.ssfi.SSFIInjection;
import com.pcxg.fitools.tools.ssfi.SSFITool;
import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class FaultInjectServiceImpl implements FaultInjectService{
    private static final Logger logger = LogManager.getLogger(FaultInjectServiceImpl.class);

    @Autowired
    SSHConf sshConf;
    @Autowired
    DataCollector dataCollector;

    private SSHConnection ssh;

    private Date lastTime;

    @Override
    public boolean inject(FaultInjectInfo faultInjectInfo) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            FaultInjection faultInjection = init(faultInjectInfo, countDownLatch);
            //本次故障注入的配置有问题，直接返回错误
            if (!faultInjection.check(faultInjectInfo)) {
                return false;
            }
            if (lastTime == null) {
                lastTime = faultInjectInfo.getEnvEndTime();
            } else {
                if (!lastTime.before(faultInjectInfo.getEnvStartTime())) {
                    return false;
                }
            }
            //注入故障
            faultInjection.inject();

            //新开一个线程，等待故障注入实验结束后收集数据
            Thread collect = new Thread(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
                logger.info("Start collecting data...");
                //查询数据库，整合map的数据，存到数据库里
                //jdbcTemplate.execute("SET SESSION group_concat_max_len = 100000;");

                Map<String, String[]> faultInjectionMap = faultInjection.getInjectionMap();
                dataCollector.collect(faultInjectInfo, faultInjectionMap);
            });

            collect.start();
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }


        return true;
    }

    private FaultInjection init(FaultInjectInfo faultInjectInfo, CountDownLatch countDownLatch) throws Exception {
        ssh = new SSHConnection(sshConf.getHost(), sshConf.getUsername(),
                sshConf.getPassword(), sshConf.getPort(), sshConf.getWorkspace());
        String envType = faultInjectInfo.getEnvType().toUpperCase();
        switch (envType) {
            case "HADOOP DOCKER":
                faultInjectInfo.setEnvironment(new HadoopDockerEnv());
                break;
            case "SOCKSHOP DOCKER":
                faultInjectInfo.setEnvironment(new SockShopEnv());
                break;
            default:
                throw new Exception("Unknown env type!");
        }
        String toolType = faultInjectInfo.getToolType().toUpperCase();
        FaultInjection faultInjection;
        switch (toolType) {
            case "SSFI":
                faultInjection = new SSFIInjection(faultInjectInfo, ssh, countDownLatch);
                break;
            case "CHAOSBLADE":
                faultInjection = new ChaosBladeInjection(faultInjectInfo, ssh, countDownLatch);
                break;
            default:
                throw new Exception("Unknown tool type!");
        }

//        if (faultInjectInfo.getEnvType().equals("Hadoop Docker")) {
//            faultInjectInfo.setEnvironment(new HadoopDockerEnv());
//        } else {
//            throw new Exception("Unknown env type!");
//        }
        return faultInjection;
    }

}
