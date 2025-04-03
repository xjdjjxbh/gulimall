package com.atguigu.gulimall.product.app;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.service.AttrAttrgroupRelationService;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.AttrService;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 属性分组
 *
 * @author liuchong
 * @email sunlightcs@gmail.com
 * @date 2025-01-22 20:07:13
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private AttrService attrService;
    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    /**
     * 属性分组三级分类查询
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params,
                  @PathVariable("catelogId") Long catelogId) {
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params,catelogId);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
        Long catelogId = attrGroup.getCatelogId();
        Long[] path = categoryService.findCatelogPath(catelogId);
        attrGroup.setCatelogPath(path);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    /**
     * 获取当前分组关联的所有属性
     */
    @GetMapping("/{attrgroupId}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId) {
        List<AttrEntity> list = attrService.getRelations(attrgroupId);
        return R.ok().put("data", list);
    }

    /**
     * 删除属性与分组的关联关系
     * @param attrGroupRelationVos
     * @return
     */
    @PostMapping("/attr/relation/delete")
    public R deleteRelations(@RequestBody AttrGroupRelationVo[] attrGroupRelationVos) {
        attrService.deleteRelations(attrGroupRelationVos);
        return R.ok();
    }

    /**
     * 获取属性分组没有关联的其他属性
     * @param attrgroupId   属性分组id
     * @param params
     * @return
     */
    @GetMapping("/{attrgroupId}/noattr/relation")
    public R queryNotRelationed(@PathVariable("attrgroupId") Long attrgroupId,
                                @RequestParam Map<String, Object> params) {
        PageUtils page = attrService.queryNotRelationed(attrgroupId,params);
        return R.ok().put("page", page);
    }

    /**
     * 添加属性与分组关联关系
     * @param attrGroupRelationVos
     * @return
     */
    @PostMapping("/attr/relation")
    public R queryNotRelationed(@RequestBody AttrGroupRelationVo[] attrGroupRelationVos) {
        attrAttrgroupRelationService.addRelation(attrGroupRelationVos);
        return R.ok();
    }


    /**
     * 获取分类下所有分组&关联属性
     * @param catelogId   分类id
     * @return
     */
    @GetMapping("/{catelogId}/withattr")
    public R getGroupsWithAttrs (@PathVariable("catelogId") Long catelogId) {
        List<AttrGroupWithAttrsVo> list = attrAttrgroupRelationService.getGroupsWithAttrs(catelogId);
        return R.ok().put("data", list);
    }


}
