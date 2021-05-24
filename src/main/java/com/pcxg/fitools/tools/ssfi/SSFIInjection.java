package com.pcxg.fitools.tools.ssfi;

import com.pcxg.fitools.entity.FaultInjectInfo;
import com.pcxg.fitools.env.Environment;
import com.pcxg.fitools.entity.FaultConf;
import com.pcxg.fitools.service.FaultInjectServiceImpl;
import com.pcxg.fitools.tools.FaultInjectTool;
import com.pcxg.fitools.tools.FaultInjection;
import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;


public class SSFIInjection extends FaultInjection {
    private static final Logger logger = LogManager.getLogger(SSFIInjection.class);

    private CountDownLatch countDownLatch;

    public SSFIInjection(FaultInjectInfo faultInjectInfo, SSHConnection ssh, CountDownLatch countDownLatch) {
        super(faultInjectInfo, ssh);
        this.countDownLatch = countDownLatch;
    }

    @Override
    public boolean check(FaultInjectInfo faultInjectInfo) {
        if (!checkDate()) {
            return false;
        }
        if (!"SSFI".equals(faultInjectInfo.getToolType().toUpperCase())) {
            return false;
        }
        return true;
    }

    @Override
    public void inject() throws Exception {
        this.setInjectionMap(new LinkedHashMap<>());
        Map<String, String[]> injectMap = this.getInjectionMap();
        Environment environment = this.getFaultInjectInfo().getEnvironment();
        SSHConnection ssh = this.getSsh();
        environment.setSsh(ssh);
        FaultInjectTool ssfi = new SSFITool();
        ssfi.setSsh(ssh);
        Timer timer = new Timer();
        Timer envTimer = new Timer();
        Timer jobTimer = new Timer();
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (!check(getFaultInjectInfo())) {
            throw new Exception("Invalid configuration");
        }

        envTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ssh.connect();
                logger.info("envStart start at {} ",sdf.format(new Date()));
                addMapToMap(environment.start(), injectMap);
                logger.info("envStart end at {} ",sdf.format(new Date()));
            }
        }, getFaultInjectInfo().getEnvStartTime());

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.info("faultInjection start at {} ",sdf.format(new Date()));
                    addMapToMap(ssfi.run(getFaultInjectInfo().getFaultConf()), injectMap);
                    logger.info("faultInjection end at {} ",sdf.format(new Date()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, getFaultInjectInfo().getFaultStartTime());

        jobTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("job start at {} ",sdf.format(new Date()));
                addMapToMap(environment.startJob(getFaultInjectInfo().getJobName()), injectMap);
                logger.info("job end at {} ",sdf.format(new Date()));
            }
        }, getFaultInjectInfo().getJobStartTime());

        envTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("envEnd start at {} ",sdf.format(new Date()));
                addMapToMap(catLogs(), injectMap);
                addMapToMap(environment.end(), injectMap);
                logger.info("envEnd end at {} ",sdf.format(new Date()));
                ssh.close();
                countDownLatch.countDown();
            }
        }, getFaultInjectInfo().getEnvEndTime());

    }

    private void addMapToMap(Map<String, String[]> from, Map<String, String[]> to) {
        for (Map.Entry<String, String[]> entry: from.entrySet()) {
            to.put(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String[]> catLogs() {
        Map<String, String[]> ret = new LinkedHashMap<>();
        String location = getFaultInjectInfo().getFaultConf().
                getFaultLocation().getLocation().split(" ")[0];
        String type = getFaultInjectInfo().getFaultConf().getFaultLocation().getType().split(" ")[0];
        if ("DOCKER".equals(type.toUpperCase())) {
            String catActCommand = "docker exec " + location + " /bin/bash -c '" +
                    "cat " + SSFITool.activationLogPath +
                    "'";
            String catInjCommand = "docker exec " + location + " /bin/bash -c '" +
                    "cat " + SSFITool.injectionLogPath +
                    "'";
            ret.put("activation", getSsh().runCommand(catActCommand));
            ret.put("injection", getSsh().runCommand(catInjCommand));
        }
        return ret;
    }

}
