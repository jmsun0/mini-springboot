package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelDecoder;
import com.sjm.core.nio.core.NIOTools;
import com.sjm.core.util.Misc;

/**
 * 
 * 报文类型： ByteArrayWithFiles
 * 
 * 报文结构： [header长度，包括自己(4字节)] [data长度(4字节)] [每个files长度(8字节)] [data][file1][file2]...
 * 
 * @author root
 *
 */
public class ByteArrayWithFilesDecoder extends ChannelDecoder {

    static final int STATE_HEADER_SIZE = 0;
    static final int STATE_DATA_OFFSET = 1;
    static final int STATE_FILES_OFFSETS = 2;
    static final int STATE_DATA = 3;
    static final int STATE_FILES = 4;

    static class DecodeContext implements Closeable {
        public int state;
        public byte[] buffer = new byte[8];
        public int bufferIndex;
        public int dataSize;
        public long[] filesSizes;
        public long fileSize;
        public int filesIndex;
        public long position;
        public ByteArrayWithFiles packet;
        public FileChannel fc;

        @Override
        public void close() throws IOException {
            Misc.close(fc);
            Misc.close(packet);
        }
    }

    @Override
    protected void decode(ChannelContext ctx) throws IOException {
        DecodeContext dc = getDecodeContext(ctx);
        switch (dc.state) {
            case STATE_HEADER_SIZE: {
                readBuffer(ctx, dc, 4);
                if (dc.bufferIndex == 4) {
                    dc.bufferIndex = 0;
                    int headerSize = NIOTools.getInt(dc.buffer);
                    if (headerSize % 8 != 0)
                        throw new IOException();
                    dc.filesSizes = new long[headerSize / 8 - 1];
                    dc.state = STATE_DATA_OFFSET;
                }
                break;
            }
            case STATE_DATA_OFFSET: {
                readBuffer(ctx, dc, 4);
                if (dc.bufferIndex == 4) {
                    dc.bufferIndex = 0;
                    dc.dataSize = NIOTools.getInt(dc.buffer);
                    dc.filesIndex = 0;
                    dc.state = STATE_FILES_OFFSETS;
                }
                break;
            }
            case STATE_FILES_OFFSETS: {
                if (dc.filesIndex == dc.filesSizes.length) {
                    dc.packet = new ByteArrayWithFiles(new byte[dc.dataSize], new ArrayList<>());
                    dc.position = 0;
                    dc.state = STATE_DATA;
                    break;
                }
                readBuffer(ctx, dc, 8);
                if (dc.bufferIndex == 8) {
                    dc.bufferIndex = 0;
                    dc.filesSizes[dc.filesIndex++] = NIOTools.getLong(dc.buffer);
                }
                break;
            }
            case STATE_DATA: {
                int n = Math.min(ctx.readBuffer.remaining(), dc.dataSize - (int) dc.position);
                ctx.readBuffer.get(dc.packet.data, (int) dc.position, n);
                dc.position += n;
                if (dc.position == dc.dataSize) {
                    dc.position = 0;
                    dc.filesIndex = 0;
                    dc.state = STATE_FILES;
                }
                break;
            }
            case STATE_FILES: {
                if (dc.filesIndex == dc.filesSizes.length) {
                    finish(ctx, dc);
                    break;
                }
                openFile(dc);
                int oldLimit = ctx.readBuffer.limit();
                int n = (int) Math.min(ctx.readBuffer.remaining(), dc.fileSize);
                ctx.readBuffer.limit(ctx.readBuffer.position() + n);
                if (dc.fc.write(ctx.readBuffer) != n)
                    throw new IOException();
                ctx.readBuffer.limit(oldLimit);
                finishCurrent(dc, n);
                break;
            }
        }
    }

    @Override
    protected int beforeRead(ChannelContext ctx) throws IOException {
        DecodeContext dc = getDecodeContext(ctx);
        if (dc.state == STATE_FILES) {
            while (true) {
                if (dc.filesIndex == dc.filesSizes.length) {
                    finish(ctx, dc);
                    break;
                }
                openFile(dc);
                long n = dc.fc.transferFrom(ctx.channel, dc.position, dc.fileSize - dc.position);
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

    private void readBuffer(ChannelContext ctx, DecodeContext dc, int count) {
        int n = Math.min(ctx.readBuffer.remaining(), count - dc.bufferIndex);
        ctx.readBuffer.get(dc.buffer, dc.bufferIndex, n);
        dc.bufferIndex += n;
    }

    private void finish(ChannelContext ctx, DecodeContext dc) throws IOException {
        ctx.processer.process(ctx, dc.packet);
        dc.packet = null;
        dc.bufferIndex = 0;
        dc.state = STATE_HEADER_SIZE;
    }

    private void openFile(DecodeContext dc) throws IOException {
        if (dc.fc == null) {
            File file = Files.createTempFile(null, null).toFile();
            dc.packet.files.add(file);
            dc.fc = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
            dc.fileSize = dc.filesSizes[dc.filesIndex];
        }
    }

    private void finishCurrent(DecodeContext dc, long n) throws IOException {
        dc.position += n;
        if (dc.position == dc.fileSize) {
            dc.fc.close();
            dc.fc = null;
            dc.position = 0;
            dc.filesIndex++;
        }
    }
}
