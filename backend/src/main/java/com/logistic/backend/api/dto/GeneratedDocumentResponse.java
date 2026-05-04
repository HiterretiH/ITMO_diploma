package com.logistic.backend.api.dto;

import com.logistic.backend.document.DocumentType;
import com.logistic.backend.document.FileFormat;
import java.time.Instant;

public record GeneratedDocumentResponse(
        Long id, DocumentType documentType, FileFormat fileFormat, String sha256, Instant createdAt) {}
