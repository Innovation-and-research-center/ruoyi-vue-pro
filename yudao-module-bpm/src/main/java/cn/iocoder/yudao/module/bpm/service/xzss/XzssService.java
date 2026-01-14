package cn.iocoder.yudao.module.bpm.service.xzss;

import java.util.*;
import javax.validation.*;
import cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo.*;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssDO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.xzss.XzssKzDO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;

/**
 * 行政诉讼 Service 接口
 *
 * @author 管理员
 */
public interface XzssService {

    /**
     * 创建行政诉讼
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createXzss(Long userId,@Valid XzssSaveReqVO createReqVO);

    /**
     * 更新行政诉讼
     *
     * @param updateReqVO 更新信息
     */
    void updateXzss(@Valid XzssSaveReqVO updateReqVO);

    /**
     * 删除行政诉讼
     *
     * @param id 编号
     */
    void deleteXzss(Long id);

    /**
    * 批量删除行政诉讼
    *
    * @param ids 编号
    */
    void deleteXzssListByIds(List<Long> ids);

    /**
     * 获得行政诉讼
     *
     * @param id 编号
     * @return 行政诉讼
     */
    XzssDO getXzss(Long id);

    /**
     * 获得行政诉讼分页
     *
     * @param pageReqVO 分页查询
     * @return 行政诉讼分页
     */
    PageResult<XzssDO> getXzssPage(XzssPageReqVO pageReqVO);

    // ==================== 子表（行政诉讼拓展） ====================

    /**
     * 获得行政诉讼拓展
     *
     * @param xmGuid 备用主键
     * @return 行政诉讼拓展
     */
    XzssKzDO getXzssKzByXmGuid(String xmGuid);

}