package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.order.constant.OrderConstant;
import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.WareFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 给订单确认页返回需要用到的数据
     *
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        /*这个是基于ThreadLocal的，在不同的线程中，获取到的请求头是不一样的
        此处是获取老请求中的请求头
         */
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //获取当前登录的用户
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {

            /*这个是基于ThreadLocal的，在不同的线程中，获取到的请求头是不一样的
            此处是将老请求中的请求头信息放到当前的异步请求中
            */
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addresses = memberFeignService.getAddresses(memberResponseVo.getId());
            orderConfirmVo.setAddress(addresses);
        }, executor);


        //获取购物车里面所有的购物项
        CompletableFuture<Void> getCartsFuture = CompletableFuture.runAsync(() -> {

            /*这个是基于ThreadLocal的，在不同的线程中，获取到的请求头是不一样的
            此处是将老请求中的请求头信息放到当前的异步请求中
            */
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> orderItemVos = cartFeignService.currentUserCartItems();
            orderConfirmVo.setItems(orderItemVos);
        }, executor).thenRunAsync(() -> {
            //查询当前商品有货还是无货
/*            RequestContextHolder.setRequestAttributes(requestAttributes);
            这里是又开启了一个新的线程来查询是否有库存，由于这里不需要用户信息，所以这里不需要获取请求头信息
 */
            List<Long> skuIds = orderConfirmVo.getItems().stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.getHasStock(skuIds);
            List<SkuHasStockVo> data = r.getData(new TypeReference<List<SkuHasStockVo>>() {
            });

            HashMap<Long, Boolean> map = new HashMap<>();
            if (data != null && !data.isEmpty()) {
                for (SkuHasStockVo item : data) {
                    map.put(item.getSkuId(), item.getHasStock());
                }
                orderConfirmVo.setStocks(map);
            }


        }, executor);


        //设置用户积分
        Integer integration = memberResponseVo.getIntegration();
        orderConfirmVo.setIntegration(integration);

        CompletableFuture.allOf(getAddressFuture, getCartsFuture).join();

        //生成订单防重令牌
        String orderToken = UUID.randomUUID().toString().replace("-", "");

        //分别给用户和后台存储一个orderToken
        orderConfirmVo.setOrderToken(orderToken);
        stringRedisTemplate.opsForValue()
                .set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), orderToken, 30, TimeUnit.MINUTES);

        return orderConfirmVo;
    }

    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo orderSubmitVo) {

        SubmitOrderResponseVo submitOrderResponseVo = new SubmitOrderResponseVo();

        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        //获取用户提交的令牌
        String submitOrderToken = orderSubmitVo.getOrderToken();

        //获取我们之前存储的token
//        String storedOrderToken = stringRedisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
        //如果用户提交的token和后台存储的token不一致
//        if (submitOrderToken == null || !submitOrderToken.equals(storedOrderToken)) {
//            return submitOrderResponseVo;
//        }

        /*对比和删除token应该是一个原子操作才行，不然在用户同时提交多个请求的情况下，因为异步性问题，还是可能会出现订单重复提交的情况
        并且这里不能简单地使用synchronized方法来实现原子性，因为这只分布式项目，synchronized只能锁住的单个服务
         */
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long execute = stringRedisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),     //要执行的脚本以及脚本返回的数据类型
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()),     //脚本里面的key值
                orderSubmitVo.getOrderToken());      //脚本里面的value值，它是和key值一一对应的


        if (execute != null && execute == 0) {
            //令牌验证失败
            return submitOrderResponseVo;
        }


        return null;
    }

}