package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.PurchaseService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryUnreceivePurchaseList(Map<String, Object> params) {
        QueryWrapper<PurchaseEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1).or().eq("status", 0);


        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 合并采购单
     * <p>
     * <p>
     * 这些采购需求之前就已经创建过了，现在只是把他们放到同一张采购单里面去，所以这里其实是修改操作
     * 就是把前端传递过来的，被选中的采购需求所对应的采购单设置为同一个
     *
     * @param mergeVo
     */
    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {

        Long purchaseId = mergeVo.getPurchaseId();

        //如果没有采购单Id,则要新建一个采购单
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            //设置采购单状态为新建
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getStatus());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        /*lambda表达式里面使用的局部变量必须是final或者effectively final的，这样可以避免在多线程环境下出错
        也就是说这个局部变量自从创建了之后就不能再修改它的值
         */

        //采购需求的状态是0（新建）或者1（已分配）才可以合并，因为正在采购中的商品不能被添加到采购单里面去

        Long purchaseFinishId = purchaseId;
        List<PurchaseDetailEntity> purchaseDetailEntities = mergeVo.getItems().stream().map(itemId -> {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            //设置采购需求的状态为已分配（表示已经分配给了某一个采购单）
            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.ASSIGNED.getStatus());
            purchaseDetailEntity.setPurchaseId(purchaseFinishId);
            purchaseDetailEntity.setId(itemId);    //设置采购需求的id
            return purchaseDetailEntity;
        }).collect(Collectors.toList());

        Collection<PurchaseDetailEntity> detailEntities = purchaseDetailService.listByIds(purchaseDetailEntities.stream()
                .map(PurchaseDetailEntity::getId).collect(Collectors.toList()));
        for (PurchaseDetailEntity detailEntity : detailEntities) {
            if (!detailEntity.getStatus().equals(WareConstant.PurchaseDetailEnum.CREATED.getStatus()) ||
                    !detailEntity.getStatus().equals(WareConstant.PurchaseDetailEnum.ASSIGNED.getStatus())) {
                throw new RuntimeException("只有【新建】或者【已分配】状态的采购需求才能被合并");
            }
            log.error("用户提交的采购需求状态错误");
        }


        purchaseDetailService.updateBatchById(purchaseDetailEntities);

        //修改采购单的更新时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        updateById(purchaseEntity);

    }

    /**
     * 员工接收采购单，开始采购
     *
     * @param purchaseIds
     */
    @Override
    public void receivePurchase(List<Long> purchaseIds) {
        List<PurchaseEntity> purchaseEntities = purchaseIds.stream().map(
                //查找所有的采购单
                purchaseid -> {
                    return this.getById(purchaseid);
                }
        ).filter(purchaseEntity ->
        {
            if (purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.RECEIVED.getStatus() ||
                    purchaseEntity.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getStatus()) {
                //过滤得到所有的已分配或者新建的采购单
                return true;
            } else {
                return false;
            }

        }).map(purchaseEntity -> {
            //设置采购单的更新时间和状态（变为已领取）
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVED.getStatus());
            return purchaseEntity;
        }).collect(Collectors.toList());

        //更新采购单状态
        this.updateBatchById(purchaseEntities);


        purchaseEntities.forEach(purchaseEntity -> {
            //查询每个采购单里面的所有采购需求
            List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailService
                    .listDetailByPurchaseId(purchaseEntity.getId());


            List<PurchaseDetailEntity> detailEntities = purchaseDetailEntities.stream().map(purchaseDetailEntity -> {
                PurchaseDetailEntity purchaseDetail = new PurchaseDetailEntity();
                purchaseDetail.setId(purchaseDetailEntity.getId());    //设置采购需求的id
                purchaseDetail.setStatus(WareConstant.PurchaseDetailEnum.BUYING.getStatus());    //设置采购需求的状态
                return purchaseDetail;
            }).collect(Collectors.toList());
            purchaseDetailService.updateBatchById(detailEntities);
        });


//        //更新购单对应的采购需求状态为采购中   （这种方法是错误的）
//        List<PurchaseDetailEntity> purchaseDetailEntities = purchaseEntities.stream().map(purchaseEntity -> {
//            //设置采购单id
//            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
//            purchaseDetailEntity.setId(purchaseEntity.getId());     //这里id设置错误，purchaseId是采购单的id，不是detail的id
//            //设置采购需求的采购状态为采购中
//            purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.BUYING.getStatus());
//            return purchaseDetailEntity;
//        }).collect(Collectors.toList());

        //设置采购需求的采购状态
//        由于没有设置detail表的id，所以无法使用这种方法进行更新
//        purchaseDetailService.updateBatchById(purchaseDetailEntities);
    }

    @Transactional
    @Override
    public void finishPurchase(PurchaseDoneVo purchaseDoneVo) {

        //1 改变采购项的状态
        List<PurchaseItemDoneVo> purchaseItemDoneVo = purchaseDoneVo.getItems();
        List<PurchaseDetailEntity> updateList = new ArrayList<>();
        boolean fail = false;
        for (PurchaseItemDoneVo itemDoneVo : purchaseItemDoneVo) {
            PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
            if (itemDoneVo.getStatus() == WareConstant.PurchaseDetailEnum.FAILED.getStatus()) {
                fail = true;
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.FAILED.getStatus());
            } else {
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailEnum.FINISHED.getStatus());
                //3 将采购成功的商品入库
                Long itemId = itemDoneVo.getItemId();    //获取采购项id
                PurchaseDetailEntity detailEntity = purchaseDetailService.getById(itemId);
                Long wareId = detailEntity.getWareId();    //要放在哪个仓库
                Integer skuNum = detailEntity.getSkuNum();    //要添加多少件商品
                wareSkuService.addStock(itemId, wareId, skuNum);
            }
            purchaseDetailEntity.setId(itemDoneVo.getItemId());
            updateList.add(purchaseDetailEntity);

        }

        purchaseDetailService.updateBatchById(updateList);

        //2 如果采购项全部采购成功，改变采购单的状态为已完成
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseDoneVo.getId());
        if (fail) {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.HASERROR.getStatus());
        } else {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.FINISHED.getStatus());
        }
        purchaseEntity.setUpdateTime(new Date());
        updateById(purchaseEntity);

    }

}