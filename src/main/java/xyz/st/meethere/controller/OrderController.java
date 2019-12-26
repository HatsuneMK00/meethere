package xyz.st.meethere.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.hibernate.validator.internal.engine.messageinterpolation.InterpolationTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.ParameterResolutionDelegate;
import org.springframework.web.bind.annotation.*;
import xyz.st.meethere.entity.Ground;
import xyz.st.meethere.entity.PreOrder;
import xyz.st.meethere.entity.ResponseMsg;
import xyz.st.meethere.entity.User;
import xyz.st.meethere.service.AdminService;
import xyz.st.meethere.service.GroundService;
import xyz.st.meethere.entity.*;
import xyz.st.meethere.service.OrderService;
import xyz.st.meethere.service.UserService;

import java.text.ParseException;
import java.util.List;

@RestController
@ResponseBody
public class OrderController {
    final
    OrderService orderService;
    final
    UserService userService;
    final
    AdminService adminService;
    final
    GroundService groundService;

    public OrderController(OrderService orderService, UserService userService, AdminService adminService,
                           GroundService groundService) {
        this.orderService = orderService;
        this.userService = userService;
        this.adminService = adminService;
        this.groundService = groundService;
    }

    @ApiOperation(value = "获取所有订单, 包括用户的信息和场地的信息")
    @GetMapping("/order")
    ResponseMsg getOrders() {
        ResponseMsg responseMsg = new ResponseMsg();
        responseMsg.setStatus(404);
        // fixme: 返回的内容里应该有userName,groundName
        List<PreOrder> preOrders = orderService.getOrders();

        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", preOrders);
        return responseMsg;
    }

    @ApiOperation(value = "获取用户的所有订单", notes = "如果返回404，则用户不存在")
    @GetMapping("/order/user/{userId}/preOrder")
    ResponseMsg getOrdersOfUSer(@PathVariable("userId") Integer id) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (!orderService.checkUserExistence(id)) {
            responseMsg.setStatus(404);
            return responseMsg;
        }
        // fixme: 返回的内容里应该有userName,groundName
        List<PreOrder> preOrders = orderService.getAllPreOrdersOfUser(id);
        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", preOrders);
        return responseMsg;
    }

    @ApiOperation("获取某用户指定订单")
    @GetMapping("/order/user/{userId}/order/{preOrderId}")
    ResponseMsg getOrderByIdOfUSer(@PathVariable("userId") Integer uid, @PathVariable("orderid") Integer oid) {
        // fixme: 返回的内容里应该有userName,groundName
        PreOrder preOrder = orderService.getPreOrder(uid, oid);
        ResponseMsg responseMsg = new ResponseMsg();
        if (preOrder == null) {
            responseMsg.setStatus(404);
            return responseMsg;
        }
        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", preOrder);
        return responseMsg;
    }

    @ApiOperation(value = "新增用户订单", notes = "若返回510则说明用户输入的开始时间和duration与该场地现有预约单冲突")
    @PostMapping("/order/user/{userId}/order")
    ResponseMsg addAnOrder(
            @RequestParam("groundId") Integer gid,
            @PathVariable("userId") Integer uid,
            @RequestParam("startTime") String startTime,
            @RequestParam("duration") Integer duration
    ) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (orderService.validatePreOrder(gid, startTime, duration)) {
            responseMsg.setStatus(510);
            return responseMsg;
        }
        PreOrder preOrder = new PreOrder();
        preOrder.setGroundId(gid);
        preOrder.setUserId(uid);
        preOrder.setStartTime(startTime);
        preOrder.setDuration(duration);
        preOrder.setPrice(duration * orderService.getGroundPrice(gid));
        if (orderService.addPreOrder(preOrder) == 1) {
            responseMsg.setStatus(200);
            responseMsg.getResponseMap().put("result", preOrder);
            return responseMsg;
        }
        responseMsg.setStatus(500);
        return responseMsg;
    }

    @ApiOperation("删除用户指定订单")
    @DeleteMapping("/order/{preOrderId}")
    ResponseMsg deleteOrder(@PathVariable("preOrderId") Integer oid) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (orderService.deletePreOrder(oid) == 1)
            responseMsg.setStatus(200);
        else responseMsg.setStatus(500);
        return responseMsg;
    }

    @ResponseBody
    @ApiOperation("通过preOrderId批量删除新闻")
    @DeleteMapping("/order/deleteByBatch")
    ResponseMsg deleteOrderByBatch(@RequestBody List<Integer> ids) {
        ResponseMsg msg = new ResponseMsg();
        msg.setStatus(404);
        for (Integer id : ids) {
            deleteOrder(id);
        }
        msg.setStatus(200);
        return msg;
    }

    @ApiOperation("获取某场地在目前时间之后所有预约单的开始时间和持续时间，并按开始时间升序排序")
    @GetMapping("/order/ground/{groundId}/orderTime")
    ResponseMsg getGroundOrderTime(@PathVariable("groundId") Integer gid) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (!orderService.checkGroundExistence(gid)) {
            responseMsg.setStatus(404);
            return responseMsg;
        }
        List<List> lists = orderService.getOrderTime(gid);
        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", lists);
        return responseMsg;
    }

    @ApiOperation(value = "获取某场地的所有订单", notes = "若返回404则表明场地不存在")
    @GetMapping("/order/ground/{groundId}/order")
    ResponseMsg getGroundOrders(@PathVariable("groundId") Integer gid) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (!orderService.checkGroundExistence(gid)) {
            responseMsg.setStatus(404);
            return responseMsg;
        }
        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", orderService.getGroundOrders(gid));
        return responseMsg;
    }

    //管理员用接口
    @ApiOperation("获取所有未审核订单")
    @GetMapping("/order/uncheckedOrder")
    ResponseMsg getAllUncheckedComment() {
        ResponseMsg responseMsg = new ResponseMsg();
        List<PreOrder> orders = null;
        // fixme: 返回的内容里应该有userName,groundName
//        fixme: 不通过的订单也要返回
        orders = orderService.getUncheckedOrders();
        responseMsg.getResponseMap().put("result", orders);
        responseMsg.setStatus(200);
        return responseMsg;
    }

    @ApiOperation("将指定订单审核状态标记为通过")
    @PutMapping("/order/check/{preOrderId}")
    ResponseMsg checkOrder(@PathVariable("preOrderId") Integer pid) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (!orderService.checkPreOrderExistence(pid)) {
            responseMsg.setStatus(404);
            return responseMsg;
        }
        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", orderService.checkPreOrder(pid));
        return responseMsg;
    }

    @ApiOperation("将指定订单审核状态标记为未通过")
    @PutMapping("/order/uncheck/{preOrderId}")
    ResponseMsg uncheckOrder(@PathVariable("preOrderId") Integer pid) {
        ResponseMsg responseMsg = new ResponseMsg();
        if (!orderService.checkPreOrderExistence(pid)) {
            responseMsg.setStatus(404);
            return responseMsg;
        }
        responseMsg.setStatus(200);
        responseMsg.getResponseMap().put("result", orderService.checkPreOrderFail(pid));
        return responseMsg;
    }
}
