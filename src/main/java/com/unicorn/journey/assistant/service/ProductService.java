package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.controller.vo.ProductVO;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.entity.mappers.ProductMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@LocalCache(value = CacheName.PRODUCT)
public class ProductService extends BaseService<Product> {


    /**
     * 保存产品到缓存，Key=id
     *
     * @param product
     */
    public void saveProduct(Product product) {
        this.put(product.getId(), product);
    }

    //    @Tool("Get product by name")
    public Product getProductById(int id) {
        return this.get(id);
    }

    /**
     * 根据日期获取当天在售的所有产品的信息
     *
     * @return List<Product>
     */
    @Tool("根据日期获取当天在售的所有产品的信息")
    public List<ProductVO> getAllProductByDate(String visitDate) {
        List<ProductVO> productVOList = new ArrayList<>();
        List<Product> products = this.getAll(Product.class);
        for (Product product : products) {
            ProductVO productVO = ProductMapper.INSTANCE.convertToProductVO(product);
            Product.InventoryCalendar inventoryCalendar = Arrays.stream(product.getInventoryCalendar())
                    .filter(ic ->  ic.getDate() .equals(visitDate))
                    .findFirst().orElse(null);
            productVO.setDate(visitDate);
            if (inventoryCalendar != null) {
                productVO.setPrice(inventoryCalendar.getPrice());
                productVO.setInventory(inventoryCalendar.getInventory());
            }
            productVOList.add(productVO);
        }
        return productVOList;
    }

    public List<Product> getAllProduct() {
        return this.getAll(Product.class);
    }


}
