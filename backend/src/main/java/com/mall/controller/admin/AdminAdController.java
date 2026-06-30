package com.mall.controller.admin;

import com.mall.common.Result;
import com.mall.model.AdCategory;
import com.mall.model.Advertisement;
import com.mall.model.dto.AdAdminRequest;
import com.mall.service.AdvertisementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ads")
public class AdminAdController {

    private final AdvertisementService advertisementService;

    public AdminAdController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @GetMapping
    public Result<List<Advertisement>> list() {
        return Result.ok(advertisementService.adminListAds());
    }

    @PostMapping
    public Result<Advertisement> create(@Valid @RequestBody AdAdminRequest request) {
        return Result.ok(advertisementService.createAd(request));
    }

    @PutMapping("/{id}")
    public Result<Advertisement> update(@PathVariable Long id, @Valid @RequestBody AdAdminRequest request) {
        return Result.ok(advertisementService.updateAd(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        advertisementService.deleteAd(id);
        return Result.ok();
    }

    @GetMapping("/categories")
    public Result<List<AdCategory>> listCategories() {
        return Result.ok(advertisementService.listAdCategories());
    }

    @PostMapping("/categories")
    public Result<AdCategory> createCategory(@RequestBody Map<String, String> body) {
        return Result.ok(advertisementService.createAdCategory(body.get("name")));
    }
}
