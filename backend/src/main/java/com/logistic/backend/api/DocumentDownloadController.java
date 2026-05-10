package com.logistic.backend.api;

import com.logistic.backend.api.dto.DocumentDownload;
import com.logistic.backend.order.OrderService;
import com.logistic.backend.security.CurrentUserService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/generated-documents")
@RequiredArgsConstructor
public class DocumentDownloadController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    @GetMapping("/{id}/file")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id)
            throws IOException {
        currentUserService.requireUser();
        DocumentDownload d = orderService.prepareDocumentDownload(id);
        ContentDisposition disposition =
                ContentDisposition.attachment().filename(d.filename()).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CONTENT_TYPE, d.contentType())
                .body(d.resource());
    }
}
