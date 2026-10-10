package cn.iocoder.yudao.module.bpm.service.commenttemplate;

import java.util.*;
import java.util.regex.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 旧模板代码/环节均为逗号分隔；占位符支持旧的完整字段名。 */
public final class CommentTemplateSupport {
    private static final Pattern FIELD = Pattern.compile("\\{([^{}]+)\\}");
    private CommentTemplateSupport() {}

    public static Set<String> tokens(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptySet();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean applies(String codes, String acts, String code, String node,
                                  Map<String, String> mapping) {
        boolean matchesCode = tokens(codes).stream().anyMatch(s -> s.equalsIgnoreCase(code));
        Set<String> nodes = tokens(acts);
        return matchesCode && (nodes.isEmpty() || nodes.stream()
                .anyMatch(s -> node.equals(mapping.getOrDefault(s, s))));
    }

    public static String render(String content, Function<String, Object> resolver) {
        Matcher matcher = FIELD.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            Object value = resolver.apply(matcher.group(1));
            String text = value == null || value.toString().trim().isEmpty() ? " " : value.toString().trim();
            matcher.appendReplacement(result, Matcher.quoteReplacement(text));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
