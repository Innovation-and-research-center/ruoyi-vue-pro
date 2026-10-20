package cn.iocoder.yudao.module.infra.controller.admin.file;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileContent;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.FilePreviewService;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePreviewRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FilePreviewControllerTest {

    private final FileService fileService = mock(FileService.class);
    private final FilePreviewService previewService = mock(FilePreviewService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "fileService", fileService);
        ReflectionTestUtils.setField(controller, "filePreviewService", previewService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void previewUsesStreamAndClosesIt() throws Exception {
        ByteArrayInputStream input = spy(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}));
        when(fileService.openFileContent(29L, "2026/a.pdf")).thenReturn(new FileContent(input, 4));
        mvc.perform(get("/infra/file/29/get/2026/a.pdf").param("preview", "true").header("Range", "bytes=1-2"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 1-2/4"))
                .andExpect(content().bytes(new byte[]{2, 3}));
        verify(fileService, never()).getFileContent(anyLong(), anyString());
        verify(input).close();
    }

    @Test
    void ordinaryDownloadKeepsExistingBehavior() throws Exception {
        when(fileService.getFileContent(29L, "a.pdf")).thenReturn(new byte[]{1, 2});
        mvc.perform(get("/infra/file/29/get/a.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment;")))
                .andExpect(content().bytes(new byte[]{1, 2}));
        verify(fileService, never()).openFileContent(anyLong(), anyString());
    }

    @Test
    void missingPreviewReturns404() throws Exception {
        mvc.perform(get("/infra/file/29/get/missing.pdf").param("preview", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    void preparesUsingJsonBodyAndReturnsOnlyAsciiSourcePath() throws Exception {
        String path = "/infra/file/preview/content/0123456789abcdef0123456789abcdef/file_42_version.pdf";
        when(previewService.prepare(any())).thenReturn(new FilePreviewRespVO(path, "file_42_version.pdf", "中文.pdf", 1800));
        mvc.perform(post("/infra/file/preview/prepare").contentType("application/json")
                        .content("{\"fileId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewPath").value(path));
        verify(previewService).prepare(argThat(request -> request.getFileId() == 42L && request.getPath() == null));
    }

    @Test
    void credentialContentSupportsRangeAndOriginalFilename() throws Exception {
        String ticket = "0123456789abcdef0123456789abcdef";
        String cacheName = "file_42_version.pdf";
        FilePreviewService.PreviewFile file = new FilePreviewService.PreviewFile();
        file.setConfigId(29L);
        file.setPath("2026/storage.pdf");
        file.setName("原文件 #1.pdf");
        when(previewService.resolve(ticket, cacheName)).thenReturn(file);
        ByteArrayInputStream input = spy(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        when(fileService.openFileContent(29L, "2026/storage.pdf")).thenReturn(new FileContent(input, 3));
        mvc.perform(get("/infra/file/preview/content/" + ticket + "/" + cacheName).header("Range", "bytes=1-"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(new byte[]{2, 3}));
        verify(input).close();
    }

    @Test
    void expiredCredentialCannotReadStorage() throws Exception {
        mvc.perform(get("/infra/file/preview/content/0123456789abcdef0123456789abcdef/file_42_version.pdf"))
                .andExpect(status().isGone());
        verifyNoInteractions(fileService);
    }
}
