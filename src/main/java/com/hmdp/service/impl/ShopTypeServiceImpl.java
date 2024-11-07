package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        //1.进入redis中查询店铺类型的缓存
        List<String> lists = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);//获取list集合中所有的元素（JSON字符串）
        List<ShopType> typeList = new ArrayList<>();    //用来存ShopType对象
        //2.判断缓存是否命中
        if (!lists.isEmpty()) {
            //3.命中，直接返回(我们要返回的一定是对象，所以需要将JSON字符串转换，遍历一下，逐一转换，再存到我们上面刚定义的存对象的集合中，最后返回)
            for (String list : lists) {
                ShopType shopType = JSONUtil.toBean(list, ShopType.class);
                typeList.add(shopType);
            }
            return Result.ok(typeList);
        }
        //注意: 到这个地方，lists是空的噢！
        //4.缓存未命中，数据库查list
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();    //数据库中的所有ShopType对象的集合
        if (shopTypeList.isEmpty()) {
            //5.不存在元素，报错误信息
            return Result.fail("不存在分类!");
        }
        //5.存在，将其写入redis
        //在此之前，先把对象转换成JSON(依旧是遍历，将查询到的list集合中每个对象都转换成JSON字符串，再用lists集合存好，lists是用来存JSON的噢)
        for (ShopType shopType : shopTypeList) {
            String jsonStr = JSONUtil.toJsonStr(shopType);
            lists.add(jsonStr);
        }
        //到这里才是真正把信息写入redis里
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY,lists);
        //7.返回客户端
        return Result.ok(shopTypeList);
    }
}
