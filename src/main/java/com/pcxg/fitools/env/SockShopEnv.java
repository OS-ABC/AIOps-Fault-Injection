package com.pcxg.fitools.env;

import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class SockShopEnv extends Environment{
    private static final Logger logger = LogManager.getLogger(SockShopEnv.class);
    private static final String configDir = "env";
    private static final String configName = "docker-compose.sock.yml";
    private static final String resourcePath = configDir + "/" + configName;


    @Override
    public Map<String, String[]> start() {
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
    public Map<String, String[]> startJob(String jobName) {
        Map<String, String[]> startJobRet = new LinkedHashMap<>();
        SSHConnection ssh = this.getSsh();
        //获取所有容器id及对应名称
        String dockerPSCommand = "docker ps --format \"{{.ID}}:{{.Names}}\" --no-trunc";
        startJobRet.put("docker ps", ssh.runCommand(dockerPSCommand));

        if ("LOAD-TEST".equals(jobName.toUpperCase())) {
            String loadTestCommand = "docker run --net=host --name load-test weaveworksdemos/load-test" +
                    " -h localhost:20080 -r 500 -c 5";

            String removeContainerCommand = "docker container prune -f";

            startJobRet.put("load test", ssh.runCommand(loadTestCommand));
            startJobRet.put("rm load test", ssh.runCommand(removeContainerCommand));
        }
        return startJobRet;
    }

    @Override
    public Map<String, String[]> end() {
        Map<String, String[]> endEnvRet = new LinkedHashMap<>();
        SSHConnection ssh = this.getSsh();
        String endCommand = "docker-compose -f " + this.getSsh().getWorkspace() + "/" + resourcePath + " down";
        String removeVolumesCommand = "docker volume prune -f";

        endEnvRet.put("envEnd", ssh.runCommand(endCommand));
        endEnvRet.put("remove volumes", ssh.runCommand(removeVolumesCommand));
        return endEnvRet;
    }
}
