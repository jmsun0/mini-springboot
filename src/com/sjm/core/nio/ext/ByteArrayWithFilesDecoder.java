package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelDecoder;
import com.sjm.core.util.misc.Misc;

/**
 * 
 * 报文类型： ByteArrayWithFiles
 * 
 * 报文结构：
 * 
 * [datas.size(),4bytes] [files.size(),4bytes] [datas[n].length,4bytes]...
 * [files[n].length(),8bytes]... [datas[n]]... [files[n]]...
 * 
 * @author root
 *
 */
public class ByteArrayWithFilesDecoder extends ChannelDecoder {

    static final int STATE_DATAS_FILES_LENGTH_SIZE = 0;
    static final int STATE_DATAS_LENGTH = 1;
    static final int STATE_FILES_LENGTH = 2;
    static final int STATE_DATAS = 3;
    static final int STATE_FILES = 4;

    static class DecodeContext implements Closeable {
        public int state;
        public byte[] buffer = new byte[8];
        public int bufferIndex;
        public int[] datasSizes;
        public long[] filesSizes;
        public int index;
        public long position;
        public List<byte[]> datas;
        public List<File> files;
        public FileChannel fc;

        @Override
        public void close() throws IOException {
            Misc.close(fc);
            if (files != null)
                for (File file : files)
                    file.delete();
        }
    }

    @Override
    protected boolean decode(ChannelContext ctx) throws IOException {
        DecodeContext dc = getDecodeContext(ctx);
        int n = -1;
        switch (dc.state) {
            case STATE_DATAS_FILES_LENGTH_SIZE: {
                n = readBuffer(ctx, dc, 8);
                if (dc.bufferIndex == 8) {
                    dc.bufferIndex = 0;
                    int datasLengthSize = NIOTools.getInt(dc.buffer, 0);
                    if (datasLengthSize % 4 != 0)
                        throw new IOException();
                    int filesLengthSize = NIOTools.getInt(dc.buffer, 4);
                    if (filesLengthSize % 8 != 0)
                        throw new IOException();
                    dc.datasSizes = new int[datasLengthSize / 4];
                    dc.datas = new ArrayList<>(dc.datasSizes.length);
                    dc.filesSizes = new long[filesLengthSize / 8];
                    dc.files = new ArrayList<>(dc.filesSizes.length);
                    dc.index = 0;
                    dc.state = STATE_DATAS_LENGTH;
                }
                break;
            }
            case STATE_DATAS_LENGTH: {
                if (dc.index == dc.datasSizes.length) {
                    dc.index = 0;
                    dc.state = STATE_FILES_LENGTH;
                    break;
                }
                n = readBuffer(ctx, dc, 4);
                if (dc.bufferIndex == 4) {
                    dc.bufferIndex = 0;
                    dc.datasSizes[dc.index++] = NIOTools.getInt(dc.buffer, 0);
                }
                break;
            }
            case STATE_FILES_LENGTH: {
                if (dc.index == dc.filesSizes.length) {
                    dc.index = 0;
                    dc.position = 0;
                    dc.state = STATE_DATAS;
                    break;
                }
                n = readBuffer(ctx, dc, 8);
                if (dc.bufferIndex == 8) {
                    dc.filesSizes[dc.index++] = NIOTools.getLong(dc.buffer, 0);
                }
                break;
            }
            case STATE_DATAS: {
                if (dc.index == dc.datasSizes.length) {
                    dc.index = 0;
                    dc.state = STATE_FILES;
                    break;
                }
                int currentSize = dc.datasSizes[dc.index];
                if (dc.index == dc.datas.size()) {
                    dc.datas.add(new byte[currentSize]);
                    dc.position = 0;
                }
                byte[] currentData = dc.datas.get(dc.index);
                n = Math.min(ctx.readBuffer.remaining(), currentSize - (int) dc.position);
                ctx.readBuffer.get(currentData, (int) dc.position, n);
                dc.position += n;
                if (dc.position == currentSize) {
                    dc.index++;
                }
                break;
            }
            case STATE_FILES: {
                if (dc.index == dc.filesSizes.length) {
                    finish(ctx, dc);
                    break;
                }
                openFile(dc);
                int oldLimit = ctx.readBuffer.limit();
                n = (int) Math.min(ctx.readBuffer.remaining(), dc.filesSizes[dc.index]);
                ctx.readBuffer.limit(ctx.readBuffer.position() + n);
                if (dc.fc.write(ctx.readBuffer) != n)
                    throw new IOException();
                ctx.readBuffer.limit(oldLimit);
                finishCurrent(dc, n);
                break;
            }
        }
        return n != 0;
    }

    @Override
    protected int beforeRead(ChannelContext ctx) throws IOException {
        DecodeContext dc = getDecodeContext(ctx);
        if (dc.state == STATE_FILES) {
            while (true) {
                if (dc.index == dc.filesSizes.length) {
                    finish(ctx, dc);
                    break;
                }
                openFile(dc);
                long n = dc.fc.transferFrom(ctx.channel, dc.position,
                        dc.filesSizes[dc.index] - dc.position);
                if (n <= 0) {
                    return (int) n;
                }
                finishCurrent(dc, n);
            }
        }
        return 1;
    }

    private DecodeContext getDecodeContext(ChannelContext ctx) {
        DecodeContext dc = (DecodeContext) ctx.decodeCotext;
        if (dc == null)
            ctx.decodeCotext = dc = new DecodeContext();
        return dc;
    }

    private int readBuffer(ChannelContext ctx, DecodeContext dc, int count) {
        int n = Math.min(ctx.readBuffer.remaining(), count - dc.bufferIndex);
        ctx.readBuffer.get(dc.buffer, dc.bufferIndex, n);
        dc.bufferIndex += n;
        return n;
    }

    private void finish(ChannelContext ctx, DecodeContext dc) throws IOException {
        ctx.processer.process(ctx, new ByteArrayWithFiles(dc.datas, dc.files));
        dc.datas = null;
        dc.files = null;
        dc.bufferIndex = 0;
        dc.state = STATE_DATAS_FILES_LENGTH_SIZE;
    }

    private void openFile(DecodeContext dc) throws IOException {
        if (dc.index == dc.files.size()) {
            File file = Files.createTempFile(null, null).toFile();
            dc.files.add(file);
            dc.position = 0;
            dc.fc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
        }
    }

    private void finishCurrent(DecodeContext dc, long n) throws IOException {
        dc.position += n;
        if (dc.position == dc.filesSizes[dc.index]) {
            dc.fc.close();
            dc.fc = null;
            dc.index++;
        }
    }
}
