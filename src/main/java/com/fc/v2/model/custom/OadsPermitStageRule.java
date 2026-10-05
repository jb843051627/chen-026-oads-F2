package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TOadsPermitSign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 设置许可核准单·三段数法（新一套的唯一出处）。
 *
 * 四问只认这一处给出的答案：
 * 一、每段按什么数法记为通过——各段的 {@link #evaluate(List)}；
 * 二、下一段何时算开放——本段记为通过后，段次只能逐级前进一格；
 * 三、被清掉时抹去哪一段的记录——只清给出不同意那一段的本轮，此前各段一条不动；
 * 四、封存之后还接不接补笔——不接（第三段记为通过即封存，由主单情形把守）。
 *
 * 把第一段的数法用于第二段、把第二段的数法用于第三段，都按错误处理：
 * 三段各自计数、各自评估，互不套用。要新增一种数法，另立一套承接，不回头改这里。
 *
 * @author fuce
 * @date 2026-10-05
 */
public enum OadsPermitStageRule {

    /** 第一段 区政务窗口受理：候 1 人，其意见落单即通过 */
    STAGE1(1, 1, 0, "一名即过"),
    /** 第二段 区局核定：候 2 人，先落单一方的判断即为结论；后落单一方仍需提交，人数照常累加 */
    STAGE2(2, 2, 1, "先落单为论"),
    /** 第三段 市局备案：候 2 人且需同时认可；一条同意一条不同意，整单挂在待议环节 */
    STAGE3(3, 2, 2, "两人同认");

    /** 落笔：同意 */
    public static final int VERDICT_AGREE = 0;
    /** 落笔：不同意 */
    public static final int VERDICT_REJECT = 1;

    /** 段次（1窗口受理 2区局核定 3市局备案） */
    private final int stageNo;
    /** 本段要求签署人数 */
    private final int needCount;
    /** 本段通过规则代号（落在段节行上） */
    private final int passRule;
    /** 数法名 */
    private final String ruleName;

    OadsPermitStageRule(int stageNo, int needCount, int passRule, String ruleName) {
        this.stageNo = stageNo;
        this.needCount = needCount;
        this.passRule = passRule;
        this.ruleName = ruleName;
    }

    /** 一段评估结论：留在本段／记为通过／清除本轮重走／挂在待议 */
    public enum Outcome {
        /** 人数不足，整单留在本段 */
        RUNNING,
        /** 本段记为通过 */
        PASSED,
        /** 本段给出不同意：只清本段本轮已落的签署，被清掉的一段重新往上走 */
        CLEAR_ROUND,
        /** 一条同意一条不同意：整单挂在待议环节，既不推进也不终止 */
        PENDING
    }

    public int stageNo() {
        return stageNo;
    }

    public int needCount() {
        return needCount;
    }

    public int passRule() {
        return passRule;
    }

    public String ruleName() {
        return ruleName;
    }

    /** 最末一段段次 */
    public static int lastStage() {
        return STAGE3.stageNo;
    }

    /** 按段次取数法；段次越界返回 null（那一处不替越界的段次说话） */
    public static OadsPermitStageRule ofStage(Integer stageNo) {
        if (stageNo == null) {
            return null;
        }
        for (OadsPermitStageRule r : values()) {
            if (r.stageNo == stageNo.intValue()) {
                return r;
            }
        }
        return null;
    }

    /**
     * 按本段数法评估本轮。入参为本轮已落的签署（各自时刻各自一笔），
     * 先后以各自落单时刻为准，同一秒各自提交的按落库主键再分先后，不得合并为一条。
     */
    public Outcome evaluate(List<TOadsPermitSign> roundSigns) {
        List<TOadsPermitSign> signs = roundSigns == null
                ? Collections.<TOadsPermitSign>emptyList() : new ArrayList<TOadsPermitSign>(roundSigns);
        if (signs.size() < needCount) {
            return Outcome.RUNNING;
        }
        // 落单先后：先按各自时刻，同一秒再按主键
        Collections.sort(signs, new Comparator<TOadsPermitSign>() {
            @Override
            public int compare(TOadsPermitSign a, TOadsPermitSign b) {
                long at = a.getSignedAt() == null ? 0L : a.getSignedAt().getTime();
                long bt = b.getSignedAt() == null ? 0L : b.getSignedAt().getTime();
                if (at != bt) {
                    return at < bt ? -1 : 1;
                }
                long ai = a.getId() == null ? 0L : a.getId().longValue();
                long bi = b.getId() == null ? 0L : b.getId().longValue();
                return ai < bi ? -1 : (ai == bi ? 0 : 1);
            }
        });
        switch (this) {
            case STAGE1:
                // 候 1 人，其意见落单即通过；给出不同意则清本轮
                return isAgree(signs.get(0)) ? Outcome.PASSED : Outcome.CLEAR_ROUND;
            case STAGE2:
                // 候 2 人，先落单一方的判断即为结论；后落单一方文本照常入库留痕
                return isAgree(signs.get(0)) ? Outcome.PASSED : Outcome.CLEAR_ROUND;
            case STAGE3:
                // 候 2 人且需同时认可
                int agrees = 0;
                for (TOadsPermitSign s : signs) {
                    if (isAgree(s)) {
                        agrees++;
                    }
                }
                if (agrees == needCount) {
                    return Outcome.PASSED;
                }
                // 一条同意一条不同意：挂待议；两条都不同意：按给出不同意清本轮
                return agrees == 1 ? Outcome.PENDING : Outcome.CLEAR_ROUND;
            default:
                return Outcome.RUNNING;
        }
    }

    private static boolean isAgree(TOadsPermitSign s) {
        return s.getVerdict() != null && s.getVerdict() == VERDICT_AGREE;
    }
}
