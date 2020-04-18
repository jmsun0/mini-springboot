package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ByteArrayWithFiles implements Closeable {
    public byte[] data;
    public List<File> files;

    public ByteArrayWithFiles(byte[] data, List<File> files) {
        this.data = data;
        this.files = files;
    }

    @Override
    public void close() throws IOException {
        for (File file : files)
            file.delete();
    }
}
