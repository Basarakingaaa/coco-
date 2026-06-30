package com.mall.repository;

import com.mall.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findByCategoryIdAndStatusOrderBySortOrderAsc(Long categoryId, Integer status);
    List<Advertisement> findByStatusOrderBySortOrderAsc(Integer status);
}
