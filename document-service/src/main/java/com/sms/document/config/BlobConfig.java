package com.sms.document.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties({StorageProperties.class, DocumentProperties.class})
public class BlobConfig {

    @Bean
    BlobContainerClient blobContainerClient(StorageProperties props) {
        BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        if (StringUtils.hasText(props.connectionString())) {
            builder.connectionString(props.connectionString());               // Azurite / key based
        } else if (StringUtils.hasText(props.endpoint())) {
            builder.endpoint(props.endpoint())                                // Managed Identity in Azure
                    .credential(new DefaultAzureCredentialBuilder().build());
        } else {
            throw new IllegalStateException(
                    "Set STORAGE_CONNECTION_STRING (local) or STORAGE_ENDPOINT (Azure, managed identity)");
        }
        BlobContainerClient container = builder.buildClient().getBlobContainerClient(props.container());
        if (props.createContainer()) {
            container.createIfNotExists();
        }
        return container;
    }
}
