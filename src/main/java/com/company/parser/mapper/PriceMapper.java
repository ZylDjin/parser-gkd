package com.company.parser.mapper;

import com.company.parser.model.domain.PriceHistory;
import com.company.parser.model.domain.PriceRecord;
import com.company.parser.model.domain.Product;
import com.company.parser.model.domain.ProductPrice;
import com.company.parser.model.dto.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Маппер для работы с ценами
 */
@Component
public class PriceMapper {

    /**
     * Конвертация PriceRecord в PriceDTO
     */
    public PriceDTO toDto(PriceRecord record) {
        if (record == null) return null;

        return PriceDTO.builder()
                .siteId(record.getSiteId())
                .productName(record.getProductName())
                .size(record.getSize())
                .thickness(record.getThickness())
                .steelGrade(record.getSteelGrade())
                .length(record.getLength())
                .price(record.getPrice())
                .currency(record.getCurrency())
                .unit(record.getUnit())
                .url(record.getUrl())
                .timestamp(record.getTimestamp())
                .build();
    }

    /**
     * Конвертация списка PriceRecord в список PriceDTO
     */
    public List<PriceDTO> toDtoList(List<PriceRecord> records) {
        if (records == null) return List.of();

        return records.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Конвертация PriceDTO в PriceRecord
     */
    public PriceRecord toEntity(PriceDTO dto) {
        if (dto == null) return null;

        return PriceRecord.builder()
                .siteId(dto.getSiteId())
                .productName(dto.getProductName())
                .size(dto.getSize())
                .thickness(dto.getThickness())
                .steelGrade(dto.getSteelGrade())
                .length(dto.getLength())
                .price(dto.getPrice())
                .currency(dto.getCurrency())
                .unit(dto.getUnit())
                .url(dto.getUrl())
                .timestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now())
                .build();
    }

    /**
     * Создание PriceChangeDTO из старой и новой записи
     */
    public PriceChangeDTO toPriceChangeDto(PriceRecord oldRecord, PriceRecord newRecord,
                                           String changeType) {
        return PriceChangeDTO.builder()
                .siteId(newRecord.getSiteId())
                .siteName(newRecord.getSiteName())
                .productName(newRecord.getProductName())
                .size(newRecord.getSize())
                .thickness(newRecord.getThickness())
                .oldPrice(oldRecord != null ? oldRecord.getPrice() : null)
                .newPrice(newRecord.getPrice())
                .changeAmount(oldRecord != null ?
                        newRecord.getPrice().subtract(oldRecord.getPrice()) : newRecord.getPrice())
                .changeType(changeType)
                .changeDate(Instant.now())
                .url(newRecord.getUrl())
                .build();
    }

    /**
     * Конвертация Product в список PriceRecord
     */
    public List<PriceRecord> fromProduct(Product product) {
        if (product == null || product.getPricesBySite() == null) {
            return List.of();
        }

        return product.getPricesBySite().entrySet().stream()
                .map(entry -> fromProductPrice(product, entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * Создание PriceRecord из ProductPrice
     */
    private PriceRecord fromProductPrice(Product product, String siteId, ProductPrice price) {
        return PriceRecord.builder()
                .siteId(siteId)
                .siteName(price.getSiteName())
                .productName(product.getName())
                .size(product.getSize())
                .thickness(product.getThickness())
                .steelGrade(product.getSteelGrade())
                .length(product.getLength())
                .price(price.getPrice())
                .currency(price.getCurrency())
                .unit(price.getUnit())
                .url(price.getUrl())
                .timestamp(price.getLastUpdated())
                .build();
    }

    /**
     * Группировка PriceRecord в Product
     */
    public Product toProduct(List<PriceRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        PriceRecord first = records.get(0);

        Map<String, ProductPrice> pricesBySite = records.stream()
                .collect(Collectors.toMap(
                        PriceRecord::getSiteId,
                        this::toProductPrice,
                        (existing, replacement) -> replacement
                ));

        return Product.builder()
                .name(first.getProductName())
                .size(first.getSize())
                .thickness(first.getThickness())
                .steelGrade(first.getSteelGrade())
                .length(first.getLength())
                .pricesBySite(pricesBySite)
                .build();
    }

    /**
     * Конвертация PriceRecord в ProductPrice
     */
    private ProductPrice toProductPrice(PriceRecord record) {
        return ProductPrice.builder()
                .siteId(record.getSiteId())
                .siteName(record.getSiteName())
                .price(record.getPrice())
                .currency(record.getCurrency())
                .unit(record.getUnit())
                .url(record.getUrl())
                .lastUpdated(record.getTimestamp())
                .available(true)
                .build();
    }

    /**
     * Конвертация PriceHistory в PriceChangeDTO
     */
    public PriceChangeDTO fromHistory(PriceHistory history) {
        if (history == null) return null;

        return PriceChangeDTO.builder()
                .siteId(history.getSiteId())
                .productName(history.getProductName())
                .oldPrice(history.getPreviousPrice())
                .newPrice(history.getPrice())
                .changeAmount(history.getPriceChange())
                .changePercent(history.getPriceChangePercent())
                .changeType(history.getChangeType())
                .changeDate(history.getTimestamp())
                .build();
    }
}
