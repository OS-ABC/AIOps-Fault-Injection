package com.pcxg.fitools.tools;

import com.pcxg.fitools.entity.FaultConf;
import com.pcxg.fitools.utils.SSHConnection;

import java.util.Map;

public abstract class FaultInjectTool {
    private SSHConnection ssh;
    protected abstract boolean check(FaultConf faultConf);
    protected abstract Map<String, String[]> inject(FaultConf faultConf);

    public Map<String, String[]> run(FaultConf faultConf) {
        if (!check(faultConf)) {
            return null;
        }
        return inject(faultConf);
    }

    public SSHConnection getSsh() {
        return ssh;
    }

    public void setSsh(SSHConnection ssh) {
        this.ssh = ssh;
    }
}
