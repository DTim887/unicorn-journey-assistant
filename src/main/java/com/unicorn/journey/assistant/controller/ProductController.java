package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.Product;
import com.unicorn.journey.assistant.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class ProductController {

    private final ProductService productService;

    @GetMapping("/product/{id}")
    public Result getProductById(@PathVariable("id") int id) {
        Product product = productService.getProductById(id);
        return Result.ok(product);
    }

    @GetMapping("/product/all")
    public Result allProduct() {
        List<Product> allProduct = productService.getAllProduct();
        return Result.ok(allProduct);
    }

}
