package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.model.dto.UserView;
import com.mall.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<Page<UserView>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userService.adminListUsers(page, size).map(UserView::from));
    }

    @PutMapping("/{id}/status")
    public Result<UserView> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return Result.ok(UserView.from(userService.adminUpdateStatus(id, body.get("status"))));
    }
}
