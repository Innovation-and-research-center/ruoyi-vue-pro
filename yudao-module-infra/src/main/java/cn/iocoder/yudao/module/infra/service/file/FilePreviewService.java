package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePreviewReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePreviewRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import lombok.Data;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;

/**
 * 登录用户准备预览，阅读器持短期凭证读取。沿用现有文件存储的共享读取规则，
 * 不接受远程代理地址；历史路径只用于选择配置内的存储对象。
 */
@Service
public class FilePreviewService {

    static final String KEY_PREFIX = "infra:file:preview:";
    static final Duration TTL = Duration.ofMinutes(30);
    private static final Pattern DOWNLOAD_PATH = Pattern.compile("/infra/file/(\\d+)/get/(.+)");

    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileConfigService fileConfigService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public FilePreviewRespVO prepare(FilePreviewReqVO request) {
        PreviewFile file;
        if (request.getFileId() != null) {
            // ID 无效时不降级，避免读到其他同名或历史路径文件。
            FileDO record = fileMapper.selectById(request.getFileId());
            if (record == null) {
                throw exception(FILE_NOT_EXISTS);
            }
            file = fromRecord(record);
        } else {
            file = fromLegacyPath(request);
        }
        if (fileConfigService.getFileClient(file.getConfigId()) == null) {
            throw invalidParamException("文件存储配置不可用");
        }
        String ticket = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + ticket, JsonUtils.toJsonString(file), TTL);
        return new FilePreviewRespVO("/infra/file/preview/content/" + ticket + "/" + file.getCacheName(),
                file.getCacheName(), file.getName(), TTL.getSeconds());
    }

    /** 不存在、过期、文件已删除或凭证与文件不匹配时，均拒绝读取。 */
    public PreviewFile resolve(String ticket, String cacheName) {
        if (ticket == null || !ticket.matches("[a-f0-9]{32}")) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(KEY_PREFIX + ticket);
        if (json == null) {
            return null;
        }
        PreviewFile file = JsonUtils.parseObject(json, PreviewFile.class);
        if (file == null || !Objects.equals(file.getCacheName(), cacheName)) {
            return null;
        }
        if (file.getFileId() != null) {
            FileDO current = fileMapper.selectById(file.getFileId());
            if (current == null || !Objects.equals(fromRecord(current).getCacheName(), cacheName)) {
                return null;
            }
        }
        return file;
    }

    private PreviewFile fromRecord(FileDO record) {
        String path = validatePath(record.getPath());
        String name = StrUtil.blankToDefault(record.getName(), basename(path));
        PreviewFile file = new PreviewFile();
        file.setFileId(record.getId());
        file.setConfigId(record.getConfigId());
        file.setPath(path);
        file.setName(name);
        String version = DigestUtil.sha256Hex(record.getConfigId() + "\n" + path + "\n" + record.getUpdateTime());
        file.setCacheName("file_" + record.getId() + "_" + version + "." + extension(name));
        return file;
    }

    private PreviewFile fromLegacyPath(FilePreviewReqVO request) {
        if (StrUtil.isBlank(request.getPath())) {
            throw invalidParamException("请提供文件 ID 或历史附件路径");
        }
        Long configId = request.getConfigId();
        String path = request.getPath();
        if (path.matches("(?i)^https?://.*") || path.startsWith("/admin-api/infra/file/")) {
            final String rawPath;
            try {
                rawPath = URI.create(path.replace(" ", "%20")).getRawPath();
            } catch (IllegalArgumentException ex) {
                throw invalidParamException("历史附件地址格式不正确");
            }
            Matcher matcher = DOWNLOAD_PATH.matcher(rawPath);
            if (!matcher.find()) {
                // 兼容系统已登记的对象存储直链；只查文件表，不向提交的 URL 发 HTTP 请求。
                FileDO record = fileMapper.selectOne(FileDO::getUrl, path);
                if (record != null && (configId == null || configId.equals(record.getConfigId()))) {
                    return fromRecord(record);
                }
                throw invalidParamException("仅支持系统文件地址或存储内相对路径");
            }
            Long urlConfigId = Long.valueOf(matcher.group(1));
            if (configId != null && !configId.equals(urlConfigId)) {
                throw invalidParamException("附件地址与存储配置不匹配");
            }
            configId = urlConfigId;
            // URL 路径仅解码一次；JSON 中直接提交的存储路径保持原样，保留字面量 %、+、#、?。
            path = URLUtil.decode(matcher.group(2), StandardCharsets.UTF_8, false);
        }
        if (configId == null) {
            FileConfigDO master = fileConfigService.getMasterFileConfig();
            if (master == null) {
                throw invalidParamException("未配置历史附件存储");
            }
            configId = master.getId();
        }
        path = validatePath(path);
        PreviewFile file = new PreviewFile();
        file.setConfigId(configId);
        file.setPath(path);
        file.setName(basename(path));
        file.setCacheName("legacy_" + DigestUtil.sha256Hex(configId + "\n" + path) + "." + extension(file.getName()));
        return file;
    }

    private String validatePath(String path) {
        if (StrUtil.isBlank(path) || path.length() > 4096 || path.indexOf(0) >= 0) {
            throw invalidParamException("非法的文件路径");
        }
        path = path.replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.isEmpty() || path.contains(":") || path.endsWith("/")) {
            throw invalidParamException("非法的文件路径");
        }
        for (String segment : path.split("/")) {
            if ("..".equals(segment) || ".".equals(segment)) {
                throw invalidParamException("非法的文件路径");
            }
        }
        return path;
    }

    private String basename(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String extension(String name) {
        String extension = StrUtil.subAfter(name, ".", true).toLowerCase(java.util.Locale.ROOT);
        return extension.matches("[a-z0-9]{1,16}") ? extension : "bin";
    }

    @Data
    public static class PreviewFile {
        private Long fileId;
        private Long configId;
        private String path;
        private String name;
        private String cacheName;
    }
}
