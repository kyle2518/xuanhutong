package com.xuanhutong.rx.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.rx.entity.Herb;
import com.xuanhutong.rx.service.HerbService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/herbs") @RequiredArgsConstructor
public class HerbController {
    private final HerbService service;
    @GetMapping
    public ApiResponse<Page<Herb>> search(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category, @RequestParam(defaultValue = "1") int p, @RequestParam(defaultValue = "50") int s) {
        return ApiResponse.success(service.searchHerbs(keyword, category, p, s));
    }
    @GetMapping("/{id}") public ApiResponse<Herb> get(@PathVariable Long id) { return ApiResponse.success(service.getHerb(id)); }
    @GetMapping("/{id}/tooltip") public ApiResponse<String> tooltip(@PathVariable Long id) { return ApiResponse.success(service.getHerbTooltip(id)); }
}
