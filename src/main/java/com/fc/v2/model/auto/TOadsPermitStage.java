package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 设置许可核准单·段节对象 t_oads_permit_stage
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_permit_stage")
@ApiModel(value = "TOadsPermitStage", description = "设置许可核准单·段节")
public class TOadsPermitStage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核准单号（挂主单） */
    @TableField("order_no")
    @ApiModelProperty(value = "核准单号")
    private String orderNo;

    /** 段次 1窗口受理 2区局核定 3市局备案 */
    @TableField("stage_no")
    @ApiModelProperty(value = "段次 1窗口受理 2区局核定 3市局备案")
    private Integer stageNo;

    /** 本段要求签署人数（1/2/2，立单时按数法落死） */
    @TableField("need_count")
    @ApiModelProperty(value = "本段要求签署人数")
    private Integer needCount;

    /** 本段通过规则 0一名即过 1先落单为论 2两人同认 */
    @TableField("pass_rule")
    @ApiModelProperty(value = "本段通过规则 0一名即过 1先落单为论 2两人同认")
    private Integer passRule;

    /** 本轮已完成签署人数（只由服务端按落单先后逐笔累加，前端仅回显） */
    @TableField("sign_count")
    @ApiModelProperty(value = "本轮已完成签署人数")
    private Integer signCount;

    /** 本轮轮次（被清掉重走一轮此数加一，文本按轮分存） */
    @TableField("round_no")
    @ApiModelProperty(value = "本轮轮次")
    private Integer roundNo;

    /** 段节情形 0在签 1已通过 2待议（一同一不同挂起，不推进不终止） */
    @TableField("status")
    @ApiModelProperty(value = "段节情形 0在签 1已通过 2待议")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getStageNo() {
        return stageNo;
    }

    public void setStageNo(Integer stageNo) {
        this.stageNo = stageNo;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getPassRule() {
        return passRule;
    }

    public void setPassRule(Integer passRule) {
        this.passRule = passRule;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
