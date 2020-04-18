package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelEncoder;
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
public class ByteArrayWithFilesEncoder extends ChannelEncoder {

    static final int STATE_HEADER_SIZE = 0;
    static final int STATE_DATA_OFFSET = 1;
    static final int STATE_FILES_OFFSETS = 2;
    static final int STATE_DATA = 3;
    static final int STATE_FILES = 4;

    static class EncodeContext implements Closeable {
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
        public boolean needInit = true;

        @Override
        public void close() throws IOException {
            Misc.close(fc);
            Misc.close(packet);
        }
    }

    @Override
    protected void encode(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        switch (ec.state) {
            case STATE_HEADER_SIZE: {
                if (ec.needInit) {
                    ec.needInit = false;
                    ec.bufferIndex = 0;
                    ec.packet = (ByteArrayWithFiles) ctx.writeQueue.peek();
                    ec.filesSizes = new long[ec.packet.files.size()];
                    for (int i = 0; i < ec.filesSizes.length; i++) {
                        ec.filesSizes[i] = ec.packet.files.get(i).length();
                    }
                    NIOTools.putInt(ec.buffer, (ec.filesSizes.length + 1) * 8);
                }
                writeBuffer(ctx, ec, 4);
                if (ec.bufferIndex == 4) {
                    ec.needInit = true;
                    ec.state = STATE_DATA_OFFSET;
                }
                break;
            }
            case STATE_DATA_OFFSET: {
                if (ec.needInit) {
                    ec.needInit = false;
                    ec.bufferIndex = 0;
                    ec.dataSize = ec.packet.data.length;
                    NIOTools.putInt(ec.buffer, ec.dataSize);
                }
                writeBuffer(ctx, ec, 4);
                if (ec.bufferIndex == 4) {
                    ec.needInit = true;
                    ec.filesIndex = 0;
                    ec.state = STATE_FILES_OFFSETS;
                }
                break;
            }
            case STATE_FILES_OFFSETS: {
                if (ec.filesIndex == ec.filesSizes.length) {
                    ec.position = 0;
                    ec.state = STATE_DATA;
                    break;
                }
                if (ec.needInit) {
                    ec.needInit = false;
                    ec.bufferIndex = 0;
                    NIOTools.putLong(ec.buffer, ec.filesSizes[ec.filesIndex]);
                }
                writeBuffer(ctx, ec, 8);
                if (ec.bufferIndex == 8) {
                    ec.filesIndex++;
                    ec.needInit = true;
                }
                break;
            }
            case STATE_DATA: {
                int n = Math.min(ctx.writeBuffer.remaining(), ec.dataSize - (int) ec.position);
                ctx.writeBuffer.put(ec.packet.data, (int) ec.position, n);
                ec.position += n;
                if (ec.position == ec.dataSize) {
                    ec.filesIndex = 0;
                    ec.state = STATE_FILES;
                }
                break;
            }
            case STATE_FILES: {
                if (ec.filesIndex == ec.filesSizes.length) {
                    finish(ctx, ec);
                    break;
                }
                openFile(ec);
                int n = ec.fc.read(ctx.writeBuffer);
                if (n == -1)
                    throw new IOException();
                finishCurrent(ec, n);
                break;
            }
        }
    }

    @Override
    protected int beforeWrite(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        if (ec.state == STATE_FILES) {
            while (true) {
                if (ec.filesIndex == ec.filesSizes.length) {
                    finish(ctx, ec);
                    break;
                }
                openFile(ec);
                long n = ec.fc.transferTo(ec.position, ec.fileSize - ec.position, ctx.channel);
                if (n <= 0) {
                    return (int) n;
                }
                finishCurrent(ec, n);
            }
        }
        return 1;
    }

    private EncodeContext getEncodeContext(ChannelContext ctx) {
        EncodeContext ec = (EncodeContext) ctx.encodeCotext;
        if (ec == null)
            ctx.encodeCotext = ec = new EncodeContext();
        return ec;
    }

    private void writeBuffer(ChannelContext ctx, EncodeContext ec, int count) {
        int n = Math.min(ctx.writeBuffer.remaining(), count - ec.bufferIndex);
        ctx.writeBuffer.put(ec.buffer, ec.bufferIndex, n);
        ec.bufferIndex += n;
    }

    private void finish(ChannelContext ctx, EncodeContext ec) throws IOException {
        ec.packet = null;
        ctx.writeQueue.poll();
        ec.state = STATE_HEADER_SIZE;
    }

    private void openFile(EncodeContext ec) throws IOException {
        if (ec.needInit) {
            ec.needInit = false;
            ec.position = 0;
            File file = ec.packet.files.get(ec.filesIndex);
            ec.fileSize = file.length();
            ec.fc = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        }
    }

    private void finishCurrent(EncodeContext ec, long n) throws IOException {
        ec.position += n;
        if (ec.position == ec.fileSize) {
            ec.needInit = true;
            ec.fc.close();
            ec.fc = null;
            ec.position = 0;
            ec.filesIndex++;
        } else if (ec.position > ec.fileSize)
            throw new IOException();
    }
}
