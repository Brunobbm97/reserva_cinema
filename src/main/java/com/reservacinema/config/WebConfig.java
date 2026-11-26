package com.reservacinema.config;

import com.reservacinema.common.storage.StorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Faz as imagens salvas no disco serem servidas pela URL pública /files/**.
 * Ex.: o arquivo uploads/posters/uuid.jpg fica acessível em /files/posters/uuid.jpg.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String posterDir;

    public WebConfig(StorageProperties properties) {
        this.posterDir = properties.posterDir();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // A pasta de pôsteres fica dentro de uploads/; servimos toda a pasta uploads/ em /files/.
        Path uploadsRoot = Path.of(posterDir).toAbsolutePath().normalize().getParent();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(uploadsRoot.toUri().toString());
    }
}
