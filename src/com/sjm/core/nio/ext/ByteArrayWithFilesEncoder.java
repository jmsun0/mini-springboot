package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import com.sjm.core.nio.core.ChannelContext;
import com.sjm.core.nio.core.ChannelEncoder;
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
public class ByteArrayWithFilesEncoder extends ChannelEncoder {

    static final int STATE_DATAS_FILES_LENGTH_SIZE = 0;
    static final int STATE_DATAS_LENGTH = 1;
    static final int STATE_FILES_LENGTH = 2;
    static final int STATE_DATAS = 3;
    static final int STATE_FILES = 4;

    static class EncodeContext implements Closeable {
        public int state;
        public byte[] buffer = new byte[8];
        public int bufferIndex;
        public long[] filesSizes;
        public int index;
        public long position;
        public ByteArrayWithFiles packet;
        public FileChannel fc;
        public boolean needInit = true;

        @Override
        public void close() throws IOException {
            Misc.close(fc);
        }
    }

    @Override
    protected boolean encode(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        int n = -1;
        switch (ec.state) {
            case STATE_DATAS_FILES_LENGTH_SIZE: {
                if (ec.needInit) {
                    if (ctx.writeQueue.isEmpty())
                        return false;
                    ec.needInit = false;
                    ec.bufferIndex = 0;
                    ec.packet = (ByteArrayWithFiles) ctx.writeQueue.peek();
                    ec.filesSizes = new long[ec.packet.files.size()];
                    for (int i = 0; i < ec.filesSizes.length; i++) {
                        ec.filesSizes[i] = ec.packet.files.get(i).length();
                    }
                    NIOTools.putInt(ec.buffer, 0, ec.packet.datas.size() * 4);
                    NIOTools.putInt(ec.buffer, 4, ec.packet.files.size() * 8);
                }
                n = writeBuffer(ctx, ec, 8);
                if (ec.bufferIndex == 8) {
                    ec.needInit = true;
                    ec.index = 0;
                    ec.state = STATE_DATAS_LENGTH;
                }
                break;
            }
            case STATE_DATAS_LENGTH: {
                if (ec.index == ec.packet.datas.size()) {
                    ec.index = 0;
                    ec.state = STATE_FILES_LENGTH;
                    break;
                }
                if (ec.needInit) {
                    ec.needInit = false;
                    ec.bufferIndex = 0;
                    NIOTools.putInt(ec.buffer, 0, ec.packet.datas.get(ec.index).length);
                }
                n = writeBuffer(ctx, ec, 4);
                if (ec.bufferIndex == 4) {
                    ec.index++;
                    ec.needInit = true;
                }
                break;
            }
            case STATE_FILES_LENGTH: {
                if (ec.index == ec.filesSizes.length) {
                    ec.index = 0;
                    ec.position = 0;
                    ec.state = STATE_DATAS;
                    break;
                }
                if (ec.needInit) {
                    ec.needInit = false;
                    ec.bufferIndex = 0;
                    NIOTools.putLong(ec.buffer, 0, ec.filesSizes[ec.index]);
                }
                n = writeBuffer(ctx, ec, 8);
                if (ec.bufferIndex == 8) {
                    ec.index++;
                    ec.needInit = true;
                }
                break;
            }
            case STATE_DATAS: {
                if (ec.index == ec.packet.datas.size()) {
                    ec.index = 0;
                    ec.state = STATE_FILES;
                    break;
                }
                byte[] currentData = ec.packet.datas.get(ec.index);
                n = Math.min(ctx.writeBuffer.remaining(), currentData.length - (int) ec.position);
                ctx.writeBuffer.put(currentData, (int) ec.position, n);
                ec.position += n;
                if (ec.position == currentData.length) {
                    ec.index++;
                    ec.position = 0;
                }
                break;
            }
            case STATE_FILES: {
                if (ec.index == ec.filesSizes.length) {
                    finish(ctx, ec);
                    break;
                }
                openFile(ec);
                n = ec.fc.read(ctx.writeBuffer);
                if (n == -1)
                    throw new IOException();
                finishCurrent(ec, n);
                break;
            }
        }
        return n != 0;
    }

    @Override
    protected int beforeWrite(ChannelContext ctx) throws IOException {
        EncodeContext ec = getEncodeContext(ctx);
        if (ec.state == STATE_FILES) {
            while (true) {
                if (ec.index == ec.filesSizes.length) {
                    finish(ctx, ec);
                    break;
                }
                openFile(ec);
                long n = ec.fc.transferTo(ec.position, ec.filesSizes[ec.index] - ec.position,
                        ctx.channel);
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

    private int writeBuffer(ChannelContext ctx, EncodeContext ec, int count) {
        int n = Math.min(ctx.writeBuffer.remaining(), count - ec.bufferIndex);
        ctx.writeBuffer.put(ec.buffer, ec.bufferIndex, n);
        ec.bufferIndex += n;
        return n;
    }

    private void finish(ChannelContext ctx, EncodeContext ec) throws IOException {
        ec.packet = null;
        ctx.writeQueue.poll();
        ec.state = STATE_DATAS_FILES_LENGTH_SIZE;
    }

    private void openFile(EncodeContext ec) throws IOException {
        if (ec.needInit) {
            ec.needInit = false;
            ec.position = 0;
            ec.fc = FileChannel.open(ec.packet.files.get(ec.index).toPath(),
                    StandardOpenOption.READ);
        }
    }

    private void finishCurrent(EncodeContext ec, long n) throws IOException {
        ec.position += n;
        long currentFileSize = ec.filesSizes[ec.index];
        if (ec.position == currentFileSize) {
            ec.needInit = true;
            ec.fc.close();
            ec.fc = null;
            ec.position = 0;
            ec.index++;
        } else if (ec.position > currentFileSize)
            throw new IOException();
    }
}
