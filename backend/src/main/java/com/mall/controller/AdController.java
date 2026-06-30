package com.mall.controller;

import com.mall.common.Result;
import com.mall.model.Advertisement;
import com.mall.service.AdvertisementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
public class AdController {

    private final AdvertisementService advertisementService;

    public AdController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @GetMapping
    public Result<List<Advertisement>> listAds(@RequestParam(required = false) Long categoryId) {
        return Result.ok(advertisementService.listActiveAds(categoryId));
    }
}
