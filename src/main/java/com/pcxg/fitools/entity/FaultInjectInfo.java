package com.pcxg.fitools.entity;

import com.pcxg.fitools.env.Environment;
import lombok.Data;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
@Data
public class FaultInjectInfo {
    @Future(message = "invalid time")
    private Date envStartTime;
    @Future(message = "invalid time")
    private Date envEndTime;
    @Future(message = "invalid time")
    private Date jobStartTime;
    @NotBlank(message = "jobName must not be blank")
    private String jobName;
    @Future(message = "invalid time")
    private Date faultStartTime;
    @Future(message = "invalid time")
    private Date faultEndTime;
    @NotBlank(message = "envType must not be blank")
    private String envType;
    private Environment environment;
    @NotNull(message = "faultConf must not be null")
    private FaultConf faultConf;
    @NotBlank(message = "toolType must not be blank")
    private String toolType;

    public FaultInjectInfo(@Future(message = "invalid time") Date envStartTime, @Future(message = "invalid time") Date envEndTime, @Future(message = "invalid time") Date jobStartTime, @NotBlank(message = "jobName must not be blank") String jobName, @Future(message = "invalid time") Date faultStartTime, @Future(message = "invalid time") Date faultEndTime, @NotBlank(message = "envType must not be blank") String envType, Environment environment, @NotNull(message = "faultConf must not be null") FaultConf faultConf, @NotBlank(message = "toolType must not be blank") String toolType) {
        this.envStartTime = envStartTime;
        this.envEndTime = envEndTime;
        this.jobStartTime = jobStartTime;
        this.jobName = jobName;
        this.faultStartTime = faultStartTime;
        this.faultEndTime = faultEndTime;
        this.envType = envType;
        this.environment = environment;
        this.faultConf = faultConf;
        this.toolType = toolType;
    }

    public String getToolType() {
        return toolType;
    }

    public void setToolType(String toolType) {
        this.toolType = toolType;
    }

    public Date getEnvStartTime() {
        return envStartTime;
    }

    public void setEnvStartTime(Date envStartTime) {
        this.envStartTime = envStartTime;
    }

    public Date getEnvEndTime() {
        return envEndTime;
    }

    public void setEnvEndTime(Date envEndTime) {
        this.envEndTime = envEndTime;
    }

    public Date getJobStartTime() {
        return jobStartTime;
    }

    public void setJobStartTime(Date jobStartTime) {
        this.jobStartTime = jobStartTime;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Date getFaultStartTime() {
        return faultStartTime;
    }

    public void setFaultStartTime(Date faultStartTime) {
        this.faultStartTime = faultStartTime;
    }

    public Date getFaultEndTime() {
        return faultEndTime;
    }

    public void setFaultEndTime(Date faultEndTime) {
        this.faultEndTime = faultEndTime;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public FaultConf getFaultConf() {
        return faultConf;
    }

    public void setFaultConf(FaultConf faultConf) {
        this.faultConf = faultConf;
    }

    public String getEnvType() {
        return envType;
    }

    public void setEnvType(String envType) {
        this.envType = envType;
    }


}
