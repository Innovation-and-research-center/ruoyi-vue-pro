package cn.iocoder.yudao.module.bpm.service.commenttemplate;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

class CommentTemplateSupportTest {
    @Test void matchesWholeCodeAndWholeNodeTokens() {
        assertTrue(CommentTemplateSupport.applies("DEFAULT,Other", "12,123", "default", "review",
                Collections.singletonMap("12", "review")));
        assertFalse(CommentTemplateSupport.applies("Default", "123", "default", "12", Collections.emptyMap()));
        assertFalse(CommentTemplateSupport.applies("DefaultExtra", "", "default", "review", Collections.emptyMap()));
        assertTrue(CommentTemplateSupport.applies("Default", null, "default", "review", Collections.emptyMap()));
    }
    @Test void resolvesEachOccurrenceAndPreservesReplacementMetacharacters() {
        assertEquals("$5\\正文/$5\\正文/ ", CommentTemplateSupport.render("{title}/{title}/{empty}",
                key -> key.equals("title") ? "$5\\正文" : null));
    }
    @Test void keepsPlainTextAndDoesNotRecursivelyExecuteInsertedPlaceholders() {
        assertEquals("同意", CommentTemplateSupport.render("同意", key -> { throw new AssertionError(); }));
        assertEquals("{other}", CommentTemplateSupport.render("{title}", key -> "{other}"));
    }
}
