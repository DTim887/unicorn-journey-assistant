package com.unicorn.journey.assistant.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

public class Base64Utils {

    public static String convertToBase64(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            // 根据你的业务逻辑处理空文件或 null 的情况
            // 可以返回 null, 抛出异常, 或返回空字符串
            return null; // 或 throw new IllegalArgumentException("File is null or empty");
        }
        try {
            // 1. 获取文件的字节数组
            byte[] fileBytes = multipartFile.getBytes();
            // 2. 使用 Base64 编码器进行编码
            byte[] encodedBytes = Base64.getEncoder().encode(fileBytes);
            // 3. 将编码后的字节数组转换为字符串
            return new String(encodedBytes);

        } catch (IOException e) {
            // 重新抛出或包装 IOException
            throw new IOException("Failed to read file content for Base64 conversion", e);
        }
    }
}
