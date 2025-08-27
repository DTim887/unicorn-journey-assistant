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

    public void saveProduct(Product product) {
        this.put(product.getProductName(), product);
    }

    @Tool("Get product by name")
    public Product getProductByName(String name) {
        return this.get(name);
    }

    @Tool("Get all product")
    public List<Product> getAllProduct() {
        return this.getAll(Product.class);
    }


}
