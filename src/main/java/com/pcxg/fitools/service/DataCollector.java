package com.pcxg.fitools.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.pcxg.fitools.entity.FaultInjectInfo;
import com.pcxg.fitools.entity.mysql.Event;
import com.pcxg.fitools.entity.mysql.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonMap;

@Service
public class DataCollector {
    private static final Logger logger = LogManager.getLogger(DataCollector.class);
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Qualifier("restClient")
    @Autowired
    RestHighLevelClient client;

    @Value("${skywalking.graphql.url}")
    private String graphqlURL;

    @Value("${store.es.index}")
    private String storeIndex;

    @Value("${store.es.hostname}")
    private String hostName;

    private final RestTemplate restTemplate = new RestTemplate();

    public void collect(FaultInjectInfo faultInjectInfo, Map<String, String[]> faultInjectionMap) {
        Date envEndTime = new Date();
        Date envStartTime = faultInjectInfo.getEnvStartTime();
        //Date envEndTime = faultInjectInfo.getEnvEndTime();

        jdbcTemplate.execute("SET SESSION group_concat_max_len = 100000;");

        int startTime = Integer.parseInt(String.valueOf(envStartTime.getTime()/1000));
        int endTime = Integer.parseInt(String.valueOf(envEndTime.getTime()/1000));
        ObjectMapper mapper = new ObjectMapper();

        List<String> items = new ArrayList<>();
//        queryItemData(startTime, endTime).forEach(
//                e -> {
//                    try {
//                        items.add(mapper.writeValueAsString(e));
//                    } catch (JsonProcessingException jsonProcessingException) {
//                        jsonProcessingException.printStackTrace();
//                    }
//                }
//        );
        logger.info("Finish collecting item data");

        List<String> events = new ArrayList<>();
        queryEventData(startTime, endTime).forEach(
                e -> {
                    try {
                        events.add(mapper.writeValueAsString(e));
                    } catch (JsonProcessingException jsonProcessingException) {
                        jsonProcessingException.printStackTrace();
                    }
                }
        );
        logger.info("Finish collecting event data");

        String envType = faultInjectInfo.getEnvType().toUpperCase();
        Map<String,List<String>> logsMap = null;
        switch (envType) {
            case "HADOOP DOCKER":
                logsMap = queryESData(envStartTime, envEndTime);
                break;
            case "SOCKSHOP DOCKER":
                logsMap = queryESDataForSock(envStartTime, envEndTime, faultInjectionMap);
                break;
            default:
                break;
        }
        logger.info("Finish collecting log data");

        List<String> traces = queryTraceData(envStartTime, envEndTime);
        logger.info("Finish collecting trace data");

        String faultInjectionInfo = null;
        try {
            faultInjectionInfo = mapper.writeValueAsString(faultInjectInfo);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("faultInjectionInfo", faultInjectionInfo);
        dataMap.put("actualEndTime", envEndTime);
        dataMap.put("collectTime", new Date());
        dataMap.put("faultInjectionRet", faultInjectionMap);
        dataMap.put("items", items);
        //dataMap.put("traces", new ArrayList<>());
        Map<String, Object> updateMap = new HashMap<>();
        dataMap.put("events", events);
        dataMap.put("logs", logsMap);
        dataMap.put("traceIds",traces);
        String collectIndex = hostName+"-"+storeIndex;
        IndexRequest indexRequest = new IndexRequest(collectIndex).source(dataMap);
        try {
            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
            if (response.getResult().equals(DocWriteResponse.Result.CREATED)) {
                logger.info("finish creating data");
//                String id = response.getId();
//                for (Map.Entry<String, Object> entry: updateMap.entrySet()) {
//                    logger.info("start updating {} data", entry.getKey());
//                    UpdateRequest updateRequest = new UpdateRequest(collectIndex, id);
//                    updateRequest.doc(entry.getKey(), entry.getValue());
//                    UpdateResponse updateResponse = client.update(updateRequest, RequestOptions.DEFAULT);
//                    if (updateResponse.getResult().equals(DocWriteResponse.Result.UPDATED)) {
//                        logger.info("finish updating {} data", entry.getKey());
//                    }
//                }

//                int chunkSize = 5;
//
//                for (int i=0;i<traces.size();i+=chunkSize) {
//                    int end = Math.min(traces.size(), i+chunkSize);
//                    List<String> part = traces.subList(i, end);
//                    UpdateRequest updateTraceRequest = new UpdateRequest(storeIndex,id);
//                    Map<String, Object> parameters = singletonMap("part", part);
//
//                    Script inline = new Script(ScriptType.INLINE, "painless",
//                            "ctx._source.traces.addAll(params.part)", parameters);
//                    updateTraceRequest.script(inline);
//                    UpdateResponse updateResponse = client.update(updateTraceRequest, RequestOptions.DEFAULT);
//                    if (updateResponse.getResult().equals(DocWriteResponse.Result.UPDATED)) {
//                        logger.info("finish updating trace from {} to {}",i,end);
//                    }
//                }
//                int bulkSize = 20;
//                for (int i=0;i<traces.size();i+=chunkSize * bulkSize) {
//                    BulkRequest bulkRequest = new BulkRequest();
//                    int end = Math.min(traces.size(), i+chunkSize*bulkSize);
//                    logger.info("bulk from {} to {}", i, end);
//                    for (int j=i;j<end;j+=chunkSize) {
//                        int chunkEnd = Math.min(end, j+chunkSize);
//                        List<String> part = traces.subList(j, chunkEnd);
//                        UpdateRequest updateTraceRequest = new UpdateRequest(storeIndex,id);
//                        Map<String, Object> parameters = singletonMap("part", part);
//
//                        Script inline = new Script(ScriptType.INLINE, "painless",
//                                "ctx._source.traces.addAll(params.part)", parameters);
//                        updateTraceRequest.script(inline);
//                        bulkRequest.add(updateTraceRequest);
//                    }
//                    BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
//                    for (BulkItemResponse bulkItemResponse : bulkResponse) {
//                        if (bulkItemResponse.isFailed()) {
//                            BulkItemResponse.Failure failure =
//                                    bulkItemResponse.getFailure();
//                            logger.info("error while updating trace: {}", failure.getMessage());
//                        }
//                    }
//                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        logger.info("finish collecting data");
    }

    //直接向skywalking ui接口发送请求
    //请求时间段内所有trace的概要信息
    /**
     * {"query":"query queryTraces($condition: TraceQueryCondition) {\n  traces: queryBasicTraces(condition: $condition) {\n    data: traces {\n      key: segmentId\n      endpointNames\n      duration\n      start\n      isError\n      traceIds\n    }\n    total\n  }}","variables":{"condition":{"queryDuration":{"start":"2021-02-13 1224","end":"2021-02-13 1239","step":"MINUTE"},"traceState":"ALL","paging":{"pageNum":1,"pageSize":15,"needTotal":true},"queryOrder":"BY_DURATION"}}}
     */
    //请求每个trace的详细信息

    /**
     * {"query":"query queryTrace($traceId: ID!) {\n  trace: queryTrace(traceId: $traceId) {\n    spans {\n      traceId\n      segmentId\n      spanId\n      parentSpanId\n      refs {\n        traceId\n        parentSegmentId\n        parentSpanId\n        type\n      }\n      serviceCode\n      startTime\n      endTime\n      endpointName\n      type\n      peer\n      component\n      isError\n      layer\n      tags {\n        key\n        value\n      }\n      logs {\n        time\n        data {\n          key\n          value\n        }\n      }\n    }\n  }\n  }","variables":{"traceId":"5.47.16132199152150001"}}
     */
    private List<String> queryTraceData(Date startTime, Date endTime) {
        //查询间隔,由于skywalking的前端接口一次最多返回10000条记录，所以拆成多个小窗口查询
        long queryInterval = TimeUnit.SECONDS.toMillis(120);
        int briefPageSize = 100;
        long start = startTime.getTime();
        long end = endTime.getTime();
        List<String> allTraces = new ArrayList<>();
        while(start < end) {
            long tmpEnd = Math.min(start + queryInterval, end);
            allTraces.addAll(queryPartlyTraceData(new Date(start), new Date(tmpEnd), briefPageSize));
            start = tmpEnd;
        }
        return allTraces;
    }

    private List<String> queryPartlyTraceData(Date startTime, Date endTime, int briefPageSize) {
        List<String> allTraces = new ArrayList<>();
        List<String> traceIds = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String start = sdf.format(startTime);
        String end = sdf.format(endTime);
        String queryAllService = "{\"query\":\"query queryServices($duration: Duration!) {\\n    " +
                "services: getAllServices(duration: $duration) {\\n      " +
                "id\\n      " +
                "name\\n    }\\n  }\",\"variables\":{\"" +
                "duration\":{\"start\":\""+start+"\"," +
                "\"end\":\""+end+"\",\"step\":\"SECOND\"}}}";
        String getServiceInstance = "{\n" +
                "\t\"query\": \"query Service($duration: Duration!, $serviceId: ID!) {\\n  instances: getServiceInstances(duration: $duration, serviceId: $serviceId) { \\n id\\n name\\n instanceUUID\\n}}\",\n" +
                "\t\"variables\": {\n" +
                "\t\t\"duration\": {\n" +
                "\t\t\t\"start\": \""+start+"\",\n" +
                "\t\t\t\"end\": \""+end+"\",\n" +
                "\t\t\t\"step\": \"SECOND\"\n" +
                "\t\t},\n" +
                "        \"serviceId\":\"%s\"\n" +
                "\t}\n" +
                "}";
        String queryTraceBrief = "{\"query\":\"query queryTraces($condition: TraceQueryCondition) {\\n  traces: queryBasicTraces(condition: $condition) {\\n    data: traces {\\n      key: segmentId\\n      endpointNames\\n      duration\\n      start\\n      isError\\n      traceIds\\n    }\\n    total\\n  }}\"," +
                "\"variables\":{\"condition\":{" +
                "\"serviceInstanceId\":\"%s\"," +
                "\"queryDuration\":{\"start\":\"" +
                start +
                "\",\"end\":\"" +
                end +
                "\",\"step\":\"SECOND\"}," +
                "\"traceState\":\"ALL\",\"paging\":" +
                "{\"pageNum\":%d," +
                "\"pageSize\":" + briefPageSize +
                ",\"needTotal\":true},\"queryOrder\":\"BY_START_TIME\"}}}";
        String querySingleTrace = "{\"query\":\"query queryTrace($traceId: ID!) {\\n  trace: queryTrace(traceId: $traceId) {\\n    spans {\\n      " +
                "traceId\\n      segmentId\\n      spanId\\n      parentSpanId\\n      refs {\\n        traceId\\n        parentSegmentId\\n        parentSpanId\\n        type\\n      }\\n      serviceCode\\n      startTime\\n      endTime\\n      endpointName\\n      type\\n      peer\\n      component\\n      isError\\n      layer\\n      tags {\\n        key\\n        value\\n      }\\n      logs {\\n        time\\n        data {\\n          key\\n          value\\n        }\\n      }\\n    }\\n  }\\n  }\"," +
                "\"variables\":{\"traceId\":\"%s\"}}";

        try {
            ObjectMapper mapper = new ObjectMapper();
            //查询所有服务
            String allService = postForResp(queryAllService);

            if (null != allService) {
                JsonNode servicesNode = mapper.readTree(allService);
                ArrayNode serviceArr = (ArrayNode) servicesNode.get("data").get("services");
                for (JsonNode service: serviceArr) {
                    String serviceId = service.get("id").textValue();
                    String serviceName = service.get("name").textValue();
                    //这里假定每个不同主机上的agent都会以主机名-服务名的形式命名服务
                    if (serviceName.startsWith(hostName)) {
                        //查询服务实例id
                        String serviceInstanceInfo = postForResp(String.format(getServiceInstance,serviceId));
                        if (null != serviceInstanceInfo) {
                            ArrayNode serviceInstanceArr = (ArrayNode)mapper.
                                    readTree(serviceInstanceInfo).get("data").get("instances");
                            for (JsonNode instance: serviceInstanceArr) {
                                String instanceId = instance.get("id").textValue();
                                //获取所有traceid
                                String traceBrief = postForResp(String.format(queryTraceBrief,instanceId,1));


                                JsonNode briefNode = mapper.readTree(traceBrief);
                                int total = briefNode.get("data").get("traces").get("total").asInt();
                                ArrayNode briefArr = (ArrayNode) briefNode.get("data").get("traces").get("data");
                                //logger.info("querying traces between {} and {}, total: {}",start, end, total);
                                for (JsonNode node: briefArr) {
                                    ArrayNode idArr = (ArrayNode) node.get("traceIds");
                                    idArr.forEach(e -> {
                                        if (e!=null) {
                                            traceIds.add(e.textValue());
                                        }
                                    });
                                }

                                for (int i=2;i<=(total/briefPageSize)+1;i++) {
                                    traceBrief = postForResp(String.format(queryTraceBrief, instanceId, i));
                                    //logger.info("Querying page {}", i);
                                    //logger.info(traceBrief);
                                    briefNode = mapper.readTree(traceBrief);
                                    if (briefNode!=null && briefNode.get("data")!=null && briefNode.get("data").get("traces")!=null) {
                                        briefArr = (ArrayNode) briefNode.get("data").get("traces").get("data");
                                        briefArr.forEach(node -> {
                                            ArrayNode idArr = (ArrayNode) node.get("traceIds");
                                            idArr.forEach(e -> {
                                                if (e!=null) {
                                                    traceIds.add(e.textValue());
                                                }
                                            });
                                        });
                                    }else {
                                        logger.info("Found null value {}",briefNode.textValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }
      //根据traceid查询调用链数据
//            for (String id: traceIds) {
//                String trace = postForResp(String.format(querySingleTrace, id));
//                allTraces.add(trace);
//            }

        } catch (JsonProcessingException | NullPointerException e) {
            e.printStackTrace();
        }

        return traceIds;
    }

    private String postForResp(String requestJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(requestJson,headers);
        ResponseEntity<String> response = restTemplate.postForEntity(graphqlURL, entity, String.class);
        if (response.getStatusCodeValue() != 200) {
            logger.error("Get trace data failed:{}", response.getBody());
            return null;
        }
        return response.getBody();
    }
    /**
     * 查询hadoop相关日志
     * @param startTime
     * @param endTime
     * @return
     */
    private Map<String, List<String>> queryESData(Date startTime, Date endTime) {
        Map<String, List<String>> map = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String start = sdf.format(startTime.getTime());
        String end = sdf.format(endTime.getTime());
        String[] indexes = {
                "syslog","stderr",
                "hadoop-root-secondarynamenode-hadoop-master.log",
                "hadoop-root-resourcemanager-hadoop-master.log",
                "hadoop-root-namenode-hadoop-master.log",
                "hadoop-root-nodemanager-hadoop-slave1.log",
                "hadoop-root-datanode-hadoop-slave1.log",
                "hadoop-root-nodemanager-hadoop-slave2.log",
                "hadoop-root-datanode-hadoop-slave2.log"
        };
        for (String index: indexes) {
            List<String> res = new ArrayList<>();

            final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(5L));
            SearchRequest searchRequest = new SearchRequest(hostName+"-"+index + "*");
            searchRequest.scroll(scroll);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            QueryBuilder queryBuilder;
            String[] includes;
            if (index.contains("stderr")) {
                includes = new String[]{"message", "host.name", "log.file.path", "@timestamp"};
                queryBuilder = QueryBuilders.rangeQuery("@timestamp").gte(start).lte(end);
                searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.ASC));
            }else {
                includes = new String[]{"log_time", "message", "level", "log_message", "host.name", "log.file.path"};
                queryBuilder = QueryBuilders.rangeQuery("log_time").gte(start).lte(end);
                searchSourceBuilder.sort(new FieldSortBuilder("log_time").order(SortOrder.ASC));
            }
            searchSourceBuilder.query(queryBuilder);
            searchSourceBuilder.fetchSource(includes, null);
            searchRequest.source(searchSourceBuilder);

            try {
                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                String scrollId = searchResponse.getScrollId();
                SearchHit[] searchHits = searchResponse.getHits().getHits();

                while (searchHits != null && searchHits.length > 0) {
                    //处理返回的结果
                    Arrays.stream(searchHits).forEach(hit -> res.add(hit.getSourceAsString()));

                    SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                    scrollRequest.scroll(scroll);
                    searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
                    scrollId = searchResponse.getScrollId();
                    searchHits = searchResponse.getHits().getHits();
                }

                ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                clearScrollRequest.addScrollId(scrollId);
                ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
                boolean succeeded = clearScrollResponse.isSucceeded();
                if (!succeeded) {
                    logger.error("Clean scroll failed");
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
            map.put(index, res);
        }

        return map;
    }
    /**
     * 查询sockshop所有服务的日志
     * 默认其服务日志索引格式为 主机名-日志名-日期 。
     * 日志名格式为 容器ID-json.log
     */
    private Map<String, List<String>> queryESDataForSock(Date startTime, Date endTime, Map<String, String[]> faultInjectionMap) {
        if (!faultInjectionMap.containsKey("docker ps") || !"0".equals(faultInjectionMap.get("docker ps")[3])) {
            return null;
        }
        String[] idNameArr = faultInjectionMap.get("docker ps")[1].split("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        String start = sdf.format(startTime.getTime());
        String end = sdf.format(endTime.getTime());
        Map<String, List<String>> map = new HashMap<>();

        for(String idName: idNameArr) {
            if (idName.contains(":")){
                String containerId = idName.split(":")[0];
                String containerName = idName.split(":")[1];
                List<String> res = new ArrayList<>();

                final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(5L));
                SearchRequest searchRequest = new SearchRequest(hostName+"-"+ containerId + "*");
                searchRequest.scroll(scroll);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                QueryBuilder queryBuilder;
                String[] includes = {"message"};
                queryBuilder = QueryBuilders.rangeQuery("@timestamp").gte(start).lte(end);
                searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.ASC));
                searchSourceBuilder.query(queryBuilder);
                searchSourceBuilder.fetchSource(includes, null);
                searchRequest.source(searchSourceBuilder);

                try {
                    SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                    String scrollId = searchResponse.getScrollId();
                    SearchHit[] searchHits = searchResponse.getHits().getHits();

                    while (searchHits != null && searchHits.length > 0) {
                        //处理返回的结果
                        Arrays.stream(searchHits).forEach(hit -> res.add(hit.getSourceAsString()));

                        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
                        scrollRequest.scroll(scroll);
                        searchResponse = client.scroll(scrollRequest, RequestOptions.DEFAULT);
                        scrollId = searchResponse.getScrollId();
                        searchHits = searchResponse.getHits().getHits();
                    }

                    ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
                    clearScrollRequest.addScrollId(scrollId);
                    ClearScrollResponse clearScrollResponse = client.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
                    boolean succeeded = clearScrollResponse.isSucceeded();
                    if (!succeeded) {
                        logger.error("Clean scroll failed");
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
                map.put(containerName, res);
            }
        }

        return map;
    }
    /**
     * 查询指标数
     * @return 指标list
     */
    private List<Item> queryItemData(int startTime, int endTime) {
        List<Item> items = new ArrayList<>();
        String[] historyTableNames = {"history", "history_log", "history_str", "history_text", "history_uint"};
        String itemSQL = "SELECT name, hostid, delay, type, value_type, units FROM items WHERE itemid = ?";
        String hostSQL = "SELECT host, status FROM hosts WHERE hostid = ?";
        String appSQL = "SELECT a.name FROM applications a INNER JOIN items_applications ia ON a.applicationid = ia.applicationid WHERE ia.itemid = ?";
        String tmpSQL = "SELECT itemid, group_concat(clock) as all_clock, group_concat(value) as all_value from history GROUP BY itemid";
        for (String table: historyTableNames) {
            String historySQL = "SELECT itemid, group_concat(clock) as all_clock, group_concat(value) as all_value from " +table+ " WHERE clock BETWEEN ? AND ? GROUP BY itemid";
            logger.info("start querying items from {} at {}", table, new Date());
            jdbcTemplate.query(historySQL,new Object[]{startTime, endTime},new int[]{Types.INTEGER, Types.INTEGER} ,(resultSet -> {
                Item item = new Item();
                int itemID = resultSet.getInt("itemid");
                String allClock = resultSet.getString("all_clock");
                String allValue = resultSet.getString("all_value");
                item.setId(itemID);
                item.setAllClock(allClock);
                item.setAllValue(allValue);
                jdbcTemplate.query(itemSQL, new Object[]{itemID}, new int[]{Types.BIGINT},itemRS -> {
                    item.setName(itemRS.getString("name"));
                    item.setDelay(itemRS.getString("delay"));
                    item.setType(itemRS.getInt("type"));
                    item.setValueType(itemRS.getInt("value_type"));
                    item.setUnits(itemRS.getString("units"));
                    int hostId = itemRS.getInt("hostid");
                    jdbcTemplate.query(hostSQL, new Object[]{ hostId }, new int[]{Types.BIGINT}, hostRS -> {
                        AtomicReference<String> hostName = new AtomicReference<>(hostRS.getString("host"));
                        int status = hostRS.getInt("status");
                        if (status==3) {
                            String tempSQL = "SELECT h.host FROM hosts h INNER JOIN hosts_templates ht ON ht.hostid = h.hostid WHERE ht.templateid = ?";
                            jdbcTemplate.query(tempSQL, new Object[]{ hostId }, new int[]{Types.BIGINT}, tempRS -> {
                                hostName.set(tempRS.getString(1));
                            });
                        }
                        item.setHostName(hostName.get());
                    });
                });
                jdbcTemplate.query(appSQL, new Object[]{itemID}, new int[]{Types.BIGINT}, appRS -> {
                    item.setApplicationName(appRS.getString(1));
                });
                items.add(item);
            }));
            logger.info("finish querying items from {} at {}", table, new Date());
        }

        return items;
    }

    /**
     * 查询时间段内所有的事件数据
     * @param startTime
     * @param endTime
     * @return
     */
    private List<Event> queryEventData(int startTime, int endTime) {
        String sql = "SELECT eventid, clock, value, name, severity FROM events WHERE clock BETWEEN ? AND ?";
        return new ArrayList<>(jdbcTemplate.query(sql,new Object[]{startTime, endTime},new int[]{Types.INTEGER, Types.INTEGER},
                (rs, rowNum) -> new Event(
                        rs.getInt("eventid"),
                        rs.getInt("clock"),
                        rs.getInt("value"),
                        rs.getString("name"),
                        rs.getInt("severity")
                )));
        //        SqlRowSet result = jdbcTemplate.queryForRowSet(sql);
//        while(result.next()) {
//            EventPo eventPo = new EventPo();
//            eventPo.setEventid(result.getInt("eventid"));
//            eventPo.setClock(result.getInt("clock"));
//            eventPo.setValue(result.getInt("value"));
//            eventPo.setName(result.getString("name"));
//            eventPo.setSeverity(result.getInt("clock"));
//        }
        //        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
//        for (Map<String, Object> map: list) {
//            for(Map.Entry<String, Object> entry: map.entrySet()) {
//                System.out.println(entry.getKey()+":"+entry.getValue());
//            }
//        }
    }
}
