package cn.iocoder.yudao.module.bpm.controller.admin.xzss.vo;

import cn.iocoder.yudao.module.bpm.controller.admin.xzfy.vo.XzfyRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 行政诉讼 Response VO")
@Data
@ExcelIgnoreUnannotated
public class XzssRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1284")
    @ExcelProperty("主键")
    private Long id;

    @Schema(description = "历史迁移旧工作流项目号")
    private String projectId;

    @Schema(description = "来文号")
    @ExcelProperty("来文号")
    private String swWh;

    @Schema(description = "来文机关")
    @ExcelProperty("来文机关")
    private String swJg;

    @Schema(description = "收文日期")
    @ExcelProperty("收文日期")
    private LocalDateTime swRq;

    @Schema(description = "原告")
    @ExcelProperty("原告")
    private String sqr;

    @Schema(description = "被告")
    @ExcelProperty("被告")
    private String bsqr;

    @Schema(description = "第三人")
    @ExcelProperty("第三人")
    private String dsr;

    @Schema(description = "土地坐落")
    @ExcelProperty("土地坐落")
    private String tdZl;

    @Schema(description = "诉讼类型：1一审，2二审，3再审，如果多次，继续记录")
    @ExcelProperty("诉讼类型：1一审，2二审，3再审，如果多次，继续记录")
    private Short ssLx;

    @Schema(description = "复议案号")
    @ExcelProperty("复议案号")
    private String fyAh;

    @Schema(description = "上一审案号")
    @ExcelProperty("上一审案号")
    private String ssAh;

    @Schema(description = "类别一")
    @ExcelProperty("类别一")
    private String lb1;

    @Schema(description = "类别二")
    @ExcelProperty("类别二")
    private String lb2;

    @Schema(description = "类别三")
    @ExcelProperty("类别三")
    private String lb3;

    @Schema(description = "类别四")
    @ExcelProperty("类别四")
    private String lb4;

    @Schema(description = "类别五")
    @ExcelProperty("类别五")
    private String lb5;

    @Schema(description = "复议请求")
    @ExcelProperty("复议请求")
    private String ssNr;

    @Schema(description = "承办人")
    @ExcelProperty("承办人")
    private String cbr;

    @Schema(description = "承办日期")
    @ExcelProperty("承办日期")
    private LocalDateTime cbRq;

    @Schema(description = "送法院日期")
    @ExcelProperty("送法院日期")
    private LocalDateTime sfyjgRq;

    @Schema(description = "上诉人")
    @ExcelProperty("上诉人")
    private String ssr;

    @Schema(description = "被上诉人")
    @ExcelProperty("被上诉人")
    private String bssr;

    @Schema(description = "再审申请人")
    @ExcelProperty("再审申请人")
    private String zssqr;

    @Schema(description = "再审被申请人")
    @ExcelProperty("再审被申请人")
    private String zsbsqr;

    @Schema(description = "监督监管")
    @ExcelProperty("监督监管")
    private Short issupervise;

    @Schema(description = "办理时限")
    @ExcelProperty("办理时限")
    private LocalDateTime zhubandate;

    @Schema(description = "诉讼内容")
    @ExcelProperty("诉讼内容")
    private String ssnr;

    @Schema(description = "是否已寄件提醒")
    @ExcelProperty("是否已寄件提醒")
    private Short mailTip;

    @Schema(description = "流程实例的编号", example = "30539")
    @ExcelProperty("流程实例的编号")
    private String processInstanceId;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备用主键")
    @ExcelProperty("备用主键")
    private String xmGuid;


    // === 新增部分开始 ===
    @Schema(description = "关联的行政复议列表")
    private List<XzfyRespVO> xzfyList;

    @Schema(description = "历史诉讼列表")
    private List<XzssRespVO> historyXzssList;

    private Short status;

    private String cancelReason;

}
