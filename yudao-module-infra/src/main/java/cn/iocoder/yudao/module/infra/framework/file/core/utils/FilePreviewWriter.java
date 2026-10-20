package cn.iocoder.yudao.module.infra.framework.file.core.utils;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileContent;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpRange;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.EOFException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 预览响应支持单段 Range；不将文件完整加载进内存。 */
public final class FilePreviewWriter {

    private FilePreviewWriter() {
    }

    public static void write(HttpServletRequest request, HttpServletResponse response,
                             String path, FileContent content) throws Exception {
        long total = content.getLength();
        long start = 0;
        long end = total - 1;
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        String range = request.getHeader("Range");
        if (range != null) {
            try {
                List<HttpRange> ranges = HttpRange.parseRanges(range);
                if (ranges.size() != 1 || total == 0) {
                    throw new IllegalArgumentException("仅支持单段 Range");
                }
                start = ranges.get(0).getRangeStart(total);
                end = ranges.get(0).getRangeEnd(total);
                if (start < 0 || start >= total || end < start) {
                    throw new IllegalArgumentException("Range 超出文件范围");
                }
            } catch (IllegalArgumentException ex) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader("Content-Range", "bytes */" + total);
                return;
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + total);
        }
        String filename = path.substring(path.lastIndexOf('/') + 1);
        String type = FileTypeUtils.getMineType(filename);
        // HTML、SVG 等主动内容不允许以当前应用源直接执行。
        boolean inline = "application/pdf".equals(type)
                || type.matches("image/(jpeg|png|gif|webp|bmp|x-icon)")
                || type.startsWith("audio/") || type.startsWith("video/");
        response.setContentType(type);
        response.setHeader("Content-Disposition", (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(filename, StandardCharsets.UTF_8).build().toString());
        long length = total == 0 ? 0 : end - start + 1;
        response.setContentLengthLong(length);
        if ("HEAD".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        InputStream input = content.getInputStream();
        while (start > 0) {
            long skipped = input.skip(start);
            if (skipped == 0) {
                if (input.read() == -1) {
                    throw new EOFException("文件内容不足");
                }
                skipped = 1;
            }
            start -= skipped;
        }
        byte[] buffer = new byte[16 * 1024];
        while (length > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, length));
            if (read < 0) {
                throw new EOFException("文件内容不足");
            }
            response.getOutputStream().write(buffer, 0, read);
            length -= read;
        }
    }
}
