package com.logistic.backend.api.dto;

import com.logistic.backend.document.DocumentType;
import com.logistic.backend.document.FileFormat;
import java.util.List;

public record OrderDocumentDescriptor(
        DocumentType documentType, List<FileFormat> formats, boolean requiresCompleteData) {}
