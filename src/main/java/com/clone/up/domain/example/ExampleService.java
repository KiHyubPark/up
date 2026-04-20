package com.clone.up.domain.example;

import com.clone.up.client.ExampleApiClient;
import com.clone.up.domain.example.dto.ExampleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleApiClient exampleApiClient;

    public ExampleResponse getExample(Long id) {
        return exampleApiClient.getExample(id);
    }
}
