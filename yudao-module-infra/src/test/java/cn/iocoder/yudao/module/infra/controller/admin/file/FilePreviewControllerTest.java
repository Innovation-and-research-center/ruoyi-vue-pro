package cn.iocoder.yudao.module.infra.controller.admin.file;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileContent;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FilePreviewControllerTest {

    private final FileService fileService = mock(FileService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        FileController controller = new FileController();
        ReflectionTestUtils.setField(controller, "fileService", fileService);
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
}
