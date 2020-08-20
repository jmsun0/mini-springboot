package com.sjm.core.nio.ext;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ByteArrayWithFiles implements Closeable {
    public List<byte[]> datas;
    public List<File> files;

    public ByteArrayWithFiles(List<byte[]> datas, List<File> files) {
        this.datas = datas;
        this.files = files;
    }

    @Override
    public void close() throws IOException {
        for (File file : files)
            file.delete();
    }
}
