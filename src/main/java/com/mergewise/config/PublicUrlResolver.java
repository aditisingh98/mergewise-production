package com.mergewise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlResolver {

    @Value("${mergewise.public-url:}")
    private String configuredPublicUrl;

    public String resolve() {
        if (configuredPublicUrl != null && !configuredPublicUrl.isBlank()
                && !configuredPublicUrl.contains("localhost")) {
            return trimTrailingSlash(configuredPublicUrl);
        }

        String render = env("RENDER_EXTERNAL_URL");
        if (render != null) {
            return trimTrailingSlash(render);
        }

        String railway = env("RAILWAY_PUBLIC_DOMAIN");
        if (railway != null) {
            return "https://" + railway.replaceAll("^https?://", "");
        }

        String fly = env("FLY_APP_NAME");
        if (fly != null) {
            return "https://" + fly + ".fly.dev";
        }

        String heroku = env("HEROKU_APP_NAME");
        if (heroku != null) {
            return "https://" + heroku + ".herokuapp.com";
        }

        String aws = env("AWS_APP_RUNNER_SERVICE_URL");
        if (aws != null) {
            return trimTrailingSlash(aws);
        }

        if (configuredPublicUrl != null && !configuredPublicUrl.isBlank()) {
            return trimTrailingSlash(configuredPublicUrl);
        }

        return "http://localhost:8090";
    }

    private String env(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String trimTrailingSlash(String url) {
        return url.replaceAll("/+$", "");
    }
}
