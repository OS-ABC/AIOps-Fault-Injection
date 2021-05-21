package com.pcxg.fitools.env;

import com.pcxg.fitools.utils.SSHConnection;

import java.util.Map;

public abstract class Environment {
    private SSHConnection ssh;
    abstract public Map<String, String[]> start();
    abstract public Map<String, String[]> startJob(String jobName);
    abstract public Map<String, String[]> end();

    public SSHConnection getSsh() {
        return ssh;
    }

    public void setSsh(SSHConnection ssh) {
        this.ssh = ssh;
    }
}