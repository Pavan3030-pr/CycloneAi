package com.enterprise.cyclone.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves the single-page console from the same origin as the API, when it is present.
 *
 * <p>Why this exists: a browser console that calls a different origin needs a CORS policy, a public
 * API hostname and a build-time base URL, all of which have to be correct at deploy time or the demo
 * fails in front of an audience. Shipping the built site inside the service removes the whole class
 * of problem — one container, one URL, relative {@code /api} calls, no CORS preflight, nothing to
 * misconfigure. The Vite dev server still talks to {@code localhost:8080} cross-origin in
 * development, so the API's CORS policy stays exercised during normal work.
 *
 * <p>The forward is registered only when a built site is actually on the classpath. The API-only
 * image (see {@code backend/Dockerfile}) contains no {@code static/index.html}, so it registers
 * nothing and keeps its previous behaviour exactly; there is no flag to set incorrectly.
 *
 * <p>Paths are enumerated rather than wildcarded on purpose. A catch-all forward would swallow
 * unknown {@code /api} paths and turn a developer's typo into an HTML 200 response instead of a JSON
 * 404, which is a genuinely confusing failure to debug. The routes below are the ones {@code App.tsx}
 * declares.
 */
@Configuration
public class StaticSiteConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(StaticSiteConfiguration.class);

    /** Router paths in the SPA that must resolve to the shell rather than to a 404. */
    private static final String[] SPA_ROUTES = {"/signin", "/signup", "/app", "/app/**"};

    private static final String INDEX = "static/index.html";

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        if (!new ClassPathResource(INDEX).exists()) {
            LOG.info("No bundled site found at {}, so this instance serves the API only.", INDEX);
            return;
        }

        // The shell itself, plus every client-side route.
        registry.addViewController("/").setViewName("forward:/index.html");
        for (String route : SPA_ROUTES) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
        LOG.info("Bundled site detected: the console is served from this origin at /");
    }
}
