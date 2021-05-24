package com.pcxg.fitools.tools.ssfi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.pcxg.fitools.entity.FaultConf;
import com.pcxg.fitools.tools.FaultInjectTool;
import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class SSFITool extends FaultInjectTool {
    private static final Logger logger = LogManager.getLogger(SSFITool.class);
    private static final String toolPath = "/tmp/work/fitools/ssfi/";
    private static final String inputPath = toolPath + "input/";
    private static final String outputPath = toolPath + "output/";
    private static final String packPath = toolPath + "pack/";
    private static final String jarPath = toolPath + "hadoop-jars/";
    private static final String depPath = toolPath + "hadoop-all-dep";
    private static final String toolName = "ssfi-1.0-SNAPSHOT-jar-with-dependencies.jar";
    private static final String configName = "ssfiConfig.yaml";
    public static final String activationLogPath = outputPath + "logs/activation.log";
    public static final String injectionLogPath = "/tmp/injection.log";
    /**
     * 验证故障配置，暂时只考虑类型是否为CODE，之后完善
     * @param faultConf
     * @return
     */
    protected boolean check(FaultConf faultConf) {
        if ("CODE".equals(faultConf.getFaultType().getLevel())) {
            if ("DOCKER JAR".equals(faultConf.getFaultLocation().getType())
                    && faultConf.getFaultLocation().getLocation().split(" ").length > 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据配置注入故障，打包并替换jar包,收集脚本的输出
     * @param faultConf
     * @return
     */
    protected Map<String, String[]> inject(FaultConf faultConf) {
        Map<String, String[]> injectRet = new LinkedHashMap<>();
        SSHConnection ssh = this.getSsh();
        String workspace = ssh.getWorkspace() + "/ssfi";
        String[] location = faultConf.getFaultLocation().getLocation().split(" ");
        String containerName = location[0];
        String jarName = location[1];

        try {
            JsonNode jsonNodeTree = new ObjectMapper().readTree(faultConf.getFaultParams());
            ObjectNode objectNode = (ObjectNode) jsonNodeTree;
            objectNode.put("inputPath", inputPath);
            objectNode.put("outputPath", outputPath);
            objectNode.put("dependencyPath", depPath);
            //jar包最终目录，input/component/jarName/
            String jarDestPath = inputPath + jsonNodeTree.get("component").asText() + "/" + jarName + "/";
            ssh.uploadFile(workspace, configName,
                    new ByteArrayInputStream(new YAMLMapper().writeValueAsString(objectNode).getBytes()));
            String jarFullPath = jarPath + jarName;
            //创建文件夹，解压jar包
            String prepareCommand = "docker cp " + workspace + "/" + configName + " "
                    + containerName + ":" + toolPath + " && docker exec " + containerName +
                    " /bin/bash -c 'mkdir -p " + String.join(" ",jarDestPath,
                    outputPath,packPath) +
                    " && cd " + jarDestPath +
                    " && cp " + jarFullPath + " ./" +
                    " && jar xf " + jarName +
                    " && rm -f " + jarName +
                    "'";
            String newJarPath = toolPath + jarName;

            String runSSFICommand = "docker exec " + containerName + " /bin/bash -c '" +
                    " mkdir " + outputPath + "logs" +
                    " && java -cp " + toolPath + toolName + " com.alex.ssfi.Application " +
                    toolPath + configName +
                    " && cp -r " + jarDestPath + "* " + packPath +
                    " && cp -r " + outputPath + "* " + packPath +
                    " && cd " + packPath +
                    " && jar -cf " + newJarPath + " ./*" +
                    " && rep=$(find /usr/local/hadoop/ -name " + jarName + " | awk 'NR==1{print}')" +
                    " && mv ${rep} ${rep}.bak" +
                    " && cp " + newJarPath + " ${rep}" +
                    "'";
            injectRet.put("SSFI.prepare", ssh.runCommand(prepareCommand));
            injectRet.put("SSFI.run", ssh.runCommand(runSSFICommand));
        }catch (Exception e) {
            logger.error("Fail to inject fault");
        }

        return injectRet;
    }

    private String json2yaml(String jsonString) throws JsonProcessingException {
        // parse JSON
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonString);
        // save it as YAML
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        return jsonAsYaml;
    }
}
