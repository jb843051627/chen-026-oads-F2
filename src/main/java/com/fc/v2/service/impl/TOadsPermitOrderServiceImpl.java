package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TOadsAcceptBookMapper;
import com.fc.v2.mapper.auto.TOadsPermitOrderMapper;
import com.fc.v2.mapper.auto.TOadsPermitSignMapper;
import com.fc.v2.mapper.auto.TOadsPermitStageMapper;
import com.fc.v2.mapper.auto.TOadsSpotChangeLogMapper;
import com.fc.v2.mapper.auto.TOadsSpotMapper;
import com.fc.v2.model.auto.TOadsAcceptBook;
import com.fc.v2.model.auto.TOadsPermitOrder;
import com.fc.v2.model.auto.TOadsPermitSign;
import com.fc.v2.model.auto.TOadsPermitStage;
import com.fc.v2.model.auto.TOadsSpot;
import com.fc.v2.model.auto.TOadsSpotChangeLog;
import com.fc.v2.model.custom.OadsPermitCount;
import com.fc.v2.model.custom.OadsPermitSignResult;
import com.fc.v2.model.custom.OadsPermitStageRule;
import com.fc.v2.service.ITOadsPermitOrderService;
import com.fc.v2.util.SnowflakeIdWorker;

/**
 * 设置许可核准单 Service业务层处理（新一套：三段签署数法另立承接）。
 *
 * 八条口径的落法：
 * 一、单据只由点位主档的记录生成，同一处点位、同一次变更只对应一张在办单，不留自建入口；
 * 二、每段四项数据落在段节行上，已签人数只由本类按落单先后逐笔累加，任何角色没有改写入口；
 * 三、三段数法各不相同，全在 {@link OadsPermitStageRule} 那一处，互不套用；
 * 四、段次只能逐级前进，跳段落进来的意见不予采信、直接丢弃；
 * 五、清除的粒度是一段的本轮，此前各段一条不动，本轮文本与上轮文本按轮次分存；
 * 六、第三段记为通过即封存，封存后不进一笔、不改一字、不整张撤回；
 * 七、封存与点位主档变更、变更履历出自同一次处理；受理登记簿与主单同一次落库；
 * 八、未封存数量随建单加一、随封存减一，顶部数与逐段累加出自同一次计算，偏差指名。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Service
public class TOadsPermitOrderServiceImpl implements ITOadsPermitOrderService {

    private static final Logger log = LoggerFactory.getLogger(TOadsPermitOrderServiceImpl.class);

    /** 整单情形：在办 */
    private static final int ORDER_RUNNING = 0;
    /** 整单情形：已封存 */
    private static final int ORDER_SEALED = 1;

    /** 段节情形：在签 */
    private static final int STAGE_SIGNING = 0;
    /** 段节情形：已通过 */
    private static final int STAGE_PASSED = 1;
    /** 段节情形：待议（一条同意一条不同意，挂起） */
    private static final int STAGE_PENDING = 2;

    /** 变更情形：挂上去 */
    private static final int KIND_INSTALL = 0;
    /** 变更情形：改位置 */
    private static final int KIND_RELOCATE = 1;
    /** 变更情形：整块拆除 */
    private static final int KIND_REMOVE = 2;

    /** 点位册面情形：已核齐 */
    private static final int SPOT_READY = 1;
    /** 点位册面情形：缺项 */
    private static final int SPOT_LACK = 2;

    /** 单号发号器：与主键雪花各走各的，单号给人看 */
    private static final SnowflakeIdWorker ORDER_NO = new SnowflakeIdWorker(1, 1);

    @javax.annotation.Resource
    private TOadsPermitOrderMapper oadsPermitOrderMapper;
    @javax.annotation.Resource
    private TOadsPermitStageMapper oadsPermitStageMapper;
    @javax.annotation.Resource
    private TOadsPermitSignMapper oadsPermitSignMapper;
    @javax.annotation.Resource
    private TOadsAcceptBookMapper oadsAcceptBookMapper;
    @javax.annotation.Resource
    private TOadsSpotChangeLogMapper oadsSpotChangeLogMapper;
    @javax.annotation.Resource
    private TOadsSpotMapper oadsSpotMapper;

    @Override
    public TOadsPermitOrder selectTOadsPermitOrderById(Long id) {
        return this.oadsPermitOrderMapper.selectById(id);
    }

    @Override
    public TOadsPermitOrder selectByOrderNo(String orderNo) {
        if (isBlank(orderNo)) {
            return null;
        }
        return this.oadsPermitOrderMapper.selectOne(new QueryWrapper<TOadsPermitOrder>()
                .eq("order_no", orderNo).eq("del_flag", 0));
    }

    @Override
    public List<TOadsPermitOrder> selectTOadsPermitOrderList(QueryWrapper<TOadsPermitOrder> queryWrapper) {
        return this.oadsPermitOrderMapper.selectList(queryWrapper);
    }

    /**
     * 第一条：立单只从点位主档来。主单、三段段节、受理登记簿同一次落库，
     * 未封存数量随建单加一（第八条）。当前处于哪一节由服务端轧出，不设可录入字段。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized TOadsPermitOrder openFromSpot(String spotNo, Integer changeKind, String operBy, Date at) {
        if (isBlank(spotNo) || changeKind == null
                || (changeKind != KIND_INSTALL && changeKind != KIND_RELOCATE && changeKind != KIND_REMOVE)
                || at == null) {
            return null;
        }
        // 点位主档上查得到这一行，才许立单；主档之外没有来路
        TOadsSpot spot = oadsSpotMapper.selectOne(new QueryWrapper<TOadsSpot>()
                .eq("spot_no", spotNo).eq("del_flag", 0));
        if (spot == null) {
            return null;
        }
        // 同一处点位、同一次变更只对应一张在办单
        Integer dup = oadsPermitOrderMapper.selectCount(new QueryWrapper<TOadsPermitOrder>()
                .eq("spot_no", spotNo).eq("change_kind", changeKind)
                .eq("status", ORDER_RUNNING).eq("del_flag", 0));
        if (dup != null && dup > 0) {
            return null;
        }

        Date now = at;
        TOadsPermitOrder order = new TOadsPermitOrder();
        order.setOrderNo("XK" + ORDER_NO.nextId());
        order.setSpotNo(spotNo);
        order.setChangeKind(changeKind);
        order.setCurStage(OadsPermitStageRule.STAGE1.stageNo());
        order.setStatus(ORDER_RUNNING);
        order.setDelFlag(0);
        order.setCreateBy(operBy);
        order.setCreateTime(now);
        oadsPermitOrderMapper.insert(order);

        // 单内按段分节：三段的要求人数与通过规则按那一处落死，已签人数从零起只许服务端累加
        for (OadsPermitStageRule rule : OadsPermitStageRule.values()) {
            TOadsPermitStage stage = new TOadsPermitStage();
            stage.setOrderNo(order.getOrderNo());
            stage.setStageNo(rule.stageNo());
            stage.setNeedCount(rule.needCount());
            stage.setPassRule(rule.passRule());
            stage.setSignCount(0);
            stage.setRoundNo(1);
            stage.setStatus(STAGE_SIGNING);
            stage.setDelFlag(0);
            stage.setCreateBy(operBy);
            stage.setCreateTime(now);
            oadsPermitStageMapper.insert(stage);
        }

        // 第七条：受理登记簿与主单同一次落库，事后从第三段回数到第一段逐笔吻合
        TOadsAcceptBook book = new TOadsAcceptBook();
        book.setOrderNo(order.getOrderNo());
        book.setSpotNo(spotNo);
        book.setChangeKind(changeKind);
        book.setOperBy(operBy);
        book.setAcceptedAt(now);
        book.setDelFlag(0);
        book.setCreateBy(operBy);
        book.setCreateTime(now);
        oadsAcceptBookMapper.insert(book);
        return order;
    }

    /**
     * 签署唯一入口。报文里写了第几段入库未必采信，段次与人数一概以服务端点出的结果为凭。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized OadsPermitSignResult sign(String orderNo, Integer stageNo, String signer,
                                                  Integer verdict, String opinion, Date at) {
        if (isBlank(orderNo)) {
            return OadsPermitSignResult.refused(orderNo, null, null, null, OadsPermitSignResult.MSG_NO_SUCH_ORDER);
        }
        TOadsPermitOrder order = selectByOrderNo(orderNo);
        if (order == null) {
            return OadsPermitSignResult.refused(orderNo, null, null, null, OadsPermitSignResult.MSG_NO_SUCH_ORDER);
        }
        // 第六条：走满即封存，封存之后不接补笔
        if (order.getStatus() != null && order.getStatus() == ORDER_SEALED) {
            return OadsPermitSignResult.refused(orderNo, order.getCurStage(), null,
                    order.getStatus(), OadsPermitSignResult.MSG_SEALED_NO_MORE);
        }
        if (isBlank(signer) || verdict == null
                || (verdict != OadsPermitStageRule.VERDICT_AGREE && verdict != OadsPermitStageRule.VERDICT_REJECT)
                || at == null) {
            return OadsPermitSignResult.refused(orderNo, order.getCurStage(), null,
                    order.getStatus(), OadsPermitSignResult.MSG_BAD_PEN);
        }

        // 当前处于哪一节由服务端计算得出：未通过的最小段次
        List<TOadsPermitStage> stages = stagesOf(orderNo);
        TOadsPermitStage current = null;
        for (TOadsPermitStage s : stages) {
            if (s.getStatus() == null || s.getStatus() != STAGE_PASSED) {
                current = s;
                break;
            }
        }
        if (current == null) {
            // 三段全过而整单未封存：视同封存，不再进笔
            return OadsPermitSignResult.refused(orderNo, order.getCurStage(), null,
                    order.getStatus(), OadsPermitSignResult.MSG_SEALED_NO_MORE);
        }
        OadsPermitStageRule rule = OadsPermitStageRule.ofStage(current.getStageNo());
        if (rule == null) {
            return OadsPermitSignResult.refused(orderNo, order.getCurStage(), null,
                    order.getStatus(), OadsPermitSignResult.MSG_DISCARDED);
        }

        // 第四条：段次只能逐级前进。报文段次与服务端算得的不符，这一笔丢弃，整单留在原段
        if (stageNo == null || stageNo.intValue() != current.getStageNo().intValue()) {
            return OadsPermitSignResult.refused(orderNo, current.getStageNo(), current.getSignCount(),
                    order.getStatus(), OadsPermitSignResult.MSG_DISCARDED);
        }
        // 第三条：挂在待议环节的整单，既不推进也不终止，也不再进笔
        if (current.getStatus() != null && current.getStatus() == STAGE_PENDING) {
            return OadsPermitSignResult.refused(orderNo, current.getStageNo(), current.getSignCount(),
                    order.getStatus(), OadsPermitSignResult.MSG_PENDING_NO_MORE);
        }
        int got = current.getSignCount() == null ? 0 : current.getSignCount();
        if (got >= rule.needCount()) {
            return OadsPermitSignResult.refused(orderNo, current.getStageNo(), got,
                    order.getStatus(), OadsPermitSignResult.MSG_DISCARDED);
        }

        // 第五条：同一段同一岗第二次提交，保留首次那句，不覆盖也不追加副本
        List<TOadsPermitSign> roundSigns = signsOfRound(orderNo, current.getStageNo(), current.getRoundNo());
        for (TOadsPermitSign s : roundSigns) {
            if (signer.equals(s.getSigner())) {
                return OadsPermitSignResult.refused(orderNo, current.getStageNo(), got,
                        order.getStatus(), OadsPermitSignResult.MSG_KEPT_FIRST);
            }
        }

        // 第三条：落单各自时刻各自一笔，同一秒各自提交也不得合并为一条
        TOadsPermitSign sign = new TOadsPermitSign();
        sign.setOrderNo(orderNo);
        sign.setStageNo(current.getStageNo());
        sign.setRoundNo(current.getRoundNo());
        sign.setSigner(signer);
        sign.setVerdict(verdict);
        sign.setOpinion(opinion);
        sign.setSignedAt(at);
        sign.setDelFlag(0);
        sign.setCreateBy(signer);
        sign.setCreateTime(at);
        oadsPermitSignMapper.insert(sign);

        // 第二条：已签人数只由服务端按实际落单先后逐笔累加
        got += 1;
        current.setSignCount(got);
        roundSigns.add(sign);

        // 那一处评本段结论
        OadsPermitStageRule.Outcome outcome = rule.evaluate(roundSigns);
        String message;
        switch (outcome) {
            case PASSED:
                current.setStatus(STAGE_PASSED);
                oadsPermitStageMapper.updateById(current);
                if (rule.stageNo() < OadsPermitStageRule.lastStage()) {
                    // 第四条：逐级前进一格，下一段此刻才算开放
                    order.setCurStage(rule.stageNo() + 1);
                    message = OadsPermitSignResult.MSG_STAGE_PASSED;
                } else {
                    // 第六条：走满即封存；第七条：封存、主档变更、变更履历同一次处理
                    order.setStatus(ORDER_SEALED);
                    order.setSealedAt(at);
                    sealSpotChange(order, signer, at);
                    message = OadsPermitSignResult.MSG_SEALED;
                }
                oadsPermitOrderMapper.updateById(order);
                break;
            case CLEAR_ROUND:
                // 第五条：只清本段本轮已落的签署，此前各段一条不动；文本按轮分存，轮次翻一
                current.setSignCount(0);
                current.setRoundNo((current.getRoundNo() == null ? 1 : current.getRoundNo()) + 1);
                oadsPermitStageMapper.updateById(current);
                order.setCurStage(rule.stageNo());
                oadsPermitOrderMapper.updateById(order);
                message = OadsPermitSignResult.MSG_ROUND_CLEARED;
                break;
            case PENDING:
                // 第三条：一条同意一条不同意，整单挂在待议环节，既不推进也不终止
                current.setStatus(STAGE_PENDING);
                oadsPermitStageMapper.updateById(current);
                order.setCurStage(rule.stageNo());
                oadsPermitOrderMapper.updateById(order);
                message = OadsPermitSignResult.MSG_PENDING;
                break;
            case RUNNING:
            default:
                oadsPermitStageMapper.updateById(current);
                order.setCurStage(rule.stageNo());
                oadsPermitOrderMapper.updateById(order);
                message = OadsPermitSignResult.MSG_TAKEN;
                break;
        }
        return OadsPermitSignResult.taken(orderNo, order.getCurStage(),
                outcome == OadsPermitStageRule.Outcome.CLEAR_ROUND ? 0 : got,
                order.getStatus(), message);
    }

    /**
     * 第七条：整单封存时点位主档那一行同步变更，并在点位行上追加一条变更履历——
     * 由谁操作、依据哪张单、从哪一档改到哪一档、发生于哪一时刻。
     * 与封存出自同一次处理，两边数据必须一致，不许一处取处理结果、另一处靠人工补填。
     */
    private void sealSpotChange(TOadsPermitOrder order, String operBy, Date at) {
        TOadsSpot spot = oadsSpotMapper.selectOne(new QueryWrapper<TOadsSpot>()
                .eq("spot_no", order.getSpotNo()).eq("del_flag", 0));
        if (spot == null) {
            // 主档行已不在：无从变更也无从挂履历，记上一笔警告，核准单三段走满的事实不变
            log.warn("核准单[{}]封存时点位[{}]已从名册上注销，主档变更与履历跳过", order.getOrderNo(), order.getSpotNo());
            return;
        }
        Integer from = spot.getStatus();
        Integer to = spotStatusAfter(order.getChangeKind());
        spot.setStatus(to);
        oadsSpotMapper.updateById(spot);

        TOadsSpotChangeLog logRow = new TOadsSpotChangeLog();
        logRow.setSpotNo(order.getSpotNo());
        logRow.setOrderNo(order.getOrderNo());
        logRow.setOperBy(operBy);
        logRow.setFromStatus(from);
        logRow.setToStatus(to);
        logRow.setChangedAt(at);
        logRow.setDelFlag(0);
        logRow.setCreateBy(operBy);
        logRow.setCreateTime(at);
        oadsSpotChangeLogMapper.insert(logRow);
    }

    /** 变更情形落到册面情形：挂上去/改位置核齐，整块拆除缺项 */
    private static Integer spotStatusAfter(Integer changeKind) {
        if (changeKind != null && changeKind == KIND_REMOVE) {
            return SPOT_LACK;
        }
        return SPOT_READY;
    }

    @Override
    public List<TOadsPermitStage> stagesOf(String orderNo) {
        if (isBlank(orderNo)) {
            return Collections.emptyList();
        }
        return oadsPermitStageMapper.selectList(new QueryWrapper<TOadsPermitStage>()
                .eq("order_no", orderNo).eq("del_flag", 0).orderByAsc("stage_no"));
    }

    /** 本段本轮已落的签署（评估与保留首次都用这一把） */
    private List<TOadsPermitSign> signsOfRound(String orderNo, Integer stageNo, Integer roundNo) {
        return oadsPermitSignMapper.selectList(new QueryWrapper<TOadsPermitSign>()
                .eq("order_no", orderNo).eq("stage_no", stageNo).eq("round_no", roundNo)
                .eq("del_flag", 0).orderByAsc("signed_at").orderByAsc("id"));
    }

    @Override
    public List<TOadsPermitSign> signsOf(String orderNo, Integer stageNo) {
        if (isBlank(orderNo)) {
            return Collections.emptyList();
        }
        QueryWrapper<TOadsPermitSign> qw = new QueryWrapper<TOadsPermitSign>()
                .eq("order_no", orderNo).eq("del_flag", 0);
        if (stageNo != null) {
            qw.eq("stage_no", stageNo);
        }
        // 各轮文本都查得到，按各自落单时刻排；同一秒各自一笔，再按主键分先后
        qw.orderByAsc("stage_no").orderByAsc("round_no").orderByAsc("signed_at").orderByAsc("id");
        return oadsPermitSignMapper.selectList(qw);
    }

    @Override
    public List<TOadsAcceptBook> selectAcceptBookList(QueryWrapper<TOadsAcceptBook> queryWrapper) {
        return oadsAcceptBookMapper.selectList(queryWrapper);
    }

    @Override
    public List<TOadsSpotChangeLog> changeLogsOf(String spotNo) {
        if (isBlank(spotNo)) {
            return Collections.emptyList();
        }
        // 第七条：按时间倒序一段往回看
        return oadsSpotChangeLogMapper.selectList(new QueryWrapper<TOadsSpotChangeLog>()
                .eq("spot_no", spotNo).eq("del_flag", 0)
                .orderByDesc("changed_at").orderByDesc("id"));
    }

    /**
     * 第八条：未封存数量随建单加一、随封存减一。同一批查出的在办单，
     * 一头数出页面顶部那个数，一头按当前段次逐段累加——同一次计算；
     * 段次对不上、逐段累加盖不住的单，逐张指名报出。
     */
    @Override
    public OadsPermitCount unsealedCount() {
        List<TOadsPermitOrder> running = oadsPermitOrderMapper.selectList(new QueryWrapper<TOadsPermitOrder>()
                .eq("status", ORDER_RUNNING).eq("del_flag", 0));
        long total = running == null ? 0 : running.size();
        long s1 = 0, s2 = 0, s3 = 0;
        List<String> deviations = new ArrayList<String>();
        if (running != null) {
            for (TOadsPermitOrder o : running) {
                Integer cur = o.getCurStage();
                if (cur == null || OadsPermitStageRule.ofStage(cur) == null) {
                    deviations.add(o.getOrderNo());
                } else if (cur == OadsPermitStageRule.STAGE1.stageNo()) {
                    s1++;
                } else if (cur == OadsPermitStageRule.STAGE2.stageNo()) {
                    s2++;
                } else {
                    s3++;
                }
            }
        }
        return OadsPermitCount.of(total, s1, s2, s3, deviations);
    }

    /**
     * 第七条：当日所立之单与当日受理登记簿逐笔吻合。
     * 从第三段回数到第一段都出自这些单，单独抽取任意一段核查也要自洽；
     * 对不上的单号（只在一边出现的）逐张指名。
     */
    @Override
    public List<String> findUnreconciledOrders(Date day) {
        if (day == null) {
            return Collections.emptyList();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(day);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date dayStart = c.getTime();
        c.add(Calendar.DAY_OF_MONTH, 1);
        Date dayEnd = c.getTime();

        List<TOadsAcceptBook> books = oadsAcceptBookMapper.selectList(new QueryWrapper<TOadsAcceptBook>()
                .ge("accepted_at", dayStart).lt("accepted_at", dayEnd).eq("del_flag", 0));
        List<TOadsPermitOrder> orders = oadsPermitOrderMapper.selectList(new QueryWrapper<TOadsPermitOrder>()
                .ge("create_time", dayStart).lt("create_time", dayEnd).eq("del_flag", 0));

        Set<String> inBook = new LinkedHashSet<String>();
        if (books != null) {
            for (TOadsAcceptBook b : books) {
                inBook.add(b.getOrderNo());
            }
        }
        Set<String> inOrder = new LinkedHashSet<String>();
        if (orders != null) {
            for (TOadsPermitOrder o : orders) {
                inOrder.add(o.getOrderNo());
            }
        }
        List<String> diff = new ArrayList<String>();
        for (String no : inBook) {
            if (!inOrder.contains(no)) {
                diff.add(no);
            }
        }
        for (String no : inOrder) {
            if (!inBook.contains(no)) {
                diff.add(no);
            }
        }
        Collections.sort(diff);
        return diff;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
