package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final String CART_PREFIX = "gulimall:cart:";

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;


    @Override
    public CartItemVo addToCart(Long skuId, Integer num) {

        BoundHashOperations<String, Object, Object> boundHashOperations = getCartOps();

        /*查询购物车，如果购物车里面已经有了我们要添加的商品，那么我们只用修改购物车里面的商品数量即可
        如果购物车里面没有我们要添加的商品，那么我们就将商品添加到购物车里面去

        这里的get是根据redia里面map的feild字段进行get的，而不是key字段
        key （feild values）

        在上边getCartOps的时候，key就已经和登录用户绑定在一起了，所以剩下的操作都是对feild和value进行的
         */
        Object item = boundHashOperations.get(skuId.toString());
        if (item != null) {
            //如果购物车里面有该商品

            //存进去的时候我们是存储的json类型的数据,我们现在将其逆转为java对象
            CartItemVo cartItem = JSON.parseObject(item.toString(), CartItemVo.class);
            cartItem.setCount(num + cartItem.getCount());
            boundHashOperations.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }

        CartItemVo cartItemVo = new CartItemVo();

        //调用远程服务获取商品详情信息
        CompletableFuture<Void> getskuInfo = CompletableFuture.runAsync(() -> {
            R r = productFeignService.info(skuId);
            SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
            });


            cartItemVo.setCheck(true);
            cartItemVo.setCount(num);
            cartItemVo.setSkuId(skuId);
            cartItemVo.setImage(skuInfo.getSkuDefaultImg());
            cartItemVo.setTitle(skuInfo.getSkuTitle());
            cartItemVo.setPrice(skuInfo.getPrice());
        }, executor);

        //远程查询sku的组合信息
        CompletableFuture<Void> getSkuSaleAttrs = CompletableFuture.runAsync(() -> {
            List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
            cartItemVo.setSkuAttrValues(skuSaleAttrValues);
        }, executor);


        CompletableFuture.allOf(getskuInfo, getSkuSaleAttrs).join();

        /*向redis里面存入购物车商品信息
        如果我们不转化为json字符串的话，那么这个对象的序列化默认采用jdk的序列化
        这里我们直接将其转换为json字符串，那么存到redis里面的东西就方便我们辨认了
         */
        String jsonString = JSON.toJSONString(cartItemVo);
        boundHashOperations.put(skuId.toString(), jsonString);
        return cartItemVo;
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        //从redis里面获取购物车数据
        BoundHashOperations<String, Object, Object> boundHashOperations = getCartOps();
        Object item = boundHashOperations.get(skuId.toString());
        CartItemVo cartItemVo = JSON.parseObject(item.toString(), CartItemVo.class);
        return cartItemVo;
    }

    /**
     * 获取购物车里面的所有数据
     *
     * @return
     */
    @Override
    public CartVo getCart() {

        CartVo cartVo = new CartVo();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {
            //1、登录了
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //临时购物车的键
            String temptCartKey = CART_PREFIX + userInfoTo.getUserKey();

            //2、如果临时购物车的数据还未进行合并
            List<CartItemVo> tempCartItems = getCartData(temptCartKey);
            if (tempCartItems != null) {
                /*临时购物车有数据需要进行合并操作
                获取到临时购物车里面的数据之后，可以直接加入到redis里面去，不用担心覆盖，因为add方法里面会有判断
                当重复的时候就会变为修改数量
                 */
                for (CartItemVo item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                //清除临时购物车的数据
                clearCart(temptCartKey);
            }

            //3、获取登录后的购物车数据【包含合并过来的临时购物车的数据和登录后购物车的数据】
            List<CartItemVo> cartItems = getCartData(cartKey);
            cartVo.setItems(cartItems);

        } else {
            //没有登录
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            //获取临时购物车里面的所有购物项
            List<CartItemVo> cartItems = getCartData(cartKey);
            cartVo.setItems(cartItems);
        }

        return cartVo;
    }

    @Override
    public void clearCart(String cartKey) {
        stringRedisTemplate.delete(cartKey);
    }

    @Override
    public void checked(Long skuId, Integer checked) {

        CartItemVo cartItem = getCartItem(skuId);
        //因为这是直接根据已经在购物车里面的商品来查的，所以一定能够查询到商品
        cartItem.setCheck(checked == 1);
        //将修改后的数据放回购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void countItem(Long skuId, Integer num) {
        if (num == 0) {
            //如果数量减到了0，则直接删除购物车中的商品
            getCartOps().delete(skuId);
        }

        CartItemVo cartItem = getCartItem(skuId);
        //因为这是直接根据已经在购物车里面的商品来查的，所以一定能够查询到商品
        cartItem.setCount(num);
        //将修改后的数据放回购物车
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void deleteItem(Long skuId) {
        getCartOps().delete(skuId.toString());
    }

    /**
     * 获取用户购物车中的所有购物项
     *
     * @return
     */
    @Override
    public List<CartItemVo> getCurrentUserCartItems() {

        /*
        这个方法是被远程调用的,feign的远程调用默认不带cookie（session）信息，但是这里只有携带了session信息，在远程调用的时候
        才会被拦截器获取到用户的信息，所以我们在远程调用这个方法的时候要配置头信息，让其携带session信息
         */

        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo == null) {
            return null;
        }

        Long userId = userInfoTo.getUserId();

        List<CartItemVo> cartData = getCartData(CART_PREFIX + userId);
        List<CartItemVo> collect = null;
        if (cartData != null) {
            collect = cartData.stream().filter(CartItemVo::getCheck).map(item -> {
                //商品现在的价格和加入购物车时的价格可能会不同，因此这里要重新查询商品的最新价格
                BigDecimal price = productFeignService.getPrice(item.getSkuId());
                item.setPrice(price);
                return item;
            }).collect(Collectors.toList());
        }
        return collect;

    }

    private List<CartItemVo> getCartData(String key) {
        BoundHashOperations<String, Object, Object> boundHashOps = stringRedisTemplate.boundHashOps(key);
        List<Object> values = boundHashOps.values();
        if (values != null && !values.isEmpty()) {
            List<CartItemVo> collect = values.stream().map(item -> JSON.parseObject(item.toString(), CartItemVo.class)).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 获取我们要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        String cartKey = "";

        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Long userId = userInfoTo.getUserId();
        if (userId != null) {
            //用户登陆了
            cartKey = CART_PREFIX + userId;
        } else {
            //用户没有登录
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> boundHashOps = stringRedisTemplate.boundHashOps(cartKey);
        return boundHashOps;

    }
}
