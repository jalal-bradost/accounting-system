package com.bradox.erp.sign.service.domain;

import com.bradox.erp.sign.service.domain.dto.PageImage;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Rendered page images, keyed by source hash, page and scale, so a PDF page is drawn once (NFR performance). */
@Component
class PageCache {

    private static final int MAX_ENTRIES = 120;

    private final Map<String, PageImage> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PageImage> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    PageImage get(String sha256, int page, float scale, Supplier<PageImage> render) {
        String key = sha256 + ":" + page + ":" + scale;
        synchronized (cache) {
            PageImage hit = cache.get(key);
            if (hit != null) {
                return hit;
            }
        }
        PageImage image = render.get();
        synchronized (cache) {
            cache.put(key, image);
        }
        return image;
    }
}
