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
import com.fc.v2.service.impl.TOadsPermitOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * 设置许可核准单·新一套：八条口径逐条对照（mapper 全 Mock，内存假库，不落库）。
 *
 * 假库按 QueryWrapper 的条件段过滤、按服务里写定的次序排序，
 * 主键自增、create_time 由服务显式落值（与生产填充同刻）。
 */
@ExtendWith(MockitoExtension.class)
public class TOadsPermitOrderServiceImplTest {

    @Mock
    private TOadsPermitOrderMapper oadsPermitOrderMapper;
    @Mock
    private TOadsPermitStageMapper oadsPermitStageMapper;
    @Mock
    private TOadsPermitSignMapper oadsPermitSignMapper;
    @Mock
    private TOadsAcceptBookMapper oadsAcceptBookMapper;
    @Mock
    private TOadsSpotChangeLogMapper oadsSpotChangeLogMapper;
    @Mock
    private TOadsSpotMapper oadsSpotMapper;

    @InjectMocks
    private TOadsPermitOrderServiceImpl service;

    private static final int AGREE = 0;
    private static final int REJECT = 1;

    private Table<TOadsPermitOrder> orders;
    private Table<TOadsPermitStage> stages;
    private Table<TOadsPermitSign> signs;
    private Table<TOadsAcceptBook> books;
    private Table<TOadsSpotChangeLog> logs;
    private Table<TOadsSpot> spots;

    private static Date at(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 一张内存表：自增主键、按条件段过滤、按给定次序排序 */
    private static class Table<T> {
        final Map<Long, T> rows = new LinkedHashMap<Long, T>();
        final AtomicLong seq = new AtomicLong(1000);
        final Function<T, Long> idGet;
        final BiConsumer<T, Long> idSet;
        final Comparator<T> order;

        Table(Function<T, Long> idGet, BiConsumer<T, Long> idSet, Comparator<T> order) {
            this.idGet = idGet;
            this.idSet = idSet;
            this.order = order;
        }

        int insert(T e) {
            if (idGet.apply(e) == null) {
                idSet.accept(e, seq.incrementAndGet());
            }
            rows.put(idGet.apply(e), e);
            return 1;
        }

        int updateById(T e) {
            return e != null && idGet.apply(e) != null && rows.containsKey(idGet.apply(e)) ? 1 : 0;
        }

        T byId(Long id) {
            return rows.get(id);
        }

        List<T> select(QueryWrapper<?> qw) {
            List<T> out = new ArrayList<T>();
            for (T e : rows.values()) {
                if (match(qw, e)) {
                    out.add(e);
                }
            }
            if (order != null) {
                out.sort(order);
            }
            return out;
        }

        int count(QueryWrapper<?> qw) {
            return select(qw).size();
        }
    }

    private static final Pattern COND = Pattern.compile(
            "(\\w+)\\s*(>=|<=|<>|!=|=|>|<)\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}");

    /** 把 QueryWrapper 的条件段落到一行实体上核 */
    private static boolean match(QueryWrapper<?> qw, Object entity) {
        if (qw == null) {
            return true;
        }
        String seg = qw.getSqlSegment();
        Map<String, Object> params = qw.getParamNameValuePairs();
        Matcher m = COND.matcher(seg == null ? "" : seg);
        while (m.find()) {
            Object expect = params.get(m.group(3));
            Object actual = colOf(entity, m.group(1));
            if (!compare(actual, m.group(2), expect)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean compare(Object actual, String op, Object expect) {
        if ("=".equals(op)) {
            return Objects.equals(actual, expect);
        }
        if (actual == null || expect == null) {
            return false;
        }
        int c = ((Comparable) actual).compareTo(expect);
        if (">=".equals(op)) {
            return c >= 0;
        }
        if ("<=".equals(op)) {
            return c <= 0;
        }
        if (">".equals(op)) {
            return c > 0;
        }
        if ("<".equals(op)) {
            return c < 0;
        }
        return !Objects.equals(actual, expect);
    }

    /** 列名到实体字段：只覆盖服务里用到的条件列 */
    private static Object colOf(Object e, String col) {
        if (e instanceof TOadsPermitOrder) {
            TOadsPermitOrder x = (TOadsPermitOrder) e;
            if ("order_no".equals(col)) {
                return x.getOrderNo();
            }
            if ("spot_no".equals(col)) {
                return x.getSpotNo();
            }
            if ("change_kind".equals(col)) {
                return x.getChangeKind();
            }
            if ("status".equals(col)) {
                return x.getStatus();
            }
            if ("del_flag".equals(col)) {
                return x.getDelFlag();
            }
            if ("create_time".equals(col)) {
                return x.getCreateTime();
            }
        } else if (e instanceof TOadsPermitStage) {
            TOadsPermitStage x = (TOadsPermitStage) e;
            if ("order_no".equals(col)) {
                return x.getOrderNo();
            }
            if ("stage_no".equals(col)) {
                return x.getStageNo();
            }
            if ("del_flag".equals(col)) {
                return x.getDelFlag();
            }
        } else if (e instanceof TOadsPermitSign) {
            TOadsPermitSign x = (TOadsPermitSign) e;
            if ("order_no".equals(col)) {
                return x.getOrderNo();
            }
            if ("stage_no".equals(col)) {
                return x.getStageNo();
            }
            if ("round_no".equals(col)) {
                return x.getRoundNo();
            }
            if ("del_flag".equals(col)) {
                return x.getDelFlag();
            }
        } else if (e instanceof TOadsAcceptBook) {
            TOadsAcceptBook x = (TOadsAcceptBook) e;
            if ("accepted_at".equals(col)) {
                return x.getAcceptedAt();
            }
            if ("spot_no".equals(col)) {
                return x.getSpotNo();
            }
            if ("del_flag".equals(col)) {
                return x.getDelFlag();
            }
        } else if (e instanceof TOadsSpotChangeLog) {
            TOadsSpotChangeLog x = (TOadsSpotChangeLog) e;
            if ("spot_no".equals(col)) {
                return x.getSpotNo();
            }
            if ("del_flag".equals(col)) {
                return x.getDelFlag();
            }
        } else if (e instanceof TOadsSpot) {
            TOadsSpot x = (TOadsSpot) e;
            if ("spot_no".equals(col)) {
                return x.getSpotNo();
            }
            if ("del_flag".equals(col)) {
                return x.getDelFlag();
            }
        }
        throw new IllegalArgumentException("假库不认识的列 " + col + " @ " + e.getClass().getSimpleName());
    }

    @BeforeEach
    public void wireFakeDb() {
        orders = new Table<TOadsPermitOrder>(TOadsPermitOrder::getId, TOadsPermitOrder::setId, null);
        stages = new Table<TOadsPermitStage>(TOadsPermitStage::getId, TOadsPermitStage::setId,
                Comparator.comparing(TOadsPermitStage::getStageNo));
        signs = new Table<TOadsPermitSign>(TOadsPermitSign::getId, TOadsPermitSign::setId,
                Comparator.comparing(TOadsPermitSign::getStageNo)
                        .thenComparing(TOadsPermitSign::getRoundNo)
                        .thenComparing(TOadsPermitSign::getSignedAt)
                        .thenComparing(TOadsPermitSign::getId));
        books = new Table<TOadsAcceptBook>(TOadsAcceptBook::getId, TOadsAcceptBook::setId, null);
        logs = new Table<TOadsSpotChangeLog>(TOadsSpotChangeLog::getId, TOadsSpotChangeLog::setId,
                Comparator.comparing(TOadsSpotChangeLog::getChangedAt).reversed()
                        .thenComparing(Comparator.comparing(TOadsSpotChangeLog::getId).reversed()));
        spots = new Table<TOadsSpot>(TOadsSpot::getId, TOadsSpot::setId, null);

        lenient().when(oadsPermitOrderMapper.insert(any())).thenAnswer(i -> orders.insert(i.getArgument(0)));
        lenient().when(oadsPermitOrderMapper.updateById(any())).thenAnswer(i -> orders.updateById(i.getArgument(0)));
        lenient().when(oadsPermitOrderMapper.selectById(any())).thenAnswer(i -> orders.byId(i.getArgument(0)));
        lenient().when(oadsPermitOrderMapper.selectOne(any()))
                .thenAnswer(i -> orders.select(i.getArgument(0)).stream().findFirst().orElse(null));
        lenient().when(oadsPermitOrderMapper.selectList(any())).thenAnswer(i -> orders.select(i.getArgument(0)));
        lenient().when(oadsPermitOrderMapper.selectCount(any())).thenAnswer(i -> orders.count(i.getArgument(0)));

        lenient().when(oadsPermitStageMapper.insert(any())).thenAnswer(i -> stages.insert(i.getArgument(0)));
        lenient().when(oadsPermitStageMapper.updateById(any())).thenAnswer(i -> stages.updateById(i.getArgument(0)));
        lenient().when(oadsPermitStageMapper.selectList(any())).thenAnswer(i -> stages.select(i.getArgument(0)));

        lenient().when(oadsPermitSignMapper.insert(any())).thenAnswer(i -> signs.insert(i.getArgument(0)));
        lenient().when(oadsPermitSignMapper.selectList(any())).thenAnswer(i -> signs.select(i.getArgument(0)));

        lenient().when(oadsAcceptBookMapper.insert(any())).thenAnswer(i -> books.insert(i.getArgument(0)));
        lenient().when(oadsAcceptBookMapper.selectList(any())).thenAnswer(i -> books.select(i.getArgument(0)));

        lenient().when(oadsSpotChangeLogMapper.insert(any())).thenAnswer(i -> logs.insert(i.getArgument(0)));
        lenient().when(oadsSpotChangeLogMapper.selectList(any())).thenAnswer(i -> logs.select(i.getArgument(0)));

        lenient().when(oadsSpotMapper.selectOne(any()))
                .thenAnswer(i -> spots.select(i.getArgument(0)).stream().findFirst().orElse(null));
        lenient().when(oadsSpotMapper.updateById(any())).thenAnswer(i -> spots.updateById(i.getArgument(0)));
    }

    private TOadsSpot seedSpot(String spotNo, int status) {
        TOadsSpot s = new TOadsSpot();
        s.setSpotNo(spotNo);
        s.setStatus(status);
        s.setDelFlag(0);
        spots.insert(s);
        return s;
    }

    private TOadsPermitOrder openOk(String spotNo, int kind, String at) {
        TOadsPermitOrder o = service.openFromSpot(spotNo, kind, "受理岗A", at(at));
        assertNotNull(o, "点位在册就许立单");
        return o;
    }

    private TOadsPermitStage stageOf(String orderNo, int stageNo) {
        for (TOadsPermitStage s : service.stagesOf(orderNo)) {
            if (s.getStageNo() == stageNo) {
                return s;
            }
        }
        fail("缺第" + stageNo + "段的段节行");
        return null;
    }

    private void passStage1(String orderNo) {
        OadsPermitSignResult r = service.sign(orderNo, 1, "受理岗A", AGREE, "窗口收下", at("2026-10-05 10:00:00"));
        assertTrue(r.isTaken());
    }

    private void passStage2(String orderNo) {
        service.sign(orderNo, 2, "核定岗A", AGREE, "区局同意", at("2026-10-05 11:00:00"));
        service.sign(orderNo, 2, "核定岗B", AGREE, "区局复核同意", at("2026-10-05 11:05:00"));
    }

    private void reachStage3(String orderNo) {
        passStage1(orderNo);
        passStage2(orderNo);
    }

    // 第1、2、7、8条：立单只从主档来，三段段节一次落齐，登记簿同一次落库，未封存加一
    @Test
    public void openCreatesOrderStagesAndBookInOneGo() {
        seedSpot("SP-LJ-01", 0);
        TOadsPermitOrder o = openOk("SP-LJ-01", 0, "2026-10-05 09:00:00");

        assertEquals(Integer.valueOf(1), o.getCurStage(), "新单当前段次由服务端轧出：第一段");
        assertEquals(Integer.valueOf(0), o.getStatus(), "新单在办");

        List<TOadsPermitStage> ss = service.stagesOf(o.getOrderNo());
        assertEquals(3, ss.size(), "单内按段分节：三段");
        assertEquals(Integer.valueOf(1), ss.get(0).getNeedCount(), "第一段候 1 人");
        assertEquals(Integer.valueOf(2), ss.get(1).getNeedCount(), "第二段候 2 人");
        assertEquals(Integer.valueOf(2), ss.get(2).getNeedCount(), "第三段候 2 人");
        assertEquals(Integer.valueOf(0), ss.get(0).getPassRule(), "第一段一名即过");
        assertEquals(Integer.valueOf(1), ss.get(1).getPassRule(), "第二段先落单为论");
        assertEquals(Integer.valueOf(2), ss.get(2).getPassRule(), "第三段两人同认");
        for (TOadsPermitStage s : ss) {
            assertEquals(Integer.valueOf(0), s.getSignCount(), "已签人数从零起，只许服务端累加");
            assertEquals(Integer.valueOf(1), s.getRoundNo(), "首轮");
        }

        List<TOadsAcceptBook> bs = books.select(null);
        assertEquals(1, bs.size(), "受理登记簿与主单同一次落库");
        assertEquals(o.getOrderNo(), bs.get(0).getOrderNo());
        assertEquals(at("2026-10-05 09:00:00"), bs.get(0).getAcceptedAt());

        OadsPermitCount c = service.unsealedCount();
        assertEquals(1, c.getTotal(), "未封存数量随建单加一");
        assertEquals(1, c.getStage1());
        assertTrue(c.consistent());
        assertTrue(service.findUnreconciledOrders(at("2026-10-05 00:00:00")).isEmpty(), "当日登记与所立之单逐笔吻合");
    }

    // 第1条：主档之外没有来路；同一处点位、同一次变更只对应一张
    @Test
    public void openRejectedWithoutSpotOrDuplicateChange() {
        assertNull(service.openFromSpot("SP-GHOST", 0, "受理岗A", at("2026-10-05 09:00:00")), "点位不在册不许立单");
        seedSpot("SP-LJ-02", 0);
        assertNotNull(service.openFromSpot("SP-LJ-02", 0, "受理岗A", at("2026-10-05 09:00:00")));
        assertNull(service.openFromSpot("SP-LJ-02", 0, "受理岗A", at("2026-10-05 09:10:00")), "同一点位同一变更已有在办单");
        assertNotNull(service.openFromSpot("SP-LJ-02", 2, "受理岗A", at("2026-10-05 09:20:00")), "另一项变更另是一张单");
        assertNull(service.openFromSpot("SP-LJ-02", 9, "受理岗A", at("2026-10-05 09:30:00")), "变更情形越界不许立单");
        OadsPermitCount c = service.unsealedCount();
        assertEquals(2, c.getTotal(), "立成两张，未封存为二");
    }

    // 第3条：第一段候 1 人，其意见落单即通过；第4条：逐级前进一格
    @Test
    public void stage1SingleSignPassesAndAdvances() {
        seedSpot("SP-LJ-03", 0);
        TOadsPermitOrder o = openOk("SP-LJ-03", 0, "2026-10-05 09:00:00");
        OadsPermitSignResult r = service.sign(o.getOrderNo(), 1, "受理岗A", AGREE, "窗口收下", at("2026-10-05 10:00:00"));
        assertTrue(r.isTaken());
        assertEquals(OadsPermitSignResult.MSG_STAGE_PASSED, r.getMessage());
        assertEquals(Integer.valueOf(2), r.getCurStage(), "第一段记为通过，整单进入第二段");
        assertEquals(Integer.valueOf(1), stageOf(o.getOrderNo(), 1).getStatus(), "第一段段节记为已通过");
        assertEquals(Integer.valueOf(1), stageOf(o.getOrderNo(), 1).getSignCount(), "已累加人数等于要求人数");
    }

    // 第3、5条：第一段给出不同意，只清本段本轮，文本按轮分存
    @Test
    public void stage1RejectClearsOnlyOwnRound() {
        seedSpot("SP-LJ-04", 0);
        TOadsPermitOrder o = openOk("SP-LJ-04", 0, "2026-10-05 09:00:00");
        OadsPermitSignResult r = service.sign(o.getOrderNo(), 1, "受理岗A", REJECT, "要件不齐", at("2026-10-05 10:00:00"));
        assertTrue(r.isTaken());
        assertEquals(OadsPermitSignResult.MSG_ROUND_CLEARED, r.getMessage());
        TOadsPermitStage s1 = stageOf(o.getOrderNo(), 1);
        assertEquals(Integer.valueOf(2), s1.getRoundNo(), "被清掉的一段重新往上走：轮次翻一");
        assertEquals(Integer.valueOf(0), s1.getSignCount(), "本轮已签清零");
        assertEquals(Integer.valueOf(1), service.selectByOrderNo(o.getOrderNo()).getCurStage(), "整单仍留在第一段");

        List<TOadsPermitSign> history = service.signsOf(o.getOrderNo(), 1);
        assertEquals(1, history.size(), "上轮文本继续可查");
        assertEquals(Integer.valueOf(1), history.get(0).getRoundNo(), "本轮文本与上轮文本分开存放");
        assertEquals("要件不齐", history.get(0).getOpinion());

        OadsPermitSignResult r2 = service.sign(o.getOrderNo(), 1, "受理岗B", AGREE, "补齐后收下", at("2026-10-05 10:30:00"));
        assertEquals(OadsPermitSignResult.MSG_STAGE_PASSED, r2.getMessage(), "新一轮补齐各自有据");
        assertEquals(2, service.signsOf(o.getOrderNo(), 1).size(), "两轮文本都在");
    }

    // 第3条：第二段先落单为论——先同意，后不同意，照样记为通过；双方文本都入库
    @Test
    public void stage2FirstAgreeDecidesDespiteSecondReject() {
        seedSpot("SP-LJ-05", 0);
        TOadsPermitOrder o = openOk("SP-LJ-05", 0, "2026-10-05 09:00:00");
        passStage1(o.getOrderNo());

        OadsPermitSignResult r1 = service.sign(o.getOrderNo(), 2, "核定岗A", AGREE, "区局同意", at("2026-10-05 11:00:00"));
        assertEquals(OadsPermitSignResult.MSG_TAKEN, r1.getMessage(), "人数不足，整单留在本段");
        assertEquals(Integer.valueOf(1), stageOf(o.getOrderNo(), 2).getSignCount(), "人数照常累加");

        OadsPermitSignResult r2 = service.sign(o.getOrderNo(), 2, "核定岗B", REJECT, "复核有保留", at("2026-10-05 11:05:00"));
        assertEquals(OadsPermitSignResult.MSG_STAGE_PASSED, r2.getMessage(), "先落单一方的判断即为结论");
        assertEquals(Integer.valueOf(3), service.selectByOrderNo(o.getOrderNo()).getCurStage());

        List<TOadsPermitSign> ss = service.signsOf(o.getOrderNo(), 2);
        assertEquals(2, ss.size(), "双方文本都入库留痕，前端两行都展示");
        assertEquals("区局同意", ss.get(0).getOpinion());
        assertEquals("复核有保留", ss.get(1).getOpinion());
    }

    // 第3、5条：第二段先落单不同意——后落单一方仍需提交，满两笔后清本轮
    @Test
    public void stage2FirstRejectClearsAfterSecondSigns() {
        seedSpot("SP-LJ-06", 0);
        TOadsPermitOrder o = openOk("SP-LJ-06", 0, "2026-10-05 09:00:00");
        passStage1(o.getOrderNo());

        service.sign(o.getOrderNo(), 2, "核定岗A", REJECT, "区局不同意", at("2026-10-05 11:00:00"));
        assertEquals(Integer.valueOf(1), stageOf(o.getOrderNo(), 2).getSignCount(), "结论已定仍候第二人落笔");

        OadsPermitSignResult r2 = service.sign(o.getOrderNo(), 2, "核定岗B", AGREE, "复核同意", at("2026-10-05 11:05:00"));
        assertEquals(OadsPermitSignResult.MSG_ROUND_CLEARED, r2.getMessage());
        TOadsPermitStage s2 = stageOf(o.getOrderNo(), 2);
        assertEquals(Integer.valueOf(2), s2.getRoundNo(), "本轮清除，重新往上走");
        assertEquals(Integer.valueOf(0), s2.getSignCount());
        assertEquals(Integer.valueOf(2), service.selectByOrderNo(o.getOrderNo()).getCurStage(), "整单仍留在第二段");

        assertEquals(Integer.valueOf(1), stageOf(o.getOrderNo(), 1).getStatus(), "此前各段留下的记录一条不动");
        List<TOadsPermitSign> ss = service.signsOf(o.getOrderNo(), 2);
        assertEquals(2, ss.size(), "被清一轮的两笔文本继续可查");
        assertEquals(Integer.valueOf(1), ss.get(0).getRoundNo());
        assertEquals(Integer.valueOf(1), ss.get(1).getRoundNo());
    }

    // 第3、6、7、8条：第三段两条都同意才通过——走满即封存，主档与履历同一次处理，未封存减一
    @Test
    public void stage3BothAgreeSealsAndSyncsSpot() {
        TOadsSpot spot = seedSpot("SP-LJ-07", 0);
        TOadsPermitOrder o = openOk("SP-LJ-07", 0, "2026-10-05 09:00:00");
        reachStage3(o.getOrderNo());
        assertEquals(1, service.unsealedCount().getTotal());

        OadsPermitSignResult r1 = service.sign(o.getOrderNo(), 3, "备案岗A", AGREE, "市局收悉", at("2026-10-05 12:00:00"));
        assertEquals(OadsPermitSignResult.MSG_TAKEN, r1.getMessage(), "候第二人");
        OadsPermitSignResult r2 = service.sign(o.getOrderNo(), 3, "备案岗B", AGREE, "市局备案", at("2026-10-05 12:05:00"));
        assertEquals(OadsPermitSignResult.MSG_SEALED, r2.getMessage(), "两条都是同意才通过");

        TOadsPermitOrder sealed = service.selectByOrderNo(o.getOrderNo());
        assertEquals(Integer.valueOf(1), sealed.getStatus(), "走满即封存");
        assertEquals(at("2026-10-05 12:05:00"), sealed.getSealedAt());

        assertEquals(Integer.valueOf(1), spot.getStatus(), "主档那一行同步变更：挂上去后核齐");
        List<TOadsSpotChangeLog> ls = service.changeLogsOf("SP-LJ-07");
        assertEquals(1, ls.size(), "点位行上追加一条变更履历");
        TOadsSpotChangeLog lg = ls.get(0);
        assertEquals("备案岗B", lg.getOperBy(), "由谁操作");
        assertEquals(o.getOrderNo(), lg.getOrderNo(), "依据哪张单");
        assertEquals(Integer.valueOf(0), lg.getFromStatus(), "从哪一档");
        assertEquals(Integer.valueOf(1), lg.getToStatus(), "改到哪一档");
        assertEquals(at("2026-10-05 12:05:00"), lg.getChangedAt(), "发生于哪一时刻");
        assertEquals(spot.getStatus(), lg.getToStatus(), "履历与主档出自同一次处理，两边一致");

        assertEquals(0, service.unsealedCount().getTotal(), "未封存数量随封存减一");
    }

    // 第3条：第三段一条同意一条不同意，整单挂在待议环节，既不推进也不终止
    @Test
    public void stage3SplitHangsPending() {
        seedSpot("SP-LJ-08", 0);
        TOadsPermitOrder o = openOk("SP-LJ-08", 0, "2026-10-05 09:00:00");
        reachStage3(o.getOrderNo());

        service.sign(o.getOrderNo(), 3, "备案岗A", AGREE, "市局收悉", at("2026-10-05 12:00:00"));
        OadsPermitSignResult r = service.sign(o.getOrderNo(), 3, "备案岗B", REJECT, "市局有保留", at("2026-10-05 12:05:00"));
        assertEquals(OadsPermitSignResult.MSG_PENDING, r.getMessage());
        assertEquals(Integer.valueOf(2), stageOf(o.getOrderNo(), 3).getStatus(), "段节挂在待议");
        TOadsPermitOrder cur = service.selectByOrderNo(o.getOrderNo());
        assertEquals(Integer.valueOf(0), cur.getStatus(), "不终止：整单仍在办");
        assertEquals(Integer.valueOf(3), cur.getCurStage(), "不推进：整单仍留在第三段");

        OadsPermitSignResult again = service.sign(o.getOrderNo(), 3, "备案岗C", AGREE, "补笔", at("2026-10-05 12:10:00"));
        assertFalse(again.isTaken());
        assertEquals(OadsPermitSignResult.MSG_PENDING_NO_MORE, again.getMessage(), "待议环节不再进笔");
        assertEquals(2, service.signsOf(o.getOrderNo(), 3).size(), "两条文本都留痕，不进第三笔");
    }

    // 第3、5条：第三段两条都不同意，按给出不同意清本轮
    @Test
    public void stage3DoubleRejectClearsRound() {
        seedSpot("SP-LJ-09", 0);
        TOadsPermitOrder o = openOk("SP-LJ-09", 0, "2026-10-05 09:00:00");
        reachStage3(o.getOrderNo());
        service.sign(o.getOrderNo(), 3, "备案岗A", REJECT, "市局不同意", at("2026-10-05 12:00:00"));
        OadsPermitSignResult r = service.sign(o.getOrderNo(), 3, "备案岗B", REJECT, "市局复核不同意", at("2026-10-05 12:05:00"));
        assertEquals(OadsPermitSignResult.MSG_ROUND_CLEARED, r.getMessage());
        assertEquals(Integer.valueOf(2), stageOf(o.getOrderNo(), 3).getRoundNo());
        assertEquals(Integer.valueOf(0), stageOf(o.getOrderNo(), 3).getSignCount());
        assertEquals(Integer.valueOf(0), service.selectByOrderNo(o.getOrderNo()).getStatus(), "清除不是终止，整单仍在办");
    }

    // 第4条：上一段未记为通过，下一段落进来的意见不予采信
    @Test
    public void jumpStageSubmissionIsDiscarded() {
        seedSpot("SP-LJ-10", 0);
        TOadsPermitOrder o = openOk("SP-LJ-10", 0, "2026-10-05 09:00:00");

        OadsPermitSignResult r0 = service.sign(o.getOrderNo(), 3, "备案岗A", AGREE, "跳段直报", at("2026-10-05 09:30:00"));
        assertFalse(r0.isTaken());
        assertEquals(OadsPermitSignResult.MSG_DISCARDED, r0.getMessage(), "报文里写了第几段，入库未必采信");
        assertEquals(0, signs.rows.size(), "跳段那一笔被丢弃，一行都不落");

        passStage1(o.getOrderNo());
        OadsPermitSignResult r1 = service.sign(o.getOrderNo(), 3, "备案岗A", AGREE, "跳段直报", at("2026-10-05 10:30:00"));
        assertFalse(r1.isTaken());
        assertEquals(OadsPermitSignResult.MSG_DISCARDED, r1.getMessage());
        assertEquals(Integer.valueOf(2), service.selectByOrderNo(o.getOrderNo()).getCurStage(), "整单仍留在第二段");
        assertEquals(0, service.signsOf(o.getOrderNo(), 3).size(), "第三段一笔未落");

        OadsPermitSignResult r2 = service.sign(o.getOrderNo(), 1, "受理岗B", AGREE, "回头补一段", at("2026-10-05 10:40:00"));
        assertFalse(r2.isTaken(), "已过的段也不再进笔");
        assertEquals(OadsPermitSignResult.MSG_DISCARDED, r2.getMessage());
    }

    // 第5条：同一段同一岗第二次提交，保留首次那句，不覆盖也不追加副本
    @Test
    public void sameSignerTwiceKeepsFirst() {
        seedSpot("SP-LJ-11", 0);
        TOadsPermitOrder o = openOk("SP-LJ-11", 0, "2026-10-05 09:00:00");
        passStage1(o.getOrderNo());

        service.sign(o.getOrderNo(), 2, "核定岗A", AGREE, "区局同意", at("2026-10-05 11:00:00"));
        OadsPermitSignResult dup = service.sign(o.getOrderNo(), 2, "核定岗A", REJECT, "改口不同意", at("2026-10-05 11:02:00"));
        assertFalse(dup.isTaken());
        assertEquals(OadsPermitSignResult.MSG_KEPT_FIRST, dup.getMessage());

        List<TOadsPermitSign> ss = service.signsOf(o.getOrderNo(), 2);
        assertEquals(1, ss.size(), "不追加副本");
        assertEquals("区局同意", ss.get(0).getOpinion(), "保留首次那句，不覆盖");
        assertEquals(Integer.valueOf(AGREE), ss.get(0).getVerdict());

        OadsPermitSignResult r = service.sign(o.getOrderNo(), 2, "核定岗B", REJECT, "复核不同意", at("2026-10-05 11:05:00"));
        assertEquals(OadsPermitSignResult.MSG_STAGE_PASSED, r.getMessage(), "结论仍按首次落笔的那句算");
    }

    // 第6条：封存之后不进一笔、不改一字
    @Test
    public void sealedOrderRejectsEverything() {
        seedSpot("SP-LJ-12", 0);
        TOadsPermitOrder o = openOk("SP-LJ-12", 0, "2026-10-05 09:00:00");
        reachStage3(o.getOrderNo());
        service.sign(o.getOrderNo(), 3, "备案岗A", AGREE, "市局收悉", at("2026-10-05 12:00:00"));
        service.sign(o.getOrderNo(), 3, "备案岗B", AGREE, "市局备案", at("2026-10-05 12:05:00"));
        int signRows = signs.rows.size();

        OadsPermitSignResult r = service.sign(o.getOrderNo(), 3, "备案岗C", AGREE, "追补签署", at("2026-10-05 13:00:00"));
        assertFalse(r.isTaken());
        assertEquals(OadsPermitSignResult.MSG_SEALED_NO_MORE, r.getMessage(), "封存之后不接补笔");
        assertEquals(signRows, signs.rows.size(), "留痕一行未增");
        assertEquals(1, logs.rows.size(), "履历一条，不再补");
    }

    // 第3条：同一秒各自提交，按各自时刻分别记账，不得合并为一条
    @Test
    public void sameSecondSubmissionsAreTwoRows() {
        seedSpot("SP-LJ-13", 0);
        TOadsPermitOrder o = openOk("SP-LJ-13", 0, "2026-10-05 09:00:00");
        passStage1(o.getOrderNo());
        Date same = at("2026-10-05 11:00:00");
        service.sign(o.getOrderNo(), 2, "核定岗A", AGREE, "甲笔", same);
        service.sign(o.getOrderNo(), 2, "核定岗B", REJECT, "乙笔", same);

        List<TOadsPermitSign> ss = service.signsOf(o.getOrderNo(), 2);
        assertEquals(2, ss.size(), "同一秒也是两笔，不得合并");
        assertNotEquals(ss.get(0).getId(), ss.get(1).getId());
        assertEquals(same, ss.get(0).getSignedAt(), "各自时刻各自记账");
        assertEquals(same, ss.get(1).getSignedAt());
        assertEquals("甲笔", ss.get(0).getOpinion(), "先落库的排前，事后核对以各自落单时刻为准");
        assertEquals(Integer.valueOf(3), service.selectByOrderNo(o.getOrderNo()).getCurStage(), "先落单为论：甲笔同意即过");
    }

    // 第8条：顶部数与逐段累加同一次计算；段次对不上的单指名报出
    @Test
    public void unsealedCountNamesDeviatingOrders() {
        seedSpot("SP-LJ-14", 0);
        TOadsPermitOrder a = openOk("SP-LJ-14", 0, "2026-10-05 09:00:00");
        seedSpot("SP-LJ-15", 0);
        TOadsPermitOrder b = openOk("SP-LJ-15", 1, "2026-10-05 09:05:00");
        seedSpot("SP-LJ-16", 0);
        openOk("SP-LJ-16", 2, "2026-10-05 09:10:00");
        passStage1(b.getOrderNo());

        OadsPermitCount c = service.unsealedCount();
        assertEquals(3, c.getTotal());
        assertEquals(2, c.getStage1());
        assertEquals(1, c.getStage2());
        assertEquals(0, c.getStage3());
        assertTrue(c.consistent(), "顶部数与逐段累加出自同一次计算");

        TOadsPermitOrder ghost = new TOadsPermitOrder();
        ghost.setOrderNo("XK-GHOST");
        ghost.setSpotNo("SP-LJ-14");
        ghost.setChangeKind(0);
        ghost.setCurStage(9);
        ghost.setStatus(0);
        ghost.setDelFlag(0);
        orders.insert(ghost);

        OadsPermitCount c2 = service.unsealedCount();
        assertEquals(4, c2.getTotal());
        assertFalse(c2.consistent(), "逐段累加盖不住的总数要能发现");
        assertEquals(1, c2.getDeviations().size());
        assertEquals("XK-GHOST", c2.getDeviations().get(0), "出现偏差时要能指名报出对应单据");
        assertTrue(c2.getMessage().contains("XK-GHOST"));
    }

    // 第7条：履历按时间倒序一段往回看；整块拆除落到缺项
    @Test
    public void changeLogsReverseChronological() {
        TOadsSpot spot = seedSpot("SP-LJ-17", 0);
        TOadsPermitOrder o1 = openOk("SP-LJ-17", 0, "2026-10-05 09:00:00");
        reachStage3(o1.getOrderNo());
        service.sign(o1.getOrderNo(), 3, "备案岗A", AGREE, "收悉", at("2026-10-05 12:00:00"));
        service.sign(o1.getOrderNo(), 3, "备案岗B", AGREE, "备案", at("2026-10-05 12:05:00"));
        assertEquals(Integer.valueOf(1), spot.getStatus(), "挂上去封存后核齐");

        TOadsPermitOrder o2 = openOk("SP-LJ-17", 2, "2026-10-06 09:00:00");
        service.sign(o2.getOrderNo(), 1, "受理岗A", AGREE, "收下", at("2026-10-06 10:00:00"));
        service.sign(o2.getOrderNo(), 2, "核定岗A", AGREE, "同意", at("2026-10-06 11:00:00"));
        service.sign(o2.getOrderNo(), 2, "核定岗B", AGREE, "同意", at("2026-10-06 11:05:00"));
        service.sign(o2.getOrderNo(), 3, "备案岗A", AGREE, "收悉", at("2026-10-06 12:00:00"));
        service.sign(o2.getOrderNo(), 3, "备案岗B", AGREE, "备案", at("2026-10-06 12:05:00"));
        assertEquals(Integer.valueOf(2), spot.getStatus(), "整块拆除封存后缺项");

        List<TOadsSpotChangeLog> ls = service.changeLogsOf("SP-LJ-17");
        assertEquals(2, ls.size());
        assertEquals(o2.getOrderNo(), ls.get(0).getOrderNo(), "按时间倒序一段往回看");
        assertEquals(Integer.valueOf(1), ls.get(0).getFromStatus());
        assertEquals(Integer.valueOf(2), ls.get(0).getToStatus());
        assertEquals(o1.getOrderNo(), ls.get(1).getOrderNo());
        assertEquals(Integer.valueOf(0), ls.get(1).getFromStatus());
        assertEquals(Integer.valueOf(1), ls.get(1).getToStatus());
    }

    // 第7条：当日受理登记簿与所立之单逐笔吻合，对不上的指名
    @Test
    public void reconcileNamesUnmatchedOrders() {
        seedSpot("SP-LJ-18", 0);
        openOk("SP-LJ-18", 0, "2026-10-05 09:00:00");
        assertTrue(service.findUnreconciledOrders(at("2026-10-05 00:00:00")).isEmpty(), "立单即登记，天然吻合");

        TOadsAcceptBook stray = new TOadsAcceptBook();
        stray.setOrderNo("XK-STRAY");
        stray.setSpotNo("SP-LJ-18");
        stray.setChangeKind(0);
        stray.setAcceptedAt(at("2026-10-05 15:00:00"));
        stray.setDelFlag(0);
        books.insert(stray);

        List<String> diff = service.findUnreconciledOrders(at("2026-10-05 00:00:00"));
        assertEquals(1, diff.size());
        assertEquals("XK-STRAY", diff.get(0), "只在登记簿里出现的单号被指名");
        assertTrue(service.findUnreconciledOrders(at("2026-10-06 00:00:00")).isEmpty(), "别的日子不牵扯");
    }

    // 第2条与末段：签署要件不齐不落笔；屏上人数只是回显
    @Test
    public void malformedSignIsRefused() {
        seedSpot("SP-LJ-19", 0);
        TOadsPermitOrder o = openOk("SP-LJ-19", 0, "2026-10-05 09:00:00");
        assertFalse(service.sign(o.getOrderNo(), 1, " ", AGREE, "没署名", at("2026-10-05 10:00:00")).isTaken(), "签署岗空着不落");
        assertFalse(service.sign(o.getOrderNo(), 1, "受理岗A", null, "没落笔", at("2026-10-05 10:00:00")).isTaken(), "落笔空着不落");
        assertFalse(service.sign(o.getOrderNo(), 1, "受理岗A", 7, "乱写", at("2026-10-05 10:00:00")).isTaken(), "落笔越界不落");
        assertEquals(0, signs.rows.size(), "要件不齐一笔未落");
        assertEquals(OadsPermitSignResult.MSG_NO_SUCH_ORDER,
                service.sign("XK-NONE", 1, "受理岗A", AGREE, "查无此单", at("2026-10-05 10:00:00")).getMessage());
    }
}
