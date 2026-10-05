package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TOadsAcceptBook;
import com.fc.v2.model.auto.TOadsPermitOrder;
import com.fc.v2.model.custom.OadsPermitCount;
import com.fc.v2.model.custom.OadsPermitSignResult;
import com.fc.v2.service.ITOadsPermitOrderService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 设置许可核准单 Controller（新一套：三段签署数法另立承接）
 *
 * 旁路一张不开：没有另起单、改旧笔、追补签署的端点；
 * 一次签署只有 /sign 这一个入口，一张单只有 /open 这一个来路（由点位主档生成）。
 * 表样沿用现有模板，本次不改版式——这里只出数据。
 *
 * @author fuce
 * @date 2026-10-05
 */
@Api(value = "设置许可核准单")
@Controller
@RequestMapping("/oadsPermitOrder")
public class OadsPermitOrderController extends BaseController {

    private final String prefix = "admin/oadsPermitOrder";

    @Autowired
    private ITOadsPermitOrderService oadsPermitOrderService;

    @ApiOperation(value = "核准单台账跳转", notes = "核准单台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("oadsPermitOrder:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "设置许可核准单台账", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public ResultTable list(TOadsPermitOrder record) {
        QueryWrapper<TOadsPermitOrder> queryWrapper = new QueryWrapper<TOadsPermitOrder>();
        queryWrapper.eq("del_flag", 0);
        if (record != null && record.getSpotNo() != null && !record.getSpotNo().trim().isEmpty()) {
            queryWrapper.eq("spot_no", record.getSpotNo());
        }
        if (record != null && record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        queryWrapper.orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TOadsPermitOrder> page =
                new com.github.pagehelper.PageInfo<TOadsPermitOrder>(oadsPermitOrderService.selectTOadsPermitOrderList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "设置许可核准单立单", action = "open")
    @ApiOperation(value = "由点位主档立单", notes = "同一处点位、同一次变更只对应一张在办单")
    @PostMapping("/open")
    @RequiresPermissions("oadsPermitOrder:open")
    @ResponseBody
    public AjaxResult open(String spotNo, Integer changeKind) {
        TOadsPermitOrder order = oadsPermitOrderService.openFromSpot(spotNo, changeKind, ShiroUtils.getLoginName(), new Date());
        return order != null ? success("立单成功，单号：" + order.getOrderNo()) : error("立单被拒：点位不在册，或该点位此项变更已有在办单");
    }

    @Log(title = "设置许可核准单签署", action = "sign")
    @ApiOperation(value = "签署一笔", notes = "唯一签署入口；段次与人数以服务端算得的为凭")
    @PostMapping("/sign")
    @RequiresPermissions("oadsPermitOrder:sign")
    @ResponseBody
    public AjaxResult sign(String orderNo, Integer stageNo, Integer verdict, String opinion) {
        OadsPermitSignResult r = oadsPermitOrderService.sign(orderNo, stageNo, ShiroUtils.getLoginName(), verdict, opinion, new Date());
        return success(200, r.getMessage(), r);
    }

    @Log(title = "设置许可核准单段节回显", action = "stages")
    @ApiOperation(value = "段节回显", notes = "应签人数、通过规则、已签人数、轮次、段节情形")
    @GetMapping("/stages")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public AjaxResult stages(String orderNo) {
        return retobject(200, oadsPermitOrderService.stagesOf(orderNo));
    }

    @Log(title = "设置许可核准单签署留痕", action = "signs")
    @ApiOperation(value = "签署留痕", notes = "各轮文本都查得到，按各自落单时刻排")
    @GetMapping("/signs")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public AjaxResult signs(String orderNo, Integer stageNo) {
        return retobject(200, oadsPermitOrderService.signsOf(orderNo, stageNo));
    }

    @Log(title = "设置许可核准单未封存计数", action = "unsealedCount")
    @ApiOperation(value = "未封存计数", notes = "页面顶部那个数与逐段累加基于同一次计算，偏差单据指名")
    @GetMapping("/unsealedCount")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public AjaxResult unsealedCount() {
        OadsPermitCount c = oadsPermitOrderService.unsealedCount();
        return success(200, c.getMessage(), c);
    }

    @Log(title = "点位变更履历", action = "changeLogs")
    @ApiOperation(value = "点位变更履历", notes = "按时间倒序一段往回看")
    @GetMapping("/changeLogs")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public AjaxResult changeLogs(String spotNo) {
        return retobject(200, oadsPermitOrderService.changeLogsOf(spotNo));
    }

    @Log(title = "受理登记簿核对", action = "acceptBook")
    @ApiOperation(value = "受理登记簿", notes = "当日受理登记逐笔核对")
    @GetMapping("/acceptBook")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public ResultTable acceptBook(TOadsAcceptBook record) {
        QueryWrapper<TOadsAcceptBook> queryWrapper = new QueryWrapper<TOadsAcceptBook>();
        queryWrapper.eq("del_flag", 0);
        if (record != null && record.getSpotNo() != null && !record.getSpotNo().trim().isEmpty()) {
            queryWrapper.eq("spot_no", record.getSpotNo());
        }
        queryWrapper.orderByDesc("accepted_at").orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TOadsAcceptBook> page =
                new com.github.pagehelper.PageInfo<TOadsAcceptBook>(oadsPermitOrderService.selectAcceptBookList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "受理登记簿与当日所立之单核对", action = "reconcile")
    @ApiOperation(value = "当日核对", notes = "对不上的单号逐张指名，空串列即逐笔吻合")
    @GetMapping("/reconcile")
    @RequiresPermissions("oadsPermitOrder:list")
    @ResponseBody
    public AjaxResult reconcile(Date day) {
        List<String> diff = oadsPermitOrderService.findUnreconciledOrders(day);
        return success(200, diff.isEmpty() ? "当日登记与所立之单逐笔吻合" : "对不上的单号：" + String.join("、", diff), diff);
    }
}
