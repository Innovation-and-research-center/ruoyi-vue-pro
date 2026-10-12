package cn.iocoder.yudao.module.bpm.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForestryAttachmentFileNameTest {

    @Test
    void parsesBothFilenameParametersWithoutIncludingHeaderSuffix() {
        String name = "浙江省林业局关于做好全省林业行政处罚裁量工作的通知(1).pdf";
        assertEquals(name, ForestryAttachmentFileName.resolve(
                "attachment; filename=\"" + name + "\"; filename*=UTF-8''" + name, "默认.pdf"));
    }

    @Test
    void prefersExtendedFilenameInEitherParameterOrder() {
        String extended = "filename*=UTF-8'zh'%E9%80%9A%E7%9F%A5.pdf";
        assertEquals("通知.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename=\"fallback.pdf\"; " + extended, "默认.pdf"));
        assertEquals("通知.pdf", ForestryAttachmentFileName.resolve(
                "attachment; " + extended + "; filename=\"fallback.pdf\"", "默认.pdf"));
        assertEquals("通知.pdf", ForestryAttachmentFileName.resolve(
                "attachment; " + extended, "默认.pdf"));
    }

    @Test
    void handlesPlainAndLegacyEncodedFilenames() {
        assertEquals("通知.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename=\"通知.pdf\"; size=12", "默认.pdf"));
        assertEquals("通知+附件.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename=%E9%80%9A%E7%9F%A5+%E9%99%84%E4%BB%B6.pdf", "默认.pdf"));
        assertEquals("通知;附件.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename=\"通知;附件.pdf\"", "默认.pdf"));
    }

    @Test
    void decodesExtendedFilenameOnceAndRespectsCharset() {
        assertEquals("通知%20+附件.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename*=UTF-8''%E9%80%9A%E7%9F%A5%2520+%E9%99%84%E4%BB%B6.pdf", "默认.pdf"));
        assertEquals("café.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename*=ISO-8859-1''caf%E9.pdf", "默认.pdf"));
    }

    @Test
    void usesPlainFilenameWhenExtendedParameterIsInvalid() {
        assertEquals("通知.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename=\"通知.pdf\"; filename*=UTF-8''%ZZ.pdf", "默认.pdf"));
        assertEquals("通知;附件.pdf", ForestryAttachmentFileName.resolve(
                "attachment; filename=\"通知;附件.pdf\"; filename*=UTF-8''通知.pdf", "默认.pdf"));
    }

    @Test
    void fallsBackWhenFilenameIsMissingEmptyOrHeaderIsMalformed() {
        for (String header : new String[]{null, "", "attachment", "attachment; filename=\"\"",
                "attachment; invalid", "attachment; filename*=UTF-8''%ZZ.pdf",
                "attachment; filename*=unknown''abc.pdf"}) {
            assertEquals("默认.pdf", ForestryAttachmentFileName.resolve(header, "默认.pdf"));
        }
    }
}
