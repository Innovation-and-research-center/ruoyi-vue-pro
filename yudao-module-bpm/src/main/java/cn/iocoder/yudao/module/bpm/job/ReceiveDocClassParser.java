package cn.iocoder.yudao.module.bpm.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import cn.iocoder.yudao.framework.dict.core.DictFrameworkUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收文文件类别解析。
 */
public final class ReceiveDocClassParser {

    private static final String DOC_CLASS_DICT_TYPE = "doc_class";

    private ReceiveDocClassParser() {
    }

    public static String parse(String title) {
        if (StrUtil.isBlank(title)) {
            return "";
        }

        String normalized = title.replaceAll("\\s+", "")
                .replaceAll("[。；;，,、.!！?？]+$", "");
        String withoutTailBracket = normalized
                .replaceAll("(（[^（）]*）|\\([^()]*\\))+$", "")
                .replaceAll("[。；;，,、.!！?？]+$", "");

        List<DictDataRespDTO> dictList = DictFrameworkUtils.getDictDataList(DOC_CLASS_DICT_TYPE);
        if (CollUtil.isEmpty(dictList)) {
            return "";
        }

        List<String> labels = dictList.stream()
                .map(DictDataRespDTO::getLabel)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());

        for (String label : labels) {
            if (StrUtil.equals(withoutTailBracket, label)) {
                return label;
            }
            if (normalized.endsWith("的" + label)) {
                return label;
            }
            if (normalized.endsWith(label)) {
                return label;
            }
        }
        return "";
    }
}
