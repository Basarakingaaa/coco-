package com.mall.service;

import com.mall.model.AdCategory;
import com.mall.model.Advertisement;
import com.mall.model.dto.AdAdminRequest;

import java.util.List;

public interface AdvertisementService {
    List<Advertisement> listActiveAds(Long categoryId);

    // ---- 管理端 ----
    List<Advertisement> adminListAds();
    Advertisement createAd(AdAdminRequest request);
    Advertisement updateAd(Long id, AdAdminRequest request);
    void deleteAd(Long id);

    List<AdCategory> listAdCategories();
    AdCategory createAdCategory(String name);
}
