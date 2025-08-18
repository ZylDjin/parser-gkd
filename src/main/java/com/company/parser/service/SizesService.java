package com.company.parser.service;

import com.company.parser.core.Category;
import com.company.parser.core.Competitor;
import com.company.parser.core.SizeKey;
import com.company.parser.core.SizesConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class SizesService {
    private final ResourceLoader loader;

    public SizesService(ResourceLoader loader) {
        this.loader = loader;
    }

    public SizesConfig load(String resourceLocation) throws Exception {
        Resource r = loader.getResource(resourceLocation);
        if (!r.exists()) throw new IllegalArgumentException("sizes.yml not found: " + resourceLocation);
        if (resourceLocation.startsWith("file:")) {
            return SizesConfig.load(Path.of(r.getURI()));
        }
        try (InputStream is = r.getInputStream()) {
            return SizesConfig.load(is);
        }
    }

    public List<SizeKey> sizesUnion(SizesConfig cfg, Category cat) {
        return cfg.sizesUnion(cat);
    }

    public boolean hasSize(SizesConfig cfg, Competitor c, Category cat, SizeKey s) {
        return cfg.hasSize(c, cat, s);
    }

    public Set<Competitor> enabledCompetitors(List<String> names) {
        Set<Competitor> set = new LinkedHashSet<>();
        if (names != null) {
            for (String n : names) {
                try { set.add(Competitor.valueOf(n)); } catch (Exception ignore) {}
            }
        }
        return set;
    }
}
