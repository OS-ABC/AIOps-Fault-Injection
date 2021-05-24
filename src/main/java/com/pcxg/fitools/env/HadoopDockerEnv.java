package com.pcxg.fitools.env;


import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;


import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HadoopDockerEnv extends Environment{
    private static final Logger logger = LogManager.getLogger(HadoopDockerEnv.class);
    private static final String configDir = "env";
    private static final String configName = "docker-compose.hadoop.yml";
    private static final String resourcePath = configDir + "/" + configName;
    private Map<String, Object> containerInfo;
    private List<String> containerNames;
    @Override
    public Map<String, String[]> start() {
        this.loadConfig();
        Map<String, String[]> startEnvRet = new LinkedHashMap<>();
        String workspace = this.getSsh().getWorkspace() + "/" + configDir;
        //todo: windows本地测试用/，之后全要改成separator
        //String workspace = this.getSsh().getWorkspace() + File.separator + "env";
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(resourcePath);

        SSHConnection ssh = this.getSsh();
        ssh.uploadFile(workspace, configName, input);

        String startCommand = "docker-compose -f " + workspace + "/" + configName + " up -d ";
        startEnvRet.put("startEnv", ssh.runCommand(startCommand));


        return startEnvRet;
    }
    @Override
    /**
     * 启动hadoop集群，开始运行指定的任务
     */
    public Map<String, String[]> startJob(String jobName) {
        Map<String, String[]> startJobRet = new LinkedHashMap<>();

        //find master and slave names
        String master = this.containerNames.get(0);
        SSHConnection ssh = this.getSsh();
        String startHadoopCommand = "docker exec " + this.containerNames.get(0) + " /bin/bash -c '/tmp/work/config/start-cluster.sh'";

        startJobRet.put("startHadoopCluster", ssh.runCommand(startHadoopCommand));
        // 让hadoop集群完成初始化,等待30s
//        try {
//            TimeUnit.SECONDS.sleep(30);
//        }catch (InterruptedException e) {
//            logger.error(e.getMessage());
//        }

        //start hadoop
//        String startHadoopCommand = "docker exec " + master + " /bin/bash -c '/tmp/work/config/start-cluster.sh'";
//        String[] startHadoopRet = ssh.runCommand(startHadoopCommand);
//        startJobRet.put("startHadoop", startHadoopRet);

        //cat jps
        for(String name: this.containerNames) {
            String jpsCommand = "docker exec " + name + " /bin/bash -c 'jps'";
            String[] jpsRet = ssh.runCommand(jpsCommand);
            startJobRet.put(name + ".jps", jpsRet);
        }

        if ("WORDCOUNT".equals(jobName.toUpperCase())) {

            //run wordcount
            //String wordcountCommand = "docker exec " + master + " /bin/bash -c '/tmp/work/config/wc.sh'";
//            String infiWordCountSH = "#!/bin/bash\n" +
//                    "\n" +
//                    "# test the hadoop cluster by running wordcount\n" +
//                    "\n" +
//                    "# create input files\n" +
//                    "mkdir input\n" +
//                    "echo \"Hello Docker\" >input/file2.txt\n" +
//                    "echo \"Hello Hadoop\" >input/file1.txt\n" +
//                    "\n" +
//                    "# create input directory on HDFS\n" +
//                    "hadoop fs -mkdir -p input\n" +
//                    "\n" +
//                    "# put input files to HDFS\n" +
//                    "hdfs dfs -put ./input/* input\n" +
//                    "\n" +
//                    "# run wordcount in loop\n" +
//                    "while true\n" +
//                    "do\n" +
//                    "    hadoop jar $HADOOP_HOME/share/hadoop/mapreduce/sources/hadoop-mapreduce-examples-3.2.1-sources.jar org.apache.hadoop.examples.WordCount input output\n" +
//                    "    hdfs dfs -cat output/part-r-00000\n" +
//                    "    hdfs dfs -rm -r output\n" +
//                    "done";
//            String infiwcCommand = "docker exec " + master + " /bin/bash -c '" +
//                    " echo \"" +
//                    infiWordCountSH +
//                    "\" " + "> infi-wc.sh" +
//                    " && chmod +x ./infi-wc.sh" +
//                    " && ./infi-wc.sh" +
//                    "'";
//            startJobRet.put("infi-wordcount", ssh.runCommand(infiwcCommand));
            String wordcountCommand = "docker exec " + master + " /bin/bash -c '" +
                    " mkdir input" +
                    " && cp /usr/local/hadoop/README.txt input/file2.txt" +
                    " && cp /usr/local/hadoop/NOTICE.txt input/file1.txt" +
//                    " && echo \"Hello Docker\" >input/file2.txt" +
//                    " && echo \"Hello Hadoop\" >input/file1.txt" +
                    " && hadoop fs -mkdir -p input" +
                    " && hdfs dfs -put ./input/* input" +
                    " && hadoop jar $HADOOP_HOME/share/hadoop/mapreduce/sources/hadoop-mapreduce-examples-3.2.1-sources.jar org.apache.hadoop.examples.WordCount input output" +
                    "'";
            String catCommand = "docker exec " + master + " /bin/bash -c '" +
                    " hdfs dfs -cat output/part-r-00000" +
                    "'";
            startJobRet.put("wordcount", ssh.runCommand(wordcountCommand));
            startJobRet.put("wordcountResult", ssh.runCommand(catCommand));
        }

        return startJobRet;
    }
    @Override
    public Map<String, String[]> end() {
        Map<String, String[]> endEnvRet = new LinkedHashMap<>();
        SSHConnection ssh = this.getSsh();
        String endCommand = "docker-compose -f " + this.getSsh().getWorkspace() + "/" + resourcePath + " down";
        endEnvRet.put("envEnd", ssh.runCommand(endCommand));
        return endEnvRet;
    }


    /**
     * 根据自带的docker-compose文件获取集群部署信息
     */
    private void loadConfig() {
        Yaml yaml = new Yaml();
        try {
            InputStream input = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
            Map<String, Object> map = yaml.load(input);
            this.containerInfo = (Map<String, Object>) map.get("services");
            List<String> names = new ArrayList<>();
            for(Map.Entry<String, Object> entry: this.containerInfo.entrySet()) {
                Map<String, Object> tmp = (Map<String, Object>) entry.getValue();
                names.add((String) tmp.get("container_name"));
            }
            this.containerNames = names;
        }catch (Exception e) {
            logger.error("Fail to load config file");
        }
    }

    public Map<String, Object> getContainerInfo() {
        return containerInfo;
    }
}