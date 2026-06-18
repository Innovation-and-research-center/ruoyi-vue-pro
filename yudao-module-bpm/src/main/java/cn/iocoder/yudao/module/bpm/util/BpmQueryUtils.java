package cn.iocoder.yudao.module.bpm.util;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BPM 查询辅助方法。
 */
public class BpmQueryUtils {

    public static List<String> splitKeywords(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        return Arrays.stream(keyword.trim().split("\\s+"))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

}
