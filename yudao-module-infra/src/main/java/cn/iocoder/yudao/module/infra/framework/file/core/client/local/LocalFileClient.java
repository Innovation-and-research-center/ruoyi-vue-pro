package cn.iocoder.yudao.module.infra.framework.file.core.client.local;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.iocoder.yudao.module.infra.framework.file.core.client.AbstractFileClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileContent;

/**
 * 本地文件客户端
 *
 * @author 芋道源码
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    public LocalFileClient(Long id, LocalFileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        // 执行写入
        String filePath = getFilePath(path);
        FileUtil.writeBytes(content, filePath);
        // 拼接返回路径
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public void delete(String path) {
        String filePath = getFilePath(path);
        FileUtil.del(filePath);
    }

    @Override
    public byte[] getContent(String path) {
        String filePath = getFilePath(path);
        try {
            return FileUtil.readBytes(filePath);
        } catch (IORuntimeException ex) {
            if (ex.getMessage().startsWith("File not exist:")) {
                return null;
            }
            throw ex;
        }
    }

    @Override
    public FileContent openContent(String path) throws Exception {
        Path base = Paths.get(config.getBasePath()).toRealPath();
        Path target = base.resolve(path).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        if (!Files.exists(target)) {
            return null;
        }
        target = target.toRealPath();
        if (!target.startsWith(base) || !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("非法的文件路径");
        }
        return new FileContent(Files.newInputStream(target), Files.size(target));
    }

    private String getFilePath(String path) {
        return config.getBasePath() + File.separator + path;
    }

}
