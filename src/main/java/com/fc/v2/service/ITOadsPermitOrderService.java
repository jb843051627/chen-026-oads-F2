package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TOadsAcceptBook;
import com.fc.v2.model.auto.TOadsPermitOrder;
import com.fc.v2.model.auto.TOadsPermitSign;
import com.fc.v2.model.auto.TOadsPermitStage;
import com.fc.v2.model.auto.TOadsSpotChangeLog;
import com.fc.v2.model.custom.OadsPermitCount;
import com.fc.v2.model.custom.OadsPermitSignResult;

import java.util.Date;
import java.util.List;

/**
 * 设置许可核准单 Service接口（新一套：三段签署数法另立承接，旧一套 t_oads_permit_bill 锁住不动）
 *
 * 旁路一张不开：没有另起单、改旧笔、追补签署的入口；
 * 一次签署只有 {@link #sign} 这一个入口，一张单只有 {@link #openFromSpot} 这一个来路。
 *
 * @author fuce
 * @date 2026-10-05
 */
public interface ITOadsPermitOrderService {

    /**
     * 立单：只能由点位主档的记录生成，同一处点位、同一次变更只对应一张在办单。
     * 主单、三段段节、受理登记簿同一次落库。点位不在册或已有同变更在办单，返回 null。
     */
    TOadsPermitOrder openFromSpot(String spotNo, Integer changeKind, String operBy, Date at);

    /**
     * 签署：唯一入口。报文里写了第几段不作凭，段次与人数一概以服务端算得的为凭；
     * 屏上回显的已签人数只是回显，没有一处留给人工填写。
     */
    OadsPermitSignResult sign(String orderNo, Integer stageNo, String signer, Integer verdict, String opinion, Date at);

    /** 按主键回查单据 */
    TOadsPermitOrder selectTOadsPermitOrderById(Long id);

    /** 按单号回查单据 */
    TOadsPermitOrder selectByOrderNo(String orderNo);

    /** 分页台账查询（回显当前段次与整单情形，均为服务端轧出） */
    List<TOadsPermitOrder> selectTOadsPermitOrderList(QueryWrapper<TOadsPermitOrder> queryWrapper);

    /** 单内按段分节：三段段节（应签人数、通过规则、已签人数、轮次、段节情形） */
    List<TOadsPermitStage> stagesOf(String orderNo);

    /** 签署留痕：各轮文本都查得到，按各自落单时刻排，同一秒各自一笔 */
    List<TOadsPermitSign> signsOf(String orderNo, Integer stageNo);

    /** 受理登记簿查询（当日登记逐笔核对用） */
    List<TOadsAcceptBook> selectAcceptBookList(QueryWrapper<TOadsAcceptBook> queryWrapper);

    /** 点位变更履历：按时间倒序一段往回看 */
    List<TOadsSpotChangeLog> changeLogsOf(String spotNo);

    /** 未封存计数：页面顶部那个数与逐段累加基于同一次计算，偏差单据指名报出 */
    OadsPermitCount unsealedCount();

    /**
     * 事后核对：当日所立之单与当日受理登记簿逐笔吻合则回空串列；
     * 对不上的单号（只在一边出现的）逐张指名。
     */
    List<String> findUnreconciledOrders(Date day);
}
