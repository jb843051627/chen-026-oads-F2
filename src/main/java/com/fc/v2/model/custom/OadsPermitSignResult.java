package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 设置许可核准单·签署一笔的应答。
 * 屏上看到的段次与已签人数只是回显，一概以服务端落库结果为准；
 * 报文里写了第几段不作凭，采不采信看服务端算得的当前段。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "OadsPermitSignResult", description = "核准单签署一笔的应答")
public class OadsPermitSignResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 应答原话：落单采信，人数照常累加 */
    public static final String MSG_TAKEN = "已落单采信，人数照常累加";
    /** 应答原话：本段记为通过，整单进入下一段 */
    public static final String MSG_STAGE_PASSED = "本段记为通过，整单进入下一段";
    /** 应答原话：三段走满，整单封存 */
    public static final String MSG_SEALED = "三段走满，整单封存";
    /** 应答原话：本段给出不同意，本轮已落的签署清除，重新往上走 */
    public static final String MSG_ROUND_CLEARED = "本段给出不同意，本轮签署已清除，重新往上走";
    /** 应答原话：一条同意一条不同意，整单挂在待议环节 */
    public static final String MSG_PENDING = "一条同意一条不同意，整单挂在待议环节";
    /** 应答原话：段次不符，这一笔已丢弃，整单留在原段 */
    public static final String MSG_DISCARDED = "段次不符，这一笔已丢弃，整单留在原段";
    /** 应答原话：本段保留首次那句，不覆盖也不追加副本 */
    public static final String MSG_KEPT_FIRST = "本段保留首次那句，不覆盖也不追加副本";
    /** 应答原话：整单已封存，不再进一笔 */
    public static final String MSG_SEALED_NO_MORE = "整单已封存，不再进一笔";
    /** 应答原话：整单挂在待议环节，不再进笔 */
    public static final String MSG_PENDING_NO_MORE = "整单挂在待议环节，不再进笔";
    /** 应答原话：查无此单 */
    public static final String MSG_NO_SUCH_ORDER = "查无此单";
    /** 应答原话：签署要件不齐，这一笔未落 */
    public static final String MSG_BAD_PEN = "签署要件不齐，这一笔未落";

    /** 这一笔是否落库采信（丢弃/保留首次/拒笔都为 false） */
    @ApiModelProperty(value = "这一笔是否落库采信")
    private boolean taken;

    /** 核准单号 */
    @ApiModelProperty(value = "核准单号")
    private String orderNo;

    /** 服务端算得的当前段次（回显） */
    @ApiModelProperty(value = "服务端算得的当前段次")
    private Integer curStage;

    /** 本段本轮已签人数（回显，只由服务端逐笔累加） */
    @ApiModelProperty(value = "本段本轮已签人数")
    private Integer signCount;

    /** 整单情形 0在办 1已封存（回显） */
    @ApiModelProperty(value = "整单情形")
    private Integer orderStatus;

    /** 应答原话 */
    @ApiModelProperty(value = "应答原话")
    private String message;

    /** 落库采信：人数照常累加，或带出本段结论 */
    public static OadsPermitSignResult taken(String orderNo, Integer curStage, Integer signCount,
                                             Integer orderStatus, String message) {
        OadsPermitSignResult r = new OadsPermitSignResult();
        r.setTaken(true);
        r.setOrderNo(orderNo);
        r.setCurStage(curStage);
        r.setSignCount(signCount);
        r.setOrderStatus(orderStatus);
        r.setMessage(message);
        return r;
    }

    /** 不予采信：这一笔被丢弃或拒在门外，整单原样 */
    public static OadsPermitSignResult refused(String orderNo, Integer curStage, Integer signCount,
                                               Integer orderStatus, String message) {
        OadsPermitSignResult r = new OadsPermitSignResult();
        r.setTaken(false);
        r.setOrderNo(orderNo);
        r.setCurStage(curStage);
        r.setSignCount(signCount);
        r.setOrderStatus(orderStatus);
        r.setMessage(message);
        return r;
    }

    public boolean isTaken() {
        return taken;
    }

    public void setTaken(boolean taken) {
        this.taken = taken;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Integer getCurStage() {
        return curStage;
    }

    public void setCurStage(Integer curStage) {
        this.curStage = curStage;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
