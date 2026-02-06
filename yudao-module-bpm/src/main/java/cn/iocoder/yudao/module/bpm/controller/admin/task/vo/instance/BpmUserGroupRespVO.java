package cn.iocoder.yudao.module.bpm.controller.admin.task.vo.instance;

import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import lombok.Data;

import java.util.List;

@Data
public class BpmUserGroupRespVO {
    private Long id;          // 部门ID
    private String name;      // 部门名称
    private List<UserSimpleRespVO> children;
}
