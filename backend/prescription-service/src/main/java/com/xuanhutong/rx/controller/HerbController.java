package com.xuanhutong.rx.controller;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuanhutong.common.dto.response.ApiResponse;
import com.xuanhutong.rx.dto.request.HerbRequest;
import com.xuanhutong.rx.entity.Herb;
import com.xuanhutong.rx.service.HerbService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    @PostMapping public ApiResponse<Herb> create(@Valid @RequestBody HerbRequest r) { return ApiResponse.success(service.createHerb(r)); }
    @PutMapping("/{id}") public ApiResponse<Herb> update(@PathVariable Long id, @Valid @RequestBody HerbRequest r) { return ApiResponse.success(service.updateHerb(id, r)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable Long id) { service.deleteHerb(id); return ApiResponse.success(); }
    @PostMapping("/batch") public ApiResponse<Integer> batchCreate(@RequestBody List<HerbRequest> requests) { return ApiResponse.success(service.batchCreateHerbs(requests)); }
}
