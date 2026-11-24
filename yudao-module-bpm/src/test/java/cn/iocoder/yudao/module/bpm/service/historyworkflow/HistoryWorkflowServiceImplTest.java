package cn.iocoder.yudao.module.bpm.service.historyworkflow;

import cn.hutool.core.util.URLUtil;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.LocalFileClientConfig;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HistoryWorkflowServiceImplTest {
    private final HistoryWorkflowServiceImpl service = new HistoryWorkflowServiceImpl();
    private static final String BASE = "https://example.test/oa/admin-api/infra/file/29/get";

    @BeforeEach
    void setUp() {
        FileConfigService configs = mock(FileConfigService.class);
        LocalFileClientConfig config = new LocalFileClientConfig();
        config.setDomain("https://example.test/oa");
        when(configs.getMasterFileConfig()).thenReturn(FileConfigDO.builder().id(29L).config(config).build());
        ReflectionTestUtils.setField(service, "fileConfigService", configs);
    }

    @Test
    void litigationAndReconsiderationAppendFilenameToLegacyDirectory() {
        for (String type : new String[]{"xzfy", "xzss"}) {
            String directory = "/法律法规流程/行政审批/case-id/user/";
            String name = "材料 中文#?%+.pdf";
            Map<String, Object> attachment = attachment(directory, name);
            fill(type, attachment);
            assertEquals(directory + name, attachment.get("filePath"));
            String url = (String) attachment.get("fileUrl");
            assertEquals(BASE + directory + name, URLUtil.decode(url, StandardCharsets.UTF_8, false));
            assertFalse(url.contains("#"));
            assertFalse(url.contains("?"));
            assertTrue(url.endsWith(".pdf"));
            fill(type, attachment);
            assertEquals(url, attachment.get("fileUrl"));
            assertEquals(directory, attachment.get("filepath"));
        }
    }

    @Test
    void completePathsAndExistingUrlsAreUnchanged() {
        for (String path : new String[]{"/folder/stored.pdf", "/folder/filename.pdf", "https://storage.test/file.pdf"}) {
            Map<String, Object> attachment = attachment(path, "filename.pdf");
            fill("xzfy", attachment);
            assertEquals(path.startsWith("https:") ? path : BASE + path, attachment.get("fileUrl"));
        }
        Map<String, Object> attachment = attachment("/old/directory", "filename.pdf");
        attachment.put("fileUrl", "https://storage.test/new.pdf");
        fill("xzss", attachment);
        assertEquals("https://storage.test/new.pdf", attachment.get("fileUrl"));
    }

    @Test
    void receiveDocAndOtherWorkflowRulesAreUnchanged() {
        Map<String, Object> business = new HashMap<>();
        business.put("type", "receive_doc");
        business.put("attachFilePath", "/receive/directory");
        Map<String, Object> received = attachment(null, "file.pdf");
        ReflectionTestUtils.invokeMethod(service, "fillAttachmentUrls", business, Collections.singletonList(received));
        assertEquals(BASE + "/receive/directory/file.pdf", received.get("fileUrl"));
        for (String type : new String[]{"leave", "time_explain", "confflow"}) {
            Map<String, Object> attachment = attachment("/unchanged/path", "file.pdf");
            fill(type, attachment);
            assertEquals(BASE + "/unchanged/path", attachment.get("fileUrl"));
        }
    }

    @Test
    void missingFilenameDoesNotInventAStoragePath() {
        Map<String, Object> attachment = attachment("/directory", null);
        fill("xzfy", attachment);
        assertEquals(BASE + "/directory", attachment.get("fileUrl"));
    }

    private void fill(String type, Map<String, Object> attachment) {
        ReflectionTestUtils.invokeMethod(service, "fillAttachmentUrls",
                Collections.singletonMap("type", type), Collections.singletonList(attachment));
    }

    private Map<String, Object> attachment(String path, String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("filepath", path);
        result.put("filename", name);
        return result;
    }
}
