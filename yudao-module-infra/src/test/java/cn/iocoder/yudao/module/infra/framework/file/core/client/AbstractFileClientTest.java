package cn.iocoder.yudao.module.infra.framework.file.core.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractFileClientTest {

    @Test
    void testFormatFileUrl_EncodePath() {
        TestFileClient client = new TestFileClient(29L);

        String url = client.formatFileUrl("http://127.0.0.1:48080",
                "20260902/指令单[2026]65号 #1?.pdf");

        assertEquals("http://127.0.0.1:48080/admin-api/infra/file/29/get/"
                + "20260902/%E6%8C%87%E4%BB%A4%E5%8D%95%5B2026%5D65%E5%8F%B7%20%231%3F.pdf", url);
    }

    private static final class TestFileClient extends AbstractFileClient<FileClientConfig> {

        private TestFileClient(Long id) {
            super(id, new FileClientConfig() {
            });
        }

        @Override
        protected void doInit() {
        }

        @Override
        public String upload(byte[] content, String path, String type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getContent(String path) {
            throw new UnsupportedOperationException();
        }
    }
}
