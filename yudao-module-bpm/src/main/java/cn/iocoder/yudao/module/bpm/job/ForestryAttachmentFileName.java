package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import org.springframework.http.ContentDisposition;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 林业局附件响应头中的文件名解析。
 */
final class ForestryAttachmentFileName {

    private static final Pattern PLAIN_FILENAME = Pattern.compile(
            "(?:^|;)\\s*filename\\s*=\\s*(\"(?:\\\\.|[^\"\\\\])*\"|[^;]*)", Pattern.CASE_INSENSITIVE);

    private ForestryAttachmentFileName() {
    }

    static String resolve(String header, String fallbackName) {
        if (StrUtil.isNotBlank(header)) {
            try {
                // Spring 优先使用 filename*，并按其声明的字符集解码。
                ContentDisposition disposition = parseDisposition(header);
                String name = disposition.getFilename();
                if (StrUtil.isNotBlank(name)) {
                    // 兼容旧接口在普通 filename 中使用百分号编码；保留字面量 +。
                    // filename* 已经解码，不能再次解码，否则会改变含 % 的真实文件名。
                    if (disposition.getCharset() == null) {
                        name = URLUtil.decode(name, StandardCharsets.UTF_8, false);
                    }
                    name = FileUtil.cleanInvalid(name);
                    if (StrUtil.isNotBlank(name)) {
                        return name;
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // 响应头缺失或格式异常时，保留接口提供的文件名，避免丢失附件。
            }
        }
        return FileUtil.cleanInvalid(fallbackName);
    }

    private static ContentDisposition parseDisposition(String header) {
        try {
            return ContentDisposition.parse(header);
        } catch (IllegalArgumentException ex) {
            // 对方 filename* 可能含未编码的中文或括号；单独解析普通 filename。
            Matcher matcher = PLAIN_FILENAME.matcher(header);
            if (matcher.find()) {
                return ContentDisposition.parse("attachment; filename=" + matcher.group(1).trim());
            }
            throw ex;
        }
    }
}
