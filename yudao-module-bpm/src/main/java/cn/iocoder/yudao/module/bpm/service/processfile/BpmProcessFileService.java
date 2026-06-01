package cn.iocoder.yudao.module.bpm.service.processfile;

import cn.iocoder.yudao.module.bpm.dal.dataobject.processfile.BpmProcessFileDO;

public interface BpmProcessFileService {

    /** 保存文件→流程关联 */
    void save(String fileUrl, String processInstanceId);

    /** 根据文件路径查关联 */
    BpmProcessFileDO getByFilePath(String filePath);

    /** 检查用户是否有权限下载该文件 */
    boolean canAccess(Long userId, String filePath);
}
