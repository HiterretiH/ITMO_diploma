package com.logistic.backend.api.dto;

import org.springframework.core.io.Resource;

public record DocumentDownload(Resource resource, String filename, String contentType) {}
