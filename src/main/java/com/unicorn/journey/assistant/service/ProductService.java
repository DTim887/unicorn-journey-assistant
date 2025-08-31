package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.Product;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.PRODUCT)
public class ProductService extends BaseService<Product> {

    /**
     * 保存产品到缓存，Key=id
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
     * 获取所有产品列表
     * @return List<Product>
     */
    @Tool("获取当前在售的所有产品的信息")
    public List<Product> getAllProduct() {
        return this.getAll(Product.class);
    }


}
