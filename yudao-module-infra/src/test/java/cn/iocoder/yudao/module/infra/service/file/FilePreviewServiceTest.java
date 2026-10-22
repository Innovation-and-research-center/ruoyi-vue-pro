package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePreviewReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePreviewRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FilePreviewServiceTest {

    private final FileMapper fileMapper = mock(FileMapper.class);
    private final FileConfigService configService = mock(FileConfigService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final Map<String, String> tickets = new HashMap<>();
    private final FilePreviewService service = new FilePreviewService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "fileMapper", fileMapper);
        ReflectionTestUtils.setField(service, "fileConfigService", configService);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        when(redis.opsForValue()).thenReturn(values);
        when(configService.getFileClient(anyLong())).thenReturn(mock(FileClient.class));
        when(configService.getMasterFileConfig()).thenReturn(FileConfigDO.builder().id(29L).build());
        doAnswer(call -> {
            tickets.put(call.getArgument(0), call.getArgument(1));
            assertEquals(Duration.ofMinutes(30), call.getArgument(2));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(values.get(anyString())).thenAnswer(call -> tickets.get(call.getArgument(0)));
    }

    @Test
    void idWinsAndPreservesOriginalNameWithoutPuttingItInUrl() {
        FileDO record = record(42L, "2026/存储名.pdf");
        when(fileMapper.selectById(42L)).thenReturn(record);
        FilePreviewReqVO request = legacy("../invalid-fallback.pdf");
        request.setFileId(42L);
        FilePreviewRespVO response = service.prepare(request);
        assertTrue(response.getCacheName().startsWith("file_42_"));
        assertTrue(response.getPreviewPath().matches("/infra/file/preview/content/[a-f0-9]{32}/[a-zA-Z0-9_.-]+"));
        assertEquals("原始中文 #1?%.pdf", response.getName());
        FilePreviewService.PreviewFile file = resolve(response);
        assertNotNull(file);
        assertEquals(record.getPath(), file.getPath());
        assertEquals(29L, file.getConfigId());
    }

    @Test
    void missingIdNeverFallsBackToPath() {
        FilePreviewReqVO request = legacy("valid.pdf");
        request.setFileId(404L);
        assertThrows(RuntimeException.class, () -> service.prepare(request));
        verify(redis, never()).opsForValue();
        verifyNoInteractions(configService);
    }

    @Test
    void legacyRawPathPreservesSpecialCharacters() {
        String path = "2026/中文 空格(1)[2]#?%+%23.pdf";
        FilePreviewRespVO response = service.prepare(legacy(path));
        assertEquals(path, resolve(response).getPath());
        assertFalse(response.getPreviewPath().contains("%"));
        assertFalse(response.getPreviewPath().contains("?"));
        assertEquals(1800, response.getExpiresIn());
    }

    @Test
    void legacyDownloadUrlDecodesOnlyOnceAndKeepsPlus() {
        FilePreviewReqVO request = legacy("https://old-host/admin-api/infra/file/29/get/2026/"
                + "%E4%B8%AD%E6%96%87%20%231%3F%25+%2523.pdf?preview=true");
        FilePreviewService.PreviewFile file = resolve(service.prepare(request));
        assertEquals("2026/中文 #1?%+%23.pdf", file.getPath());
        assertEquals(29L, file.getConfigId());
    }

    @Test
    void registeredStorageUrlUsesDatabasePathWithoutProxyingRemoteUrl() {
        String url = "https://storage.example/bucket/file.pdf";
        FileDO record = record(42L, "2026/中文 #?%.pdf");
        when(fileMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.toolkit.support.SFunction<FileDO, ?>>any(), eq(url)))
                .thenReturn(record);
        when(fileMapper.selectById(42L)).thenReturn(record);
        FilePreviewService.PreviewFile file = resolve(service.prepare(legacy(url)));
        assertEquals(42L, file.getFileId());
        assertEquals(record.getPath(), file.getPath());
    }

    @Test
    void freshTicketsKeepStableCacheNameAndDifferentPathsDoNotCollide() {
        FilePreviewRespVO first = service.prepare(legacy("a/同名.docx"));
        FilePreviewRespVO second = service.prepare(legacy("a/同名.docx"));
        FilePreviewRespVO other = service.prepare(legacy("b/同名.docx"));
        assertNotEquals(first.getPreviewPath(), second.getPreviewPath());
        assertEquals(first.getCacheName(), second.getCacheName());
        assertNotEquals(first.getCacheName(), other.getCacheName());
    }

    @Test
    void differentFileIdsWithSameNameDoNotCollideAndUpdatesInvalidateTickets() {
        FileDO first = record(1L, "same.docx");
        FileDO second = record(2L, "same.docx");
        when(fileMapper.selectById(1L)).thenReturn(first);
        when(fileMapper.selectById(2L)).thenReturn(second);
        FilePreviewReqVO request = new FilePreviewReqVO();
        request.setFileId(1L);
        FilePreviewRespVO response = service.prepare(request);
        request.setFileId(2L);
        assertNotEquals(response.getCacheName(), service.prepare(request).getCacheName());
        first.setUpdateTime(LocalDateTime.of(2026, 10, 21, 0, 0));
        assertNull(resolve(response));
        when(fileMapper.selectById(1L)).thenReturn(null);
        assertNull(resolve(response));
    }

    @Test
    void expiredTamperedAndMismatchedTicketsCannotRead() {
        FilePreviewRespVO response = service.prepare(legacy("file.pdf"));
        String ticket = ticket(response);
        assertNull(service.resolve(ticket, "other.pdf"));
        assertNull(service.resolve("../invalid", response.getCacheName()));
        tickets.clear();
        assertNull(resolve(response));
    }

    @Test
    void rejectsTraversalRemoteProxyAndConflictingConfig() {
        for (String path : new String[]{"../a.pdf", "a/../b.pdf", "a/./b.pdf", "a\\..\\b.pdf", "https://example/a.pdf", "file:///tmp/a.pdf", ""}) {
            assertThrows(RuntimeException.class, () -> service.prepare(legacy(path)), path);
        }
        FilePreviewReqVO request = legacy("https://host/admin-api/infra/file/29/get/a.pdf");
        request.setConfigId(30L);
        assertThrows(RuntimeException.class, () -> service.prepare(request));
        assertThrows(RuntimeException.class, () -> service.prepare(legacy("https://host/admin-api/infra/file/29/get/%2e%2e/a.pdf")));
    }

    private FilePreviewReqVO legacy(String path) {
        FilePreviewReqVO request = new FilePreviewReqVO();
        request.setPath(path);
        return request;
    }

    private FileDO record(Long id, String path) {
        FileDO record = FileDO.builder().id(id).configId(29L).path(path).name("原始中文 #1?%.pdf").build();
        record.setUpdateTime(LocalDateTime.of(2026, 10, 20, 0, 0));
        return record;
    }

    private String ticket(FilePreviewRespVO response) {
        return response.getPreviewPath().split("/")[5];
    }

    private FilePreviewService.PreviewFile resolve(FilePreviewRespVO response) {
        return service.resolve(ticket(response), response.getCacheName());
    }
}
