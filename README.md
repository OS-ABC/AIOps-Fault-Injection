# AIOps-Fault-Injection



### 工具介绍

本工具基于SpringBoot框架开发，以API服务形式接收用户发送的请求，实现如下功能：

- 以SSH方式登录远程主机，并按用户发送的配置完成启动/关闭Hadoop集群、注入指定类型故障故障、运行WordCount工作负载等定时任务。
- 在销毁Hadoop集群后查询指标、日志、调用链和事件数据并保存到数据库中。

整体工作流程图如下

![示意图](C:\Users\18305\Desktop\答辩\其他图\示意图.png)





### 如何使用

1. 提前在远程主机上配置好Zabbix、Filebeat和SkyWalking，确保该主机能通过SSH方式连接。
2. 在远程主机上打包好Hadoop镜像，修改resources/env文件内docker-compose.hadoop.yml文件内的对应镜像名称。该镜像应包括如下内容：
   - 本工具支持的故障注入工具（ChaosBlade/SSFI）
   - SkyWalking agent包
   - 添加了SkyWalking打点代码后重新编译的Hadoop安装包
3. 修改resources文件夹下的配置文件(application-sample.properties),依次填上如下配置：
   - 服务开放端口
   - 远程主机的主机名、用户名、密码、上传文件的工作目录等
   - Zabbix使用的MySQL数据库地址
   - SkyWalking的GraphQL地址
   - Filebeat保存日志的Elasticsearch地址
4. 直接在IDE中运行或maven打包后运行工具代码
5. 向localhost:<配置端口>/test发送POST请求，请求数据的示例和主要字段含义如下所示。

![sample-json](C:\Users\18305\Desktop\sample-json.png)

| **关键字段名**             | **字段含义**                               | 支持类型             |
| -------------------------- | ------------------------------------------ | :------------------- |
| **toolType**               | 故障注入管理模块使用的开源故障注入工具类型 | ChaosBlade/SSFI      |
| **envType**                | 故障注入管理模块启动的系统类型             | Hadoop Docker        |
| **envStartTime**           | 系统启动时间                               | 时间戳               |
| **envEndTime**             | 系统结束时间                               | 时间戳               |
| **jobStartTime**           | 开始执行工作负载时间                       | 时间戳               |
| **faultStartTime**         | 故障注入开始时间                           | 时间戳               |
| **faultEndTime**           | 故障注入结束时间                           | 时间戳               |
| **jobName**                | 工作负载名称                               | WordCount            |
| **faultConf.faultParams**  | 故障注入的具体参数                         | json字符串           |
| **faultLocation.type**     | 故障注入位置的类型                         | DOCKER               |
| **faultLocation.location** | 故障注入的具体位置                         | docker容器名称       |
| **faultType.level**        | 故障注入类型的级别                         | CPU/MEM/CODE等       |
| **faultType.type**         | 故障注入的具体类型                         | CPU LOAD/DISK FILL等 |