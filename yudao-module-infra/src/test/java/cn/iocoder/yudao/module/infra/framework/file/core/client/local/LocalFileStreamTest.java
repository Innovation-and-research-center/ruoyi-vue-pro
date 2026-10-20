package cn.iocoder.yudao.module.infra.framework.file.core.client.local;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStreamTest {

    @TempDir
    Path directory;

    @Test
    void readsChinesePathAndReportsMissingFiles() throws Exception {
        LocalFileClient client = client(directory);
        Files.write(directory.resolve("中文 #1.pdf"), new byte[]{1, 2, 3});
        try (FileContent content = client.openContent("中文 #1.pdf")) {
            assertEquals(3, content.getLength());
            assertEquals(1, content.getInputStream().read());
        }
        assertNull(client.openContent("missing.pdf"));
    }

    @Test
    void rejectsTraversalAndSymlinksOutsideStorage() throws Exception {
        Path root = Files.createDirectory(directory.resolve("root"));
        Files.write(directory.resolve("outside.pdf"), new byte[]{1});
        LocalFileClient client = client(root);
        assertThrows(IllegalArgumentException.class, () -> client.openContent("../outside.pdf"));
        Files.createSymbolicLink(root.resolve("link.pdf"), directory.resolve("outside.pdf"));
        assertThrows(IllegalArgumentException.class, () -> client.openContent("link.pdf"));
    }

    private LocalFileClient client(Path root) {
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setBasePath(root.toString());
        config.setDomain("http://localhost");
        return new LocalFileClient(29L, config);
    }
}
