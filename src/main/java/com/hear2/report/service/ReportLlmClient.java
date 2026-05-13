package com.hear2.report.service;

import java.util.Optional;

public interface ReportLlmClient {

    boolean isAvailable();

    Optional<String> generateJson(String prompt);
}
