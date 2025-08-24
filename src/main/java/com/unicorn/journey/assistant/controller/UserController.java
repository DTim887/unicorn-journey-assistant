package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.request.LoginRequest;
import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //switch login user
    @CrossOrigin(origins = "*")
    @PostMapping("/user/login")
    public Result login(@RequestBody LoginRequest loginRequest) {
        User user = userService.retrieveUserByNickname(loginRequest.getNickname());
        if (user != null) {
            userService.login(user);
        }
        return Result.ok();
    }

    //Get current login user
    @CrossOrigin(origins = "*")
    @GetMapping("/user/current")
    public Result currentUser() {
        User currentUser = userService.currentUser();
        return Result.ok(currentUser);
    }

    //Get all user in system
    @CrossOrigin(origins = "*")
    @GetMapping("/user/all")
    public Result allUser() {
        List<User> allUser = userService.getAll(User.class);
        return Result.ok(allUser);
    }

}
