package com.unicorn.journey.assistant.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "STT", url = "${doubao.sst.host}")
public interface ByteDanceRecognizeClient {


    @PostMapping("/recognize/flash")
    SSTResponse recognizeFlash(
            @RequestHeader("X-Api-App-Key") String appId,
            @RequestHeader("X-Api-Access-Key") String accessKey,
            @RequestHeader("X-Api-Resource-Id") String resourceId,
            @RequestHeader("X-Api-Sequence") int sequence,
            @RequestBody AudioRequest audioRequest);
}
