package com.unicorn.journey.assistant.controller;

import com.unicorn.journey.assistant.controller.vo.Result;
import com.unicorn.journey.assistant.entity.User;
import com.unicorn.journey.assistant.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //switch login user
    @PostMapping("/user/login")
    public Result login(@RequestParam String nickname) {
        User user = userService.retrieveUserByNickname(nickname);
        if (user != null) {
            userService.login(user);
        }
        return Result.ok();
    }

    //Get current login user
    @GetMapping("/user/current")
    public Result currentUser() {
        User currentUser = userService.currentUser();
        return Result.ok(currentUser);
    }

}
