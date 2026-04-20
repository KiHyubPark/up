package com.clone.up.domain.up.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
// @RestController  : @Controller + @ResponseBody. 반환값을 JSON으로 직렬화해서 응답 바디에 담음 (REST API 용)
@RestController
// @RequiredArgsConstructor : final 필드를 파라미터로 받는 생성자를 자동 생성
//   → 아래처럼 직접 생성자를 쓰는 것과 동일
//   public UpController(UpService upService) { this.upService = upService; }
//   → Spring이 해당 생성자를 보고 UpService를 자동으로 주입 (생성자 주입)
@RequiredArgsConstructor
@RequestMapping("/api/v1/up")
public class UpController {
}
