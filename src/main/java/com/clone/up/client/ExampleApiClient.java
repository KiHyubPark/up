package com.clone.up.client;

import com.clone.up.domain.example.dto.ExampleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "example-api", url = "${feign.example-api.url}")
public interface ExampleApiClient {

    @GetMapping("/example")
    ExampleResponse getExample(@RequestParam("id") Long id);
}
