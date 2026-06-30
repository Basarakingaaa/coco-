package com.mall.service.impl;

import com.mall.common.BusinessException;
import com.mall.model.AdCategory;
import com.mall.model.Advertisement;
import com.mall.model.dto.AdAdminRequest;
import com.mall.repository.AdCategoryRepository;
import com.mall.repository.AdvertisementRepository;
import com.mall.service.AdvertisementService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdvertisementServiceImpl implements AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final AdCategoryRepository adCategoryRepository;

    public AdvertisementServiceImpl(AdvertisementRepository advertisementRepository,
                                     AdCategoryRepository adCategoryRepository) {
        this.advertisementRepository = advertisementRepository;
        this.adCategoryRepository = adCategoryRepository;
    }

    @Override
    public List<Advertisement> listActiveAds(Long categoryId) {
        if (categoryId != null) {
            return advertisementRepository.findByCategoryIdAndStatusOrderBySortOrderAsc(categoryId, 1);
        }
        return advertisementRepository.findByStatusOrderBySortOrderAsc(1);
    }

    @Override
    public List<Advertisement> adminListAds() {
        return advertisementRepository.findAll();
    }

    @Override
    public Advertisement createAd(AdAdminRequest request) {
        Advertisement ad = new Advertisement();
        applyRequest(ad, request);
        return advertisementRepository.save(ad);
    }

    @Override
    public Advertisement updateAd(Long id, AdAdminRequest request) {
        Advertisement ad = advertisementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "广告不存在"));
        applyRequest(ad, request);
        return advertisementRepository.save(ad);
    }

    @Override
    public void deleteAd(Long id) {
        if (!advertisementRepository.existsById(id)) {
            throw new BusinessException(404, "广告不存在");
        }
        advertisementRepository.deleteById(id);
    }

    private void applyRequest(Advertisement ad, AdAdminRequest request) {
        ad.setCategoryId(request.getCategoryId());
        ad.setTitle(request.getTitle());
        ad.setImageUrl(request.getImageUrl());
        ad.setLinkUrl(request.getLinkUrl());
        ad.setSortOrder(request.getSortOrder());
        ad.setStartTime(request.getStartTime());
        ad.setEndTime(request.getEndTime());
        ad.setStatus(request.getStatus());
    }

    @Override
    public List<AdCategory> listAdCategories() {
        return adCategoryRepository.findAll();
    }

    @Override
    public AdCategory createAdCategory(String name) {
        AdCategory category = new AdCategory();
        category.setName(name);
        category.setStatus(1);
        return adCategoryRepository.save(category);
    }
}
