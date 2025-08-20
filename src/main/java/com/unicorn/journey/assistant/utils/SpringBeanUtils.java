package com.unicorn.journey.assistant.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

public class SpringBeanUtils {

    @Component
    public static class ApplicationContextHolder implements ApplicationContextAware {
        private static ApplicationContext context;
        @Override
        public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
            context = applicationContext;
        }
    }

    private static ApplicationContext getContext() {
        return ApplicationContextHolder.context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return getContext().getBean(clazz);
    }
}
