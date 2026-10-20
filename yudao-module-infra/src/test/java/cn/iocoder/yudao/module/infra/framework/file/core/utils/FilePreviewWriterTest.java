package cn.iocoder.yudao.module.infra.framework.file.core.utils;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileContent;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FilePreviewWriterTest {

    @Test
    void streamsPdfAndEncodesChineseFilename() throws Exception {
        MockHttpServletResponse response = write("GET", null, "中文 #1.pdf", "0123456789");
        assertEquals(200, response.getStatus());
        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").startsWith("inline;"));
        assertTrue(response.getHeader("Content-Disposition").contains("filename*=UTF-8''"));
        assertEquals("0123456789", response.getContentAsString());
        assertEquals(10, response.getContentLengthLong());
    }

    @Test
    void supportsBoundedOpenAndSuffixRanges() throws Exception {
        MockHttpServletResponse bounded = write("GET", "bytes=2-5", "a.pdf", "0123456789");
        assertEquals(206, bounded.getStatus());
        assertEquals("bytes 2-5/10", bounded.getHeader("Content-Range"));
        assertEquals("2345", bounded.getContentAsString());
        assertEquals("789", write("GET", "bytes=-3", "a.pdf", "0123456789").getContentAsString());
        assertEquals("789", write("GET", "bytes=7-", "a.pdf", "0123456789").getContentAsString());
    }

    @Test
    void rejectsInvalidOrMultipleRanges() throws Exception {
        for (String range : new String[]{"bytes=10-", "bytes=5-2", "bytes=0-1,4-5", "invalid"}) {
            MockHttpServletResponse response = write("GET", range, "a.pdf", "0123456789");
            assertEquals(416, response.getStatus(), range);
            assertEquals("bytes */10", response.getHeader("Content-Range"));
            assertEquals(0, response.getContentAsByteArray().length);
        }
        assertEquals(416, write("GET", "bytes=0-", "empty.pdf", "").getStatus());
    }

    @Test
    void headAndEmptyFilesHaveNoBody() throws Exception {
        MockHttpServletResponse head = write("HEAD", null, "a.pdf", "0123456789");
        assertEquals(10, head.getContentLengthLong());
        assertEquals(0, head.getContentAsByteArray().length);
        assertEquals(0, write("GET", null, "empty.pdf", "").getContentLengthLong());
    }

    @Test
    void activeContentIsDownloadedInsteadOfExecuted() throws Exception {
        assertTrue(write("GET", null, "a.html", "<script></script>")
                .getHeader("Content-Disposition").startsWith("attachment;"));
        assertTrue(write("GET", null, "a.svg", "<svg/>")
                .getHeader("Content-Disposition").startsWith("attachment;"));
    }

    private MockHttpServletResponse write(String method, String range, String path, String body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/infra/file/1/get/" + path);
        if (range != null) request.addHeader("Range", range);
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (FileContent content = new FileContent(new ByteArrayInputStream(bytes), bytes.length)) {
            FilePreviewWriter.write(request, response, path, content);
        }
        return response;
    }
}
