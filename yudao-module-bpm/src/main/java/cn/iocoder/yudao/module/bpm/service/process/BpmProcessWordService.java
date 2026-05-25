package cn.iocoder.yudao.module.bpm.service.process;

public interface BpmProcessWordService {

    /**
     * 根据流程实例 ID 生成阅办单 Word 文档
     *
     * @param processInstanceId 流程实例编号
     * @return Word 文档字节数组
     */
    byte[] generateWord(String processInstanceId) throws Exception;

    /**
     * 根据流程实例 ID 生成阅办单 HTML（用于前端预览/编辑/打印）
     *
     * @param processInstanceId 流程实例编号
     * @return HTML 字符串
     */
    String generateHtml(String processInstanceId) throws Exception;
}
