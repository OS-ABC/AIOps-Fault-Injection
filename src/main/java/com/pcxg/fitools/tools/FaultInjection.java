package com.pcxg.fitools.tools;

import com.pcxg.fitools.entity.FaultConf;
import com.pcxg.fitools.entity.FaultInjectInfo;
import com.pcxg.fitools.env.Environment;
import com.pcxg.fitools.utils.SSHConnection;

import java.util.Date;
import java.util.Map;

public abstract class FaultInjection {
    private FaultInjectInfo faultInjectInfo;
    private SSHConnection ssh;
    private Map<String, String[]> injectionMap;

    public abstract boolean check(FaultInjectInfo faultInjectInfo);
    public abstract void inject() throws Exception;

    public FaultInjection(FaultInjectInfo faultInjectInfo, SSHConnection ssh) {
        this.faultInjectInfo = faultInjectInfo;
        this.ssh = ssh;
    }

    public boolean checkDate() {
        Date envStart = faultInjectInfo.getEnvStartTime();
        Date envEnd = faultInjectInfo.getEnvEndTime();
        Date jobStart = faultInjectInfo.getJobStartTime();
        Date faultStart = faultInjectInfo.getFaultStartTime();
        Date faultEnd = faultInjectInfo.getFaultEndTime();
        //faultEnd可以为null
        if (envStart == null || envEnd == null || jobStart == null || faultStart == null) {
            return false;
        }
        if (faultEnd != null && (envStart.after(faultEnd) || envEnd.before(faultEnd)
                || faultStart.after(faultEnd))) {
            return false;
        }
        if (envStart.after(envEnd) || envStart.after(jobStart) || envStart.after(faultStart)) {
            return false;
        }
        if (envEnd.before(jobStart) || envEnd.before(faultStart)) {
            return false;
        }

        return true;
    }

    public FaultInjectInfo getFaultInjectInfo() {
        return faultInjectInfo;
    }

    public void setFaultInjectInfo(FaultInjectInfo faultInjectInfo) {
        this.faultInjectInfo = faultInjectInfo;
    }

    public SSHConnection getSsh() {
        return ssh;
    }

    public void setSsh(SSHConnection ssh) {
        this.ssh = ssh;
    }

    public void setInjectionMap(Map<String, String[]> injectionMap) {
        this.injectionMap = injectionMap;
    }

    public Map<String, String[]> getInjectionMap() {
        return injectionMap;
    }
}