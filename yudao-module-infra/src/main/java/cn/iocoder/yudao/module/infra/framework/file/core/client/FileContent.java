package cn.iocoder.yudao.module.infra.framework.file.core.client;

import java.io.IOException;
import java.io.InputStream;

/** 一次文件读取；调用方必须关闭，释放本地文件或对象存储连接。 */
public final class FileContent implements AutoCloseable {

    private final InputStream inputStream;
    private final long length;

    public FileContent(InputStream inputStream, long length) {
        this.inputStream = inputStream;
        this.length = length;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getLength() {
        return length;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
