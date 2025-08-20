package com.unicorn.journey.assistant.service;

import com.unicorn.journey.assistant.annotations.LocalCache;
import com.unicorn.journey.assistant.constant.CacheName;
import com.unicorn.journey.assistant.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@LocalCache(value = CacheName.USERS)
public class UserService extends BaseService<User> {


    public void saveUser(User user) {
        this.put(user.getId(), user);
    }

    public User retrieveUserById(int id) {
        return this.get(id);
    }

    public List<User> retrieveAllUsers() {
        return this.getAll(User.class);
    }
}
