package com.sjm.core.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Sockets {
    public static Socket create(String host, int port, SSLFactory ssl) throws IOException {
        return ssl != null ? ssl.createSocket(host, port) : new Socket(host, port);
    }

    public static ServerSocket createServer(int port, SSLFactory ssl) throws IOException {
        return ssl != null ? ssl.createServerSocket(port) : new ServerSocket(port);
    }

    public static class Address {
        public String host;
        public int port;

        public Address(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public Address(String addr) {
            String[] strs = addr.split(":");
            if (strs.length != 2)
                throw new IllegalArgumentException();
            this.host = strs[0];
            this.port = Integer.parseInt(strs[1]);
        }

        public Address() {}

        @Override
        public String toString() {
            return host + ":" + port;
        }
    }

    public static abstract class SSLFactory {
        public abstract Socket createSocket(String host, int port) throws IOException;

        public abstract ServerSocket createServerSocket(int port) throws IOException;
    }
    public static abstract class AbstractSSLFactory extends SSLFactory {
        protected abstract SSLContext getSSLContextImpl() throws Exception;

        private SSLContext ctx;

        private SSLContext getSSLContext() throws IOException {
            if (ctx == null)
                try {
                    ctx = getSSLContextImpl();
                } catch (Exception e) {
                    throw new IOException("create ssl context fail", e);
                }
            return ctx;
        }

        private SSLSocketFactory socketFactory;

        private SSLSocketFactory getSSLSocketFactory() throws IOException {
            if (socketFactory == null)
                socketFactory = getSSLContext().getSocketFactory();
            return socketFactory;
        }

        private SSLServerSocketFactory serverSocketFactory;

        private SSLServerSocketFactory getSSLServerSocketFactory() throws IOException {
            if (serverSocketFactory == null)
                serverSocketFactory = getSSLContext().getServerSocketFactory();
            return serverSocketFactory;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return getSSLSocketFactory().createSocket(host, port);
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            return getSSLServerSocketFactory().createServerSocket(port);
        }
    }
    public static class SimpleSSLFactory extends AbstractSSLFactory {
        public static final SimpleSSLFactory INSTANCE = new SimpleSSLFactory();

        @Override
        protected SSLContext getSSLContextImpl() throws Exception {
            return SSLContext.getDefault();
        }
    }
    public static class DefaultSSLFactory extends AbstractSSLFactory {
        private String keyStoreFile;
        private String keyStorePass;
        private String trustStoreFile;
        private String keyPass;

        public DefaultSSLFactory(String keyStoreFile, String keyStorePass, String trustStoreFile,
                String keyPass) {
            this.keyStoreFile = keyStoreFile;
            this.keyStorePass = keyStorePass;
            this.trustStoreFile = trustStoreFile;
            this.keyPass = keyPass;
        }

        @Override
        protected SSLContext getSSLContextImpl() throws Exception {
            SSLContext ctx = SSLContext.getInstance("SSL");
            KeyStore keyStore = KeyStore.getInstance("JKS");
            KeyStore trustStore = KeyStore.getInstance("JKS");
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try (InputStream keyStoreIn = classLoader.getResourceAsStream(keyStoreFile);
                    InputStream trustStoreIn = classLoader.getResourceAsStream(trustStoreFile);) {
                keyStore.load(keyStoreIn, keyStorePass.toCharArray());
                trustStore.load(trustStoreIn, keyStorePass.toCharArray());
            }
            final String algorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
            kmf.init(keyStore, keyPass.toCharArray());
            tmf.init(trustStore);
            ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return ctx;
        }
    }

    public interface SocketHandler {
        public void handle(Socket socket) throws IOException;
    }

    public static SocketServer bind(int port, SSLFactory ssl, Executor executor,
            SocketHandler handler) throws IOException {
        if (executor == null)
            executor = Misc.DEAULT_EXECUTOR;
        SocketServer server = new SocketServer(port, ssl, executor, handler);
        executor.execute(server);
        return server;
    }

    public static class SocketServer implements Runnable {
        private ServerSocket server;
        private boolean running;
        private Executor executor;
        private SocketHandler handler;

        public SocketServer(int port, SSLFactory ssl, Executor executor, SocketHandler handler)
                throws IOException {
            this.server = Sockets.createServer(port, ssl);
            this.executor = executor;
            this.handler = handler;
            this.running = true;
        }

        public void stop() throws IOException {
            running = false;
            server.close();
        }

        @Override
        public void run() {
            while (running)
                try {
                    final Socket client = server.accept();
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try (Socket socket = client) {
                                handler.handle(socket);
                            } catch (IOException e) {
                            }
                        }
                    });
                } catch (IOException e) {
                }
        }
    }

    public static class SmtpClient implements Closeable {
        private static final String CRLF = "\r\n";
        private static final byte[] CRLF_BYTES = CRLF.getBytes();

        private String user;
        private String pass;
        private String host;
        private int port;
        private boolean ssl;

        public SmtpClient(String user, String pass, String host, int port, boolean ssl) {
            this.user = user;
            this.pass = pass;
            this.host = host;
            this.port = port;
            this.ssl = ssl;
        }

        private Socket socket;
        private InputStream in;
        private OutputStream out;

        private void println() throws IOException {
            out.write(CRLF_BYTES);
            out.flush();
            checkResponse();
        }

        private void println(byte[] bytes) throws IOException {
            out.write(bytes);
            println();
        }

        private void println(String str) throws IOException {
            println(str.getBytes());
        }

        private byte[] encodeToBase64Bytes(String str) {
            return IOUtil.encodeCharset(Strings.encodeBase64(str.getBytes()),
                    Charset.defaultCharset());
        }

        private IOUtil.ByteVector vec;

        private void checkResponse() throws IOException {
            while (true) {
                vec.clear();
                if (IOUtil.readLine(in, vec) == -1)
                    throw new IOException("can not read line");
                if (Misc.DEBUG)
                    System.out.println(vec.toString());
                if (vec.size() < 4)
                    throw new IOException("response is too small");
                if (vec.get(3) == ' ')
                    break;
            }
            int code;
            try {
                code = Numbers.parseIntWithoutSign(vec.getLocalBytes(), null, 10, 0, 3);
            } catch (NumberFormatException e) {
                throw new IOException("illegal response code number format");
            }
            if (code >= 400)
                throw new IOException(vec.toString());
        }

        public void connect() throws IOException {
            socket = Sockets.create(host, port, ssl ? SimpleSSLFactory.INSTANCE : null);
            in = IOUtil.buffer(socket.getInputStream());
            out = IOUtil.buffer(socket.getOutputStream());
            vec = new IOUtil.ByteVector();
            checkResponse();
            println("EHLO " + host);
            println("AUTH LOGIN");
            println(encodeToBase64Bytes(user));
            println(encodeToBase64Bytes(pass));
        }

        public void sendMail(String to, String subject, String content) throws IOException {
            println("MAIL FROM:<" + user + ">");
            println("RCPT TO:<" + to + ">");
            println("DATA");
            println(new MyStringBuilder().append("Content-Type:text/html").append(CRLF)
                    .append("Subject:=?utf-8?B?").appendBase64(subject.getBytes()).append("?=")
                    .append(CRLF).append("From:").append('<').append(user).append('>').append(CRLF)
                    .append("To:").append('<').append(to).append('>').append(CRLF).append(CRLF)
                    .append(content).append(CRLF).append('.').append(CRLF).toString());
        }

        @Override
        public void close() throws IOException {
            try {
                println("QUIT");
            } catch (Exception e) {
            }
            Misc.close(out);
            Misc.close(in);
            Misc.close(socket);
        }
    }
    public static enum FtpCmd {
        USER, PASS, PWD, PASV, CWD, ABOR, TYPE, SIZE, REST, LIST, NLST, RETR, STOR, MKD, DELE, RMD, RNFR, RNTO, QUIT;

        private byte[] bytes = name().getBytes();

        public byte[] getBytes() {
            return bytes;
        }
    }
    public static class BaseFtpClient implements Closeable {
        private static final String CRLF = "\r\n";
        private static final byte[] CRLF_BYTES = CRLF.getBytes();

        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private IOUtil.ByteVector vec;
        private int code;

        public void checkResponse() throws IOException {
            vec.clear();
            if (IOUtil.readLine(in, vec) == -1)
                throw new IOException("can not read line");
            if (Misc.DEBUG)
                System.out.println(vec.toString());
            if (vec.size() < 3)
                throw new IOException("response is too small");
            try {
                code = Numbers.parseIntWithoutSign(vec.getLocalBytes(), null, 10, 0, 3);
            } catch (NumberFormatException e) {
                throw new IOException("illegal response code number format");
            }
            if (code >= 400)
                throw new IOException(vec.toString());
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return new String(vec.getLocalBytes(), 4, vec.size());
        }

        public long getLongMessage() {
            return Numbers.parseLongWithoutSign(vec.getLocalBytes(), null, 10, 4, vec.size());
        }

        public String getBracketsMessage() {
            return Strings.substring(vec.getLocalBytes(), null, 0, vec.size() - 1, '(', ')');
        }

        public void connect(String host, int port, SSLFactory ssl) throws IOException {
            socket = Sockets.create(host, port, ssl);
            in = IOUtil.buffer(socket.getInputStream());
            out = IOUtil.buffer(socket.getOutputStream());
            vec = new IOUtil.ByteVector();
            checkResponse();
        }

        public void run(FtpCmd cmd, byte[] arg) throws IOException {
            out.write(cmd.getBytes());
            if (arg != null) {
                out.write(' ');
                out.write(arg);
            }
            out.write(CRLF_BYTES);
            out.flush();
            checkResponse();
        }

        @Override
        public void close() throws IOException {
            Misc.close(in);
            Misc.close(out);
            Misc.close(socket);
        }
    }
    public static class FtpClient implements Closeable {
        private String user;
        private String pass;
        private String host;
        private int port;
        private SSLFactory ssl;
        private String charset;

        public FtpClient(String user, String pass, String host, int port, SSLFactory ssl,
                String charset) {
            this.user = user;
            this.pass = pass;
            this.host = host;
            this.port = port;
            this.ssl = ssl;
            this.charset = charset;
        }

        private Charset cst;

        private Charset getCharset() throws IOException {
            if (cst == null)
                cst = IOUtil.getCharsetThrow(charset);
            return cst;
        }

        private BaseFtpClient client;

        private void run(FtpCmd cmd, Object arg) throws IOException {
            if (client == null)
                connect();
            client.run(cmd, arg == null ? null : arg.toString().getBytes(getCharset()));
        }

        private void run(FtpCmd cmd) throws IOException {
            run(cmd, null);
        }

        private Socket openPasvSocket() throws IOException {
            run(FtpCmd.PASV);
            String response = client.getBracketsMessage();
            if (response == null)
                throw new IOException("illegal PASV response");
            String[] result = response.split(",");
            if (result.length != 6)
                throw new IOException("illegal PASV response");
            String host = result[0] + "." + result[1] + "." + result[2] + "." + result[3];
            int port = (Integer.parseInt(result[4]) << 8) + Integer.parseInt(result[5]);
            return Sockets.create(host, port, ssl);
        }

        private void getData(OutputStream out, FtpCmd cmd, Object arg) throws IOException {
            try (Socket socket = openPasvSocket(); InputStream in = socket.getInputStream()) {
                run(cmd, arg);
                IOUtil.copy(in, out, null, false);
            } finally {
                client.checkResponse();
            }
        }

        private void setData(ByteData data, FtpCmd cmd, Object arg) throws IOException {
            try (Socket socket = openPasvSocket(); OutputStream out = socket.getOutputStream()) {
                run(cmd, arg);
                data.write(out);
            } finally {
                client.checkResponse();
            }
        }

        public synchronized void connect() throws IOException {
            client = new BaseFtpClient();
            client.connect(host, port, ssl);
            run(FtpCmd.USER, user);
            run(FtpCmd.PASS, pass);
            type("I");
        }

        public synchronized String pwd() throws IOException {
            run(FtpCmd.PWD);
            return client.getMessage();
        }

        public synchronized void cd(String dir) throws IOException {
            run(FtpCmd.CWD, dir);
        }

        public synchronized void abort() throws IOException {
            run(FtpCmd.ABOR);
        }

        public synchronized void type(String type) throws IOException {// A ascii,I binary
            run(FtpCmd.TYPE, type);
        }

        public synchronized long size(String file) throws IOException {
            run(FtpCmd.SIZE, file);
            return client.getLongMessage();
        }

        public synchronized void reset(int offset) throws IOException {
            run(FtpCmd.REST, offset);
        }

        public synchronized String listFile(String dir) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getData(out, FtpCmd.LIST, dir);
            return new String(out.toByteArray(), getCharset());
        }

        public synchronized List<String> list(String dir) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            getData(out, FtpCmd.NLST, dir);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            IOUtil.ByteVector vec = new IOUtil.ByteVector();
            List<String> lines = new ArrayList<>();
            while (IOUtil.readLine(in, vec) != -1) {
                lines.add(vec.toString(getCharset()));
                vec.clear();
            }
            return lines;
        }

        public synchronized void getFile(OutputStream out, String file) throws IOException {
            if (size(file) != 0)
                getData(out, FtpCmd.RETR, file);
        }

        public synchronized void setFile(ByteData data, String file) throws IOException {
            setData(data, FtpCmd.STOR, file);
        }

        public synchronized void mkdir(String dir) throws IOException {
            run(FtpCmd.MKD, dir);
        }

        public synchronized void delete(String file) throws IOException {
            run(FtpCmd.DELE, file);
        }

        public synchronized void rmdir(String dir) throws IOException {
            run(FtpCmd.RMD, dir);
        }

        public synchronized void rename(String from, String to) throws IOException {
            run(FtpCmd.RNFR, from);
            run(FtpCmd.RNTO, to);
        }

        @Override
        public synchronized void close() throws IOException {
            try {
                run(FtpCmd.QUIT);
            } finally {
                client.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Misc.DEBUG = false;

        // SmtpClient client = new SmtpClient("13133666574@126.com", "", "smtp.126.com", 25, false);
        // client.connect();
        // client.sendMail("sunjingming@hotpu.cn", "How are you", "Hello World");
        // client.close();

        // FtpClient client = new FtpClient("sun", "sun", "127.0.0.1", 21, null, "gbk");
        // client.connect();
        // System.out.println(client.list(null));
        // System.out.println(client.listFile(null));
        // ByteArrayOutputStream out = new ByteArrayOutputStream();
        // client.getFile(out, "a.csv");
        // System.out.println(new String(out.toByteArray(), "gbk"));
        // client.setFile(ByteData.valueOf("123456789"), "b.txt");
        // client.close();

        // SSLFactory ssl = null;
        // SSLFactory ssl = SimpleSSLFactory.INSTANCE;
        SSLFactory ssl =
                new DefaultSSLFactory("hotpu.jks", "61559355", "hotpu-trust.jks", "61559355");

        ServerSocket ssk = ssl.createServerSocket(5522);

        Socket sk = ssk.accept();

        InputStream in = sk.getInputStream();

        String str = ByteData.valueOf(in).toString();

        in.close();

        sk.close();

        ssk.close();

        System.out.println(str);
    }
}
