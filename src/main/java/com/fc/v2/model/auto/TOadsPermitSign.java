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
 * 设置许可核准单·签署留痕对象 t_oads_permit_sign（只插不改）
 *
 * @author fuce
 * @date 2026-10-05
 */
@TableName("t_oads_permit_sign")
@ApiModel(value = "TOadsPermitSign", description = "设置许可核准单·签署留痕")
public class TOadsPermitSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核准单号 */
    @TableField("order_no")
    @ApiModelProperty(value = "核准单号")
    private String orderNo;

    /** 落在哪一段 */
    @TableField("stage_no")
    @ApiModelProperty(value = "落在哪一段")
    private Integer stageNo;

    /** 哪一轮（本轮文本与上轮文本分开存放） */
    @TableField("round_no")
    @ApiModelProperty(value = "哪一轮")
    private Integer roundNo;

    /** 签署岗（受理岗/核定岗/备案岗） */
    @TableField("signer")
    @ApiModelProperty(value = "签署岗")
    private String signer;

    /** 落笔 0同意 1不同意 */
    @TableField("verdict")
    @ApiModelProperty(value = "落笔 0同意 1不同意")
    private Integer verdict;

    /** 签署文本（入库留痕，不改一字） */
    @TableField("opinion")
    @ApiModelProperty(value = "签署文本")
    private String opinion;

    /** 落单那一刻（同一秒各自提交各自一笔，不得合并） */
    @TableField("signed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落单那一刻")
    private Date signedAt;

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

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public Integer getVerdict() {
        return verdict;
    }

    public void setVerdict(Integer verdict) {
        this.verdict = verdict;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Date getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Date signedAt) {
        this.signedAt = signedAt;
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
