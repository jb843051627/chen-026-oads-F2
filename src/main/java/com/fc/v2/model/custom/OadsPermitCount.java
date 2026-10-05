package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 设置许可核准单·未封存计数（第八条）。
 * 页面顶部那个数与逐段累加的结果基于同一次计算：同一批查出的在办单，
 * 一头数出总数，一头按当前段次分桶。出现偏差时要能指名报出对应单据。
 *
 * @author fuce
 * @date 2026-10-05
 */
@ApiModel(value = "OadsPermitCount", description = "核准单未封存计数")
public class OadsPermitCount implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 页面顶部那个数：未封存总数（随建单加一、随封存减一） */
    @ApiModelProperty(value = "未封存总数")
    private long total;

    /** 逐段累加：留在第一段的在办单 */
    @ApiModelProperty(value = "第一段在办数")
    private long stage1;

    /** 逐段累加：留在第二段的在办单 */
    @ApiModelProperty(value = "第二段在办数")
    private long stage2;

    /** 逐段累加：留在第三段的在办单 */
    @ApiModelProperty(value = "第三段在办数")
    private long stage3;

    /** 偏差单据号：段次对不上、逐段累加盖不住的单，逐张指名 */
    @ApiModelProperty(value = "偏差单据号")
    private List<String> deviations = new ArrayList<String>();

    /** 应答原话 */
    @ApiModelProperty(value = "应答原话")
    private String message;

    /** 同一次计算：总数与逐段数出自同一批在办单 */
    public static OadsPermitCount of(long total, long stage1, long stage2, long stage3, List<String> deviations) {
        OadsPermitCount c = new OadsPermitCount();
        c.setTotal(total);
        c.setStage1(stage1);
        c.setStage2(stage2);
        c.setStage3(stage3);
        c.setDeviations(deviations == null ? new ArrayList<String>() : deviations);
        StringBuilder sb = new StringBuilder();
        sb.append("未封存").append(total).append("张：第一段").append(stage1)
                .append("、第二段").append(stage2).append("、第三段").append(stage3);
        if (!c.getDeviations().isEmpty()) {
            sb.append("；偏差单据：").append(String.join("、", c.getDeviations()));
        }
        c.setMessage(sb.toString());
        return c;
    }

    /** 逐段累加与顶部数是否相合（不相合时偏差单据必已指名） */
    public boolean consistent() {
        return total == stage1 + stage2 + stage3 && deviations.isEmpty();
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getStage1() {
        return stage1;
    }

    public void setStage1(long stage1) {
        this.stage1 = stage1;
    }

    public long getStage2() {
        return stage2;
    }

    public void setStage2(long stage2) {
        this.stage2 = stage2;
    }

    public long getStage3() {
        return stage3;
    }

    public void setStage3(long stage3) {
        this.stage3 = stage3;
    }

    public List<String> getDeviations() {
        return deviations;
    }

    public void setDeviations(List<String> deviations) {
        this.deviations = deviations;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
