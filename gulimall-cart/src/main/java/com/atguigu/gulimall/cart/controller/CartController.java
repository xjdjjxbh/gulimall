package com.atguigu.gulimall.cart.controller;


import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;


    @GetMapping("cart.html")
    public String cartListPage(Model model) {
        CartVo cartVo = cartService.getCart();
        //将查询出来的数据放到model里面去，然后前端就可以通过model获取到数据了
        model.addAttribute("cart", cartVo);
        return "cartList";
    }

    @GetMapping("checkItem")
    public String checked(@RequestParam("skuId") Long skuId, @RequestParam("checked") Integer checked) {
        cartService.checked(skuId,checked);
        //勾选成功了之后重定向到购物车列表页，相当于刷新
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num) {
        cartService.countItem(skuId,num);
        //改变数量成功了之后重定向到购物车列表页，相当于刷新
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        //改变数量成功了之后重定向到购物车列表页，相当于刷新
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num,
                            RedirectAttributes ra) {
        CartItemVo cartItemVo = cartService.addToCart(skuId, num);
//        model.addAttribute("item", cartItemVo);

        //在RedirectAttributes里面加了skuId之后，如果后面是重定向，那么springMVC会自动把这个skuId加到请求参数里面去
        ra.addAttribute("skuId", skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId, Model model) {
        /*添加购物车成功之后，我们返回到添加成功页面，再次查询一下购物车最新数据并展示
        上面的重定向跳转是为了避免请求重复提交
         */
        CartItemVo cartItemVo = cartService.getCartItem(skuId);
        model.addAttribute("item", cartItemVo);
        return "success.html";
    }

    @DeleteMapping("/addToCartSuccess.html")
    public void clerCart(@RequestParam("cartKey") String cartKey) {
        /*添加购物车成功之后，我们返回到添加成功页面，再次查询一下购物车最新数据并展示
        上面的重定向跳转是为了避免请求重复提交
         */
        cartService.clearCart(cartKey);
    }

    @ResponseBody
    @GetMapping("/currentUserCartItems")
    public List<CartItemVo> currentUserCartItems() {
        List<CartItemVo> currentUserCartItems = cartService.getCurrentUserCartItems();
        return currentUserCartItems;
    }
}
