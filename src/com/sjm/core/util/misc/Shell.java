package com.sjm.core.util.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.sjm.core.util.core.IOUtil;

public abstract class Shell {
    public static void main(String[] args) throws IOException {
        run(Shell.forName("sh"), System.in, System.out, System.err, null);
        // run(Shell.forName("cmd"), System.in, System.out, System.err, null);
    }

    public static Shell forName(String name) {
        switch (name) {
            case "js":
                return new JsShell(new ScriptEngineManager().getEngineByName("js"));
            case "cmd":
                return new SystemShell("cmd", "gbk");
            case "sh":
                return new SystemShell("sh", "utf8");
            default:
                throw new IllegalArgumentException(name);
        }
    }

    public static void run(Shell shell, InputStream in, OutputStream out, OutputStream err,
            String charset) throws IOException {
        shell.setWriter(IOUtil.buffer(IOUtil.toWriter(out, charset)));
        shell.setErrorWriter(IOUtil.buffer(IOUtil.toWriter(err, charset)));
        shell.start();
        while (true) {
            IOUtil.ByteSequence seq = new IOUtil.ByteSequence(4096);
            int n = seq.readLine(in);
            if (n == -1)
                break;
            String line = new String(seq.getLocalBytes(), 0, n, IOUtil.getCharset(charset));
            seq.clear();
            shell.execute(line);
        }
    }

    public abstract void execute(String script);

    public abstract void setWriter(Writer out);

    public abstract void setErrorWriter(Writer err);

    public abstract void start() throws IOException;

    private static void print(Writer out, String str) {
        try {
            out.write(str);
            out.flush();
        } catch (Exception e) {
        }
    }

    private static void println(Writer out, String str) {
        try {
            out.write(str);
            out.write(IOUtil.LINE_SEPARATOR);
            out.flush();
        } catch (Exception e) {
        }
    }

    public static class JsShell extends Shell {
        private Writer out, err;
        private ScriptEngine engine;

        public JsShell(ScriptEngine engine) {
            this.engine = engine;
        }

        @Override
        public void execute(String script) {
            try {
                Object result = engine.eval(script);
                if (result != null)
                    println(out, result.toString());
                print(out, "jjs> ");
            } catch (Exception e) {
                try {
                    println(err, e.getMessage());
                } catch (Exception ex) {
                }
            }
        }

        @Override
        public void setWriter(Writer out) {
            this.out = out;
            engine.getContext().setWriter(out);
        }

        @Override
        public void setErrorWriter(Writer err) {
            this.err = err;
            engine.getContext().setErrorWriter(err);
        }

        @Override
        public void start() {
            print(out, "jjs> ");
        }
    }
    public static class SystemShell extends Shell {
        private String cmd;
        private String charset;
        private Writer out, err;
        private Writer pw;

        public SystemShell(String cmd, String charset) {
            this.cmd = cmd;
            this.charset = charset;
        }

        @Override
        public void execute(String script) {
            println(pw, script);
        }

        @Override
        public void setWriter(Writer out) {
            this.out = out;
        }

        @Override
        public void setErrorWriter(Writer err) {
            this.err = err;
        }

        @Override
        public void start() throws IOException {
            Process p = Runtime.getRuntime().exec(cmd);
            pw = IOUtil.toWriter(p.getOutputStream(), charset);
            startDaemonThread(() -> processOutput(p));
            startDaemonThread(() -> processError(p));
        }

        private void processOutput(Process p) {
            try (Reader in = IOUtil.toReader(p.getInputStream(), charset)) {
                char[] buffer = new char[4096];
                for (int len; (len = in.read(buffer)) != -1;) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
            } catch (IOException e) {
            }
        }

        private void processError(Process p) {
            try (Reader in = IOUtil.toReader(p.getErrorStream(), charset)) {
                char[] buffer = new char[4096];
                for (int len; (len = in.read(buffer)) != -1;) {
                    err.write(buffer, 0, len);
                    err.flush();
                }
            } catch (IOException e) {
            }
        }

        private void startDaemonThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.start();
        }
    }
}
