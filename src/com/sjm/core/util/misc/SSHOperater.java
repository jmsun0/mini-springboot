package com.sjm.core.util.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.sjm.core.logger.Logger;
import com.sjm.core.logger.LoggerFactory;
import com.sjm.core.util.core.ByteData;
import com.sjm.core.util.core.IOUtil;

public class SSHOperater implements AutoCloseable {
    static final Logger logger = LoggerFactory.getLogger(SSHOperater.class);

    private String user;
    private String host;
    private int port;
    private String pass; // 登录用户密码或私钥密码
    private byte[] identity;// 私钥
    private long connectTimeout; // 连接超时
    private long executeTimeout; // 执行超时
    private long waitInterval;// 等待间隔

    private Session session;

    public SSHOperater(String user, String host, int port, String pass, byte[] identity,
            long connectTimeout, long executeTimeout) {
        this.user = user;
        this.host = host;
        this.port = port;
        this.pass = pass;
        this.identity = identity;
        this.connectTimeout = connectTimeout;
        this.executeTimeout = executeTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String user;
        private String host;
        private int port;
        private String pass; // 登录用户密码或私钥密码
        private byte[] identity;// 私钥
        private long connectTimeout; // 连接超时，默认1分钟
        private long executeTimeout; // 执行超时，默认2小时
        private long waitInterval;// 等待间隔，默认100毫秒

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder pass(String pass) {
            this.pass = pass;
            return this;
        }

        public Builder identity(byte[] identity) {
            this.identity = identity;
            return this;
        }

        public Builder useSystemIdentity() throws IOException {
            return identity(
                    Files.readAllBytes(Paths.get(System.getProperty("user.home"), ".ssh/id_rsa")));
        }

        public Builder connectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder executeTimeout(long executeTimeout) {
            this.executeTimeout = executeTimeout;
            return this;
        }

        public Builder waitInterval(long waitInterval) {
            this.waitInterval = waitInterval;
            return this;
        }

        public SSHOperater build() {
            if (user == null || user.isEmpty())
                user = System.getProperty("user.name");// 默认当前进程用户
            if (port <= 0)
                port = 22;
            if (connectTimeout <= 0)
                connectTimeout = 60000;// 默认1分钟
            if (executeTimeout <= 0)
                executeTimeout = 7200000;// 默认两小时
            if (waitInterval <= 0)
                waitInterval = 100;// 默认100毫秒
            if (host == null || host.isEmpty())
                throw new IllegalArgumentException("host is required");
            if (identity == null && (pass == null || pass.isEmpty()))
                throw new IllegalArgumentException("pass is required");
            return new SSHOperater(user, host, port, pass, identity, connectTimeout,
                    executeTimeout);
        }
    }

    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        if (identity != null) {
            jsch.addIdentity("jsch", identity, null, pass != null ? pass.getBytes() : null);
        }
        // 根据用户名，主机ip，端口获取一个Session对象
        Session session = jsch.getSession(user, host, port);
        if (identity == null && pass != null && pass.length() > 0) {
            session.setPassword(pass); // 设置密码
        }
        Properties config = new Properties();
        config.put("userauth.gssapi-with-mic", "no");// GSSAPI关闭，加快访问速度
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout((int) executeTimeout); // 设置timeout时间
        session.setDaemonThread(true);
        session.connect((int) connectTimeout);
        return session;
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public Session getSession() throws JSchException {
        if (!isConnected()) {
            synchronized (this) {
                if (!isConnected()) {
                    close();
                    session = createSession();
                }
            }
        }
        return session;
    }

    public void close() {
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    @Override
    public String toString() {
        return "SSHOperater [user=" + user + ", host=" + host + ", port=" + port + "]";
    }

    private Channel openChannelWithoutRetry(String type) throws JSchException {
        Channel channel = null;
        try {
            channel = getSession().openChannel(type);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if (channel == null)
            throw new JSchException("ssh[" + host + "] open " + type + " channel failed");
        return channel;
    }

    private Channel openChannel(String type) throws JSchException, InterruptedException {
        int retryTimes = 2;
        for (int i = 0; i < retryTimes; i++) {// 重试两次
            try {
                return openChannelWithoutRetry(type);
            } catch (Exception e) {
                if (i == retryTimes - 1)
                    throw e;
                logger.warn("ssh[" + host + "] retry open " + type + " channel", e);
                Thread.sleep(500);
            }
        }
        throw new JSchException("unknown error");
    }

    public ChannelSftp openSftpChannel() throws JSchException, InterruptedException {
        return (ChannelSftp) openChannel("sftp");
    }

    public ChannelExec openExecChannel() throws JSchException, InterruptedException {
        return (ChannelExec) openChannel("exec");
    }

    public ChannelShell openShellChannel() throws JSchException, InterruptedException {
        return (ChannelShell) openChannel("shell");
    }

    public void connectChannel(Channel channel)
            throws JSchException, InterruptedException, TimeoutException {
        int retryTimes = 3;
        long statTime = System.currentTimeMillis();
        for (int i = 0; i < retryTimes; i++) {// 重试两次
            try {
                channel.connect((int) connectTimeout);
                return;
            } catch (Exception e) {
                if (i == retryTimes - 1)
                    throw e;
                if (System.currentTimeMillis() - statTime > connectTimeout)
                    throw new TimeoutException("ssh[" + host + "] connect channel timeout");
                logger.warn("ssh[" + host + "] retry connect channel", e);
                Thread.sleep(500);
            }
        }
        throw new JSchException("unknown error");
    }

    public int executeCmd(String cmd, long timeout, InputStream in, OutputStream out,
            OutputStream err) throws JSchException, InterruptedException, TimeoutException {
        if (timeout <= 0)
            timeout = executeTimeout;
        ChannelExec exec = null;
        try {
            exec = openExecChannel();
            exec.setCommand(cmd);
            exec.setInputStream(in);
            exec.setOutputStream(out);
            exec.setErrStream(err);
            connectChannel(exec);
            long endTime = System.currentTimeMillis() + timeout;
            while (!exec.isClosed() && System.currentTimeMillis() <= endTime) {
                Thread.sleep(waitInterval);
            }
            return exec.getExitStatus();
        } finally {
            if (exec != null)
                exec.disconnect();
        }
    }

    private static final Pattern PATTERN_PS1 = Pattern.compile("[#$][ ]*$");
    private static final Pattern PATTERN_PASS = Pattern.compile("password:");

    private static final Predicate<Expect> PRED_PS1 = e -> e.isLastLineRegexFound(PATTERN_PS1);
    private static final Predicate<Expect> PRED_PASS = e -> e.isLastLineRegexFound(PATTERN_PASS);

    public String executeSshCmd(String cmd, String password, long timeout)
            throws IOException, JSchException, InterruptedException, TimeoutException {
        if (timeout <= 0)
            timeout = executeTimeout;

        ChannelShell shell = null;
        Expect expect = null;
        Reader reader = null;
        Writer writer = null;
        try {
            shell = openShellChannel();
            reader = IOUtil.toReader(shell.getInputStream(), null);
            writer = IOUtil.toWriter(shell.getOutputStream(), null);
            connectChannel(shell);

            expect = new Expect(reader, writer);
            expect.setTimeout(timeout);

            int index;
            int fromBufferIndex;

            expect.expect(PRED_PS1);

            fromBufferIndex = expect.getBuffer().length();
            expect.send(cmd + "\n");
            index = expect.expect(PRED_PS1, PRED_PASS);
            if (index == 1) {
                fromBufferIndex = expect.getBuffer().length();
                expect.send(password + "\n");
                index = expect.expect(PRED_PS1, PRED_PASS);
                if (index == 1)
                    throw new JSchException("ssh password error");
            }
            String message = expect.getLastContent(fromBufferIndex);

            fromBufferIndex = expect.getBuffer().length();
            expect.send("echo $?\n");
            expect.expect(PRED_PS1);
            int code = Integer.parseInt(expect.getLastContent(fromBufferIndex).trim());

            if (code != 0)
                throw new JSchException(message);
            return message;
        } finally {
            if (shell != null)
                shell.disconnect();
            if (expect != null) {
                IOUtil.close(expect);
            } else {
                IOUtil.close(reader);
                IOUtil.close(writer);
            }
        }
    }

    public ByteData download(String remoteFile)
            throws JSchException, InterruptedException, SftpException, TimeoutException {
        ChannelSftp sftp = null;
        try {
            sftp = openSftpChannel();
            connectChannel(sftp);
            ChannelSftp sftpTmp = sftp;
            return ByteData.valueOf(sftp.get(remoteFile)).withCloseable(() -> sftpTmp.disconnect());
        } catch (Exception e) {
            if (sftp != null)
                sftp.disconnect();
            throw e;
        }
    }

    public void upload(String remoteFile, InputStream data)
            throws JSchException, InterruptedException, SftpException, TimeoutException {
        ChannelSftp sftp = null;
        try {
            sftp = openSftpChannel();
            connectChannel(sftp);
            sftp.put(data, remoteFile);
        } finally {
            if (sftp != null)
                sftp.disconnect();
        }
    }

    public SftpATTRS stat(String remoteFile) {
        ChannelSftp sftp = null;
        try {
            sftp = openSftpChannel();
            connectChannel(sftp);
            return sftp.lstat(remoteFile);
        } catch (Exception e) {
            logger.info(e.getMessage());
            return null;
        } finally {
            if (sftp != null)
                sftp.disconnect();
        }
    }
}
