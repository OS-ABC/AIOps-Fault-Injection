package com.pcxg.fitools.utils;

import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class SSHConnection {
    private static final Logger logger = LogManager.getLogger(SSHConnection.class);
    private static final int SESSION_TIMEOUT = 120000;
    private static final int CHANNEL_TIMEOUT = 120000;
    private final String host;
    private final String username;
    private final String password;
    private final int port;
    private final String workspace;
    private Session session;

    public SSHConnection(String host, String username, String password, int port, String workspace) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.port = port;
        this.workspace = workspace;
    }

    public void connect() {
        try {
            JSch jsch = new JSch();
            String path = this.getKnownHostsFilePath();
            jsch.setKnownHosts(path);
            session = jsch.getSession(this.username, this.host, this.port);
            session.setPassword(this.password);
            session.connect(SESSION_TIMEOUT);
        }catch (JSchException e) {
            e.printStackTrace();
            logger.error("SSH Connection failed");
        }
    }

    public void close() {
        session.disconnect();
    }

    /**
     * 上传文件到指定目录
     */
    public void uploadFile(String path, String name, InputStream inputStream) {
        try {
            getSession();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect(CHANNEL_TIMEOUT);
            channelSftp.cd("/");
            String[] folders = path.split( "/" );
            for ( String folder : folders ) {
                if ( folder.length() > 0 ) {
                    try {
                        channelSftp.cd( folder );
                    }
                    catch ( SftpException ee ) {
                        channelSftp.mkdir( folder );
                        channelSftp.cd( folder );
                    }
                }
            }
            channelSftp.put(inputStream, name, ChannelSftp.OVERWRITE);
            channelSftp.disconnect();
        } catch (Exception e) {
            logger.error("Upload file {} failed", path);
        }
    }
    private void getSession() {
        try {
            ChannelExec testChannel = (ChannelExec) session.openChannel("exec");
            testChannel.setCommand("true");
            testChannel.connect();
            if(logger.isDebugEnabled()) {
                logger.debug("Session successfully tested, use it again.");
            }
            testChannel.disconnect();
        } catch (Throwable t) {
            logger.info("Session terminated. Create a new one.");
            this.connect();
        }
    }
    /**
     *
     * @param command
     * @return 返回命令的标准输出、错误输出、开始时间、结束时间
     */
    public String[] runCommand(String command) {
        String[] msgs = new String[6];
        try {
            getSession();
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setCommand(command);
            InputStream err = channelExec.getErrStream();
            InputStream out = channelExec.getInputStream();
            StringBuilder stderr = new StringBuilder();
            StringBuilder stdout = new StringBuilder();
            //DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            channelExec.connect(CHANNEL_TIMEOUT);
            String startTime = String.valueOf(System.currentTimeMillis());
            byte[] tmp = new byte[1024];
            while(true) {
                while (out.available() > 0) {
                    int i = out.read(tmp, 0, 1024);
                    if (i < 0) break;
                    stdout.append(new String(tmp, 0, i));
                }
                while (err.available() > 0) {
                    int i = err.read(tmp, 0, 1024);
                    if (i < 0) break;
                    stderr.append(new String(tmp, 0, i));
                }
                if (channelExec.isClosed()) {
                    if ((out.available() > 0) || (err.available() > 0)) continue;
                    logger.info("{} Status: {}", command, channelExec.getExitStatus());
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ee) {
                    logger.error(ee.getMessage());
                }
            }
            String endTime = String.valueOf(System.currentTimeMillis());
            channelExec.disconnect();
            logger.info("cost {} ms",Long.parseLong(endTime)-Long.parseLong(startTime));
            msgs[0] = command;
            msgs[1] = stdout.toString();
            msgs[2] = stderr.toString();
            msgs[3] = String.valueOf(channelExec.getExitStatus());
            msgs[4] = startTime;
            msgs[5] = endTime;
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("SSH run command {} failed", command);
        }

        return msgs;
    }
    /**
    * 获取known_hosts路径
    */
    private String getKnownHostsFilePath() {
        String path = null;
        String os = System.getProperty("os.name").toLowerCase();
        if ("linux".equals(os)) {
            path = "~/.ssh/known_hosts";
        }else if(os.contains("windows")) {
            String home = System.getProperty("user.home").replace("\\","\\\\");
            path = home + File.separator + "\\.ssh\\known_hosts";
        }
        return path;
    }


    public String getWorkspace() {
        return workspace;
    }
}
