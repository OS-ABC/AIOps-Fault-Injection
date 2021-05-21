package com.pcxg.fitools.tools.chaosblade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcxg.fitools.entity.FaultConf;
import com.pcxg.fitools.tools.FaultInjectTool;
import com.pcxg.fitools.tools.ssfi.SSFITool;
import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.util.set.Sets;

import java.util.*;

public class ChaosBladeTool extends FaultInjectTool {
    private static final Logger logger = LogManager.getLogger(ChaosBladeTool.class);
    private static final String toolPath = "/tmp/work/fitools/chaosblade/blade";
    private static final Set<String> allowFaultTypes = Sets.newHashSet(
            "CPU", "DISK", "MEMORY", "NETWORK","PROCESS"
    );
    private static final Set<String> allowFaultLocationType = Sets.newHashSet(
            "DOCKER"
    );
    @Override
    protected boolean check(FaultConf faultConf) {
        String level = faultConf.getFaultType().getLevel().toUpperCase();
        String locationType = faultConf.getFaultLocation().getType();
        if (allowFaultTypes.contains(level) && allowFaultLocationType.contains(locationType)) {
            return true;
        }
        return false;
    }

    @Override
    protected Map<String, String[]> inject(FaultConf faultConf) {
        Map<String, String[]> injectRet = new LinkedHashMap<>();
        SSHConnection ssh = this.getSsh();
        String containerName = faultConf.getFaultLocation().getLocation();
        try {
            StringBuilder sb = new StringBuilder(toolPath).append(" c ");

            String specificFault = faultConf.getFaultType().getType().toLowerCase();
            sb.append(specificFault).append(" ");

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(faultConf.getFaultParams(), Map.class);
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                sb.append("--").append(entry.getKey()).append(" ")
                        .append(entry.getValue()).append(" ");
            }
            String injectCommand = "docker exec " + containerName + " /bin/sh -c '" +
                    sb.toString() +
                    "'";
            injectRet.put("ChaosBlade.run", ssh.runCommand(injectCommand));
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("Fail to inject fault");
        }
        return injectRet;
    }
}
