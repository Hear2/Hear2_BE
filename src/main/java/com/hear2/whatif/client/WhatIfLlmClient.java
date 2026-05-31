package com.hear2.whatif.client;

import java.util.Optional;

public interface WhatIfLlmClient {

    boolean isAvailable();

    String model();

    Optional<String> generateJson(String prompt);
}
