package cn.iocoder.yudao.module.system.controller.admin.meetingroom;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.iocoder.yudao.module.system.controller.admin.meetingroom.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.meetingroom.MeetingRoomDO;
import cn.iocoder.yudao.module.system.service.meetingroom.MeetingRoomService;

@Tag(name = "管理后台 - 会议室")
@RestController
@RequestMapping("/system/meeting-room")
@Validated
public class MeetingRoomController {

    @Resource
    private MeetingRoomService meetingRoomService;

    @PostMapping("/create")
    @Operation(summary = "创建会议室")
    @PreAuthorize("@ss.hasPermission('system:meeting-room:create')")
    public CommonResult<Long> createMeetingRoom(@Valid @RequestBody MeetingRoomSaveReqVO createReqVO) {
        return success(meetingRoomService.createMeetingRoom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会议室")
    @PreAuthorize("@ss.hasPermission('system:meeting-room:update')")
    public CommonResult<Boolean> updateMeetingRoom(@Valid @RequestBody MeetingRoomSaveReqVO updateReqVO) {
        meetingRoomService.updateMeetingRoom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议室")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:meeting-room:delete')")
    public CommonResult<Boolean> deleteMeetingRoom(@RequestParam("id") Long id) {
        meetingRoomService.deleteMeetingRoom(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除会议室")
                @PreAuthorize("@ss.hasPermission('system:meeting-room:delete')")
    public CommonResult<Boolean> deleteMeetingRoomList(@RequestParam("ids") List<Long> ids) {
        meetingRoomService.deleteMeetingRoomListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议室")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('system:meeting-room:query')")
    public CommonResult<MeetingRoomRespVO> getMeetingRoom(@RequestParam("id") Long id) {
        MeetingRoomDO meetingRoom = meetingRoomService.getMeetingRoom(id);
        return success(BeanUtils.toBean(meetingRoom, MeetingRoomRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议室分页")
    @PreAuthorize("@ss.hasPermission('system:meeting-room:query')")
    public CommonResult<PageResult<MeetingRoomRespVO>> getMeetingRoomPage(@Valid MeetingRoomPageReqVO pageReqVO) {
        PageResult<MeetingRoomDO> pageResult = meetingRoomService.getMeetingRoomPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MeetingRoomRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出会议室 Excel")
    @PreAuthorize("@ss.hasPermission('system:meeting-room:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMeetingRoomExcel(@Valid MeetingRoomPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MeetingRoomDO> list = meetingRoomService.getMeetingRoomPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "会议室.xls", "数据", MeetingRoomRespVO.class,
                        BeanUtils.toBean(list, MeetingRoomRespVO.class));
    }

}