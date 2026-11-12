package com.sms.document.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import com.sms.common.web.NotFoundException;
import com.sms.common.web.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class BlobStorageService {

    private static final Logger log = LoggerFactory.getLogger(BlobStorageService.class);

    private final BlobContainerClient container;

    public BlobStorageService(BlobContainerClient container) {
        this.container = container;
    }

    public void upload(String blobName, InputStream data, long size, String contentType) {
        try {
            BlobParallelUploadOptions options = new BlobParallelUploadOptions(BinaryData.fromStream(data, size))
                    .setHeaders(new BlobHttpHeaders().setContentType(contentType));
            container.getBlobClient(blobName).uploadWithResponse(options, null, null);
        } catch (BlobStorageException e) {
            throw new UpstreamException("Could not store the file in blob storage", e);
        }
    }

    public InputStream open(String blobName) {
        try {
            return container.getBlobClient(blobName).openInputStream();
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                throw new NotFoundException("File content not found");
            }
            throw new UpstreamException("Could not read the file from blob storage", e);
        }
    }

    public void deleteQuietly(String blobName) {
        try {
            container.getBlobClient(blobName).deleteIfExists();
        } catch (RuntimeException e) {
            log.warn("Could not clean up blob {}", blobName, e);
        }
    }
}
