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
 * 设置许可核准单·主单对象 t_oads_permit_order（新一套：三段签署数法另立承接）
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_permit_order")
@ApiModel(value = "TOadsPermitOrder", description = "设置许可核准单·主单")
public class TOadsPermitOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核准单号（同一处点位、同一次变更只对应一张在办单） */
    @TableField("order_no")
    @ApiModelProperty(value = "核准单号")
    private String orderNo;

    /** 点位码（挂点位底册，单据唯一来源） */
    @TableField("spot_no")
    @ApiModelProperty(value = "点位码")
    private String spotNo;

    /** 变更情形 0挂上去 1改位置 2整块拆除 */
    @TableField("change_kind")
    @ApiModelProperty(value = "变更情形 0挂上去 1改位置 2整块拆除")
    private Integer changeKind;

    /** 整单当前段次 1窗口受理 2区局核定 3市局备案（服务端轧出，不设录入口） */
    @TableField("cur_stage")
    @ApiModelProperty(value = "整单当前段次 1窗口受理 2区局核定 3市局备案")
    private Integer curStage;

    /** 整单情形 0在办 1已封存（封存后不进一笔不改一字） */
    @TableField("status")
    @ApiModelProperty(value = "整单情形 0在办 1已封存")
    private Integer status;

    /** 走满封存那一刻 */
    @TableField("sealed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "走满封存那一刻")
    private Date sealedAt;

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

    public String getSpotNo() {
        return spotNo;
    }

    public void setSpotNo(String spotNo) {
        this.spotNo = spotNo;
    }

    public Integer getChangeKind() {
        return changeKind;
    }

    public void setChangeKind(Integer changeKind) {
        this.changeKind = changeKind;
    }

    public Integer getCurStage() {
        return curStage;
    }

    public void setCurStage(Integer curStage) {
        this.curStage = curStage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(Date sealedAt) {
        this.sealedAt = sealedAt;
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
