package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.User;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.USERS)
public class UserService extends BaseService<User> {


    //Save user with nickname as key
    public void saveUser(User user) {
        this.put(user.getNickname(), user);
    }

    @Tool("Get user by nickname")
    public User retrieveUserByNickname(String name) {
        return this.get(name);
    }

    @Tool("Get all users")
    public List<User> retrieveAllUsers() {
        return this.getAll(User.class);
    }

    //用户登录
    public void login(User user) {
        this.put("login", user);
    }

    //获取当前登录用户
    public User currentUser() {
        return this.get("login");
    }
}
