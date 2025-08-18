package com.company.parser.core;

import com.company.parser.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class PriceSelectorService {

    private static final Logger log = LoggerFactory.getLogger(PriceSelectorService.class);

    private final AppProperties props;

    public PriceSelectorService(AppProperties props) {
        this.props = props;
    }

    public Optional<BigDecimal> selectMinValid(List<BigDecimal> candidates,
                                               Competitor comp,
                                               Category cat,
                                               SizeKey size) {
        if (candidates == null || candidates.isEmpty()) return Optional.empty();

        // getMinPrice()/getMaxPrice() уже BigDecimal
        final BigDecimal min = props.getMinPrice();
        final BigDecimal max = props.getMaxPrice();

        var filtered = candidates.stream()
                .filter(p -> p != null)
                .filter(p -> p.compareTo(min) >= 0)
                .filter(p -> p.compareTo(max) <= 0)
                .sorted(Comparator.naturalOrder())
                .toList();

        log.info("[SELECT] {} {} {} raw={}, filtered(min..max)={}",
                comp.title(), cat, size, candidates, filtered);

        if (filtered.isEmpty()) return Optional.empty();
        return Optional.of(filtered.getFirst());
    }

    public Optional<BigDecimal> selectFromVariants(List<PriceVariant> variants,
                                                   Competitor comp,
                                                   Category cat,
                                                   SizeKey size) {
        if (variants == null || variants.isEmpty()) return Optional.empty();
        var candidates = variants.stream()
                .map(PriceVariant::price)
                .toList();
        return selectMinValid(candidates, comp, cat, size);
    }
}
