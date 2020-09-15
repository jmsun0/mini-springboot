package com.sjm.core.util.misc;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.sjm.core.logger.Logger;
import com.sjm.core.logger.LoggerFactory;
import com.sjm.core.util.core.IOUtil;
import com.sjm.core.util.core.Strings;

public class Expect implements AutoCloseable {
    static final Logger logger = LoggerFactory.getLogger(Expect.class);

    private Reader reader;
    private Writer writer;

    private long timeout = 10000;

    private Thread thread;
    private StringBuilder buffer = new StringBuilder();
    private boolean closed;

    public Expect(Reader reader, Writer writer) {
        this.reader = reader;
        this.writer = writer;
        thread = new Thread(this::processRead, "Expect-" + System.identityHashCode(this));
        thread.setDaemon(true);
        thread.start();
    }

    private void processRead() {
        try {
            char[] chars = new char[1024];
            for (int len; !closed && (len = reader.read(chars)) != -1;) {
                synchronized (this) {
                    buffer.append(chars, 0, len);
                    notifyAll();
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        close();
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @SafeVarargs
    public synchronized final int expect(Predicate<Expect>... pres)
            throws InterruptedException, TimeoutException {
        long startTime = System.currentTimeMillis();
        while (true) {
            for (int i = 0; i < pres.length; i++) {
                Predicate<Expect> pre = pres[i];
                if (pre != null && pre.test(this))
                    return i;
            }
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > timeout)
                throw new TimeoutException();
            wait(timeout - elapsedTime);
        }
    }

    public void send(String data) throws IOException, InterruptedException, TimeoutException {
        int fromIndex = buffer.length();
        writer.write(data);
        writer.flush();
        expect(e -> e.buffer.length() != fromIndex);
    }

    public StringBuilder getBuffer() {
        return buffer;
    }

    public String getLastContent(int fromBufferIndex) {
        if (buffer.length() == 0)
            return null;
        int index = Strings.indexOf(buffer, '\n', fromBufferIndex, -1);
        if (index == -1)
            return null;
        int lastIndex = Strings.lastIndexOf(buffer, '\n', -1, -1);
        if (lastIndex == -1)
            return null;
        if (index == lastIndex)
            return "";
        return buffer.substring(index + 1, lastIndex);
    }

    public String getLastLine() {
        if (buffer.length() == 0)
            return "";
        int index = Strings.lastIndexOf(buffer, '\n', -1, -1);
        if (index == -1)
            return buffer.toString();
        return buffer.substring(index + 1);
    }

    public boolean isMatchSuffix(String suffix) {
        return Strings.endWith(buffer, buffer.length(), suffix);
    }

    public boolean isLastLineRegexFound(Pattern pattern) {
        String lastLine = getLastLine();
        return !lastLine.isEmpty() && pattern.matcher(lastLine).find();
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            IOUtil.close(reader);
            IOUtil.close(writer);
        }
    }
}
