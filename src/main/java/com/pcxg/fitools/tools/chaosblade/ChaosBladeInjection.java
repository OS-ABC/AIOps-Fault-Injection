package com.pcxg.fitools.tools.chaosblade;

import com.pcxg.fitools.entity.FaultInjectInfo;
import com.pcxg.fitools.env.Environment;
import com.pcxg.fitools.tools.FaultInjectTool;
import com.pcxg.fitools.tools.FaultInjection;
import com.pcxg.fitools.tools.ssfi.SSFIInjection;
import com.pcxg.fitools.tools.ssfi.SSFITool;
import com.pcxg.fitools.utils.SSHConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class ChaosBladeInjection extends FaultInjection {
    private static final Logger logger = LogManager.getLogger(ChaosBladeInjection.class);

    private CountDownLatch countDownLatch;

    public ChaosBladeInjection(FaultInjectInfo faultInjectInfo, SSHConnection ssh, CountDownLatch countDownLatch) {
        super(faultInjectInfo, ssh);
        this.countDownLatch = countDownLatch;
    }

    @Override
    public boolean check(FaultInjectInfo faultInjectInfo) {
        if (!checkDate()) {
            return false;
        }
        if (!"CHAOSBLADE".equals(faultInjectInfo.getToolType().toUpperCase())) {
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
        FaultInjectTool blade = new ChaosBladeTool();
        blade.setSsh(ssh);
        Timer envTimer = new Timer();
        Timer bladeTimer = new Timer();
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

        bladeTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    logger.info("faultInjection start at {} ",sdf.format(new Date()));
                    addMapToMap(blade.run(getFaultInjectInfo().getFaultConf()), injectMap);
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
                addMapToMap(environment.end(), injectMap);
                logger.info("envEnd end at {} ",sdf.format(new Date()));
                ssh.close();
                countDownLatch.countDown();
            }
        }, getFaultInjectInfo().getEnvEndTime());
    }


    private void addMapToMap(Map<String, String[]> from, Map<String, String[]> to) {
        if (from==null) {
            return ;
        }
        for (Map.Entry<String, String[]> entry: from.entrySet()) {
            to.put(entry.getKey(), entry.getValue());
        }
    }
}
