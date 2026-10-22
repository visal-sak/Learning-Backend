package co.istad.visal.e_learning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded files from disk. Without this, media.base-uri +
 * media.client-path produces a URL with nothing behind it — the upload
 * succeeds and every thumbnail 404s.
 */
@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    @Value("${media.location}")
    private String mediaLocation;

    @Value("${media.client-path}")
    private String mediaClientPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = mediaLocation.endsWith("/") || mediaLocation.endsWith("\\")
                ? mediaLocation
                : mediaLocation + "/";

        registry.addResourceHandler(mediaClientPath + "/**")
                .addResourceLocations("file:" + location);
    }
}