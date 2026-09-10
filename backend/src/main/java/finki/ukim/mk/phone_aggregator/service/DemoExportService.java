package finki.ukim.mk.phone_aggregator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import finki.ukim.mk.phone_aggregator.model.OfferListing;
import finki.ukim.mk.phone_aggregator.model.PhoneModel;
import finki.ukim.mk.phone_aggregator.repository.OfferListingRepository;
import finki.ukim.mk.phone_aggregator.repository.PhoneModelRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the static-demo-mode fixtures under frontend/public/demo-data/ from whatever is
 * currently in the database - a real, representative subset (not fabricated data), so the
 * frontend can run standalone (VITE_DATA_MODE=static, e.g. on Vercel) with no backend at all.
 * See ./clients/staticDataClient.js on the frontend for how these files get consumed, and the
 * root README's "Regenerating the demo fixtures" section for how to invoke this.
 * <p>
 * Only phones.json's listings are real. price-history.json is always synthetic - real price
 * history in this dataset is never more than a day or two deep, which makes an uninteresting
 * chart, so a 60-day series is generated instead (seeded per model+source, so re-running the
 * export against an unchanged database doesn't reshuffle the demo charts every time).
 */
@Service
public class DemoExportService {

    /** Cap on how many distinct PhoneModels go into the fixture set - keeps it small enough
     * to ship in a frontend bundle while still being "representative". */
    private static final int MAX_MODELS = 120;

    /** Soft per-brand cap during the first selection pass, so one brand with hundreds of
     * listings can't crowd out every other brand in the demo set. */
    private static final int MAX_PER_BRAND = 25;

    private static final int HISTORY_DAYS = 60;

    private static final String LOCAL_IMAGE_MARKER = "/product-images/";

    /** Product photos are shown as small thumbnails/cards - no need to ship them at their
     * original catalog resolution. Longest edge is capped to this many pixels. */
    private static final int MAX_IMAGE_DIMENSION = 480;

    /** Re-encoded as JPEG regardless of source format: every card in this app renders images
     * on a white background anyway, so flattening a PNG's alpha onto white loses nothing
     * visible, and JPEG compresses these product photos far better than PNG does. */
    private static final float JPEG_QUALITY = 0.82f;

    private final OfferListingRepository offerListingRepository;
    private final PhoneModelRepository phoneModelRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    public DemoExportService(OfferListingRepository offerListingRepository,
                              PhoneModelRepository phoneModelRepository,
                              ResourceLoader resourceLoader) {
        this.offerListingRepository = offerListingRepository;
        this.phoneModelRepository = phoneModelRepository;
        this.resourceLoader = resourceLoader;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public record ExportResult(int modelCount, int offerCount, String outputDir) {
    }

    @Transactional(readOnly = true)
    public ExportResult export(Path outputDir) throws IOException {
        List<OfferListing> allListings = offerListingRepository.findAll();

        List<Long> modelIds = selectRepresentativeModelIds(allListings);
        Set<Long> modelIdSet = new HashSet<>(modelIds);

        List<OfferListing> selectedListings = dedupeListings(allListings.stream()
                .filter(listing -> modelIdSet.contains(listing.getPhoneModelId()))
                .toList());

        Map<Long, PhoneModel> modelsById = phoneModelRepository.findAllById(modelIds).stream()
                .collect(Collectors.toMap(PhoneModel::getId, m -> m));

        Files.createDirectories(outputDir);
        Path imagesDir = outputDir.resolve("images");
        clearDirectory(imagesDir);
        Files.createDirectories(imagesDir);

        List<Map<String, Object>> phonesJson = new ArrayList<>();
        for (OfferListing listing : selectedListings) {
            PhoneModel model = modelsById.get(listing.getPhoneModelId());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", listing.getId());
            entry.put("phoneModelId", listing.getPhoneModelId());
            entry.put("modelKey", model != null ? model.getModelKey() : null);
            entry.put("brand", listing.getBrand());
            entry.put("title", listing.getTitle());
            entry.put("rawTitle", listing.getRawTitle());
            entry.put("siteLink", listing.getSiteLink());
            entry.put("price", listing.getPrice());
            entry.put("imageUrl", rewriteImageUrl(listing.getImageUrl(), imagesDir));
            entry.put("source", listing.getSource());
            entry.put("createdAt", listing.getCreatedAt());
            entry.put("colorRaw", listing.getColorRaw());
            entry.put("colorCanonical", listing.getColorCanonical());
            entry.put("storageGb", listing.getStorageGb());
            entry.put("ramGb", listing.getRamGb());
            phonesJson.add(entry);
        }

        Map<String, Object> priceHistoryJson = buildSyntheticPriceHistory(selectedListings);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("exportedAt", LocalDateTime.now());
        meta.put("modelCount", modelIds.size());
        meta.put("offerCount", selectedListings.size());

        var writer = objectMapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(outputDir.resolve("phones.json").toFile(), phonesJson);
        writer.writeValue(outputDir.resolve("price-history.json").toFile(), priceHistoryJson);
        writer.writeValue(outputDir.resolve("meta.json").toFile(), meta);

        return new ExportResult(modelIds.size(), selectedListings.size(), outputDir.toAbsolutePath().normalize().toString());
    }

    /**
     * Collapses offers that would render as visually identical cards in the listing (same
     * source, title, price and image - nothing else shows up there, not even color) down to
     * one, keeping the lowest id for determinism. This is a demo-fixture-only cleanup - the
     * live database and API are untouched. A known ledikom scraper quirk (its per-color
     * variant-URL resolution sometimes falls back to the base product page for more than one
     * color in the same run, and/or the matching pipeline occasionally splits one real
     * product into two PhoneModel rows) creates dozens of these in the live data; fixing
     * that at the source is a separate, riskier change, so for now the fixture just hides
     * the symptom. Deliberately not keyed on phoneModelId or color - two rows that look
     * identical in the list should collapse even if they came from different underlying
     * models/colors, since the list view never shows either.
     */
    private List<OfferListing> dedupeListings(List<OfferListing> listings) {
        Map<String, OfferListing> bestByKey = new LinkedHashMap<>();
        for (OfferListing listing : listings) {
            String key = String.join("|",
                    listing.getSource(),
                    String.valueOf(listing.getTitle()),
                    String.valueOf(listing.getPrice()),
                    String.valueOf(listing.getImageUrl()));
            OfferListing existing = bestByKey.get(key);
            if (existing == null || listing.getId() < existing.getId()) {
                bestByKey.put(key, listing);
            }
        }
        return new ArrayList<>(bestByKey.values());
    }

    /**
     * Picks a representative slice of PhoneModels: multi-offer models first (more
     * interesting for the price-comparison demo), spread across brands rather than letting
     * one brand's long tail dominate, capped at MAX_MODELS overall.
     */
    private List<Long> selectRepresentativeModelIds(List<OfferListing> allListings) {
        Map<Long, List<OfferListing>> byModel = allListings.stream()
                .collect(Collectors.groupingBy(OfferListing::getPhoneModelId));

        List<Map.Entry<Long, List<OfferListing>>> sortedByOfferCount = byModel.entrySet().stream()
                .sorted((a, b) -> b.getValue().size() - a.getValue().size())
                .toList();

        Map<String, Integer> brandCounts = new HashMap<>();
        List<Long> selected = new ArrayList<>();

        for (Map.Entry<Long, List<OfferListing>> entry : sortedByOfferCount) {
            if (selected.size() >= MAX_MODELS) {
                break;
            }
            String brand = entry.getValue().get(0).getBrand();
            int countSoFar = brandCounts.getOrDefault(brand, 0);
            if (countSoFar >= MAX_PER_BRAND) {
                continue;
            }
            selected.add(entry.getKey());
            brandCounts.put(brand, countSoFar + 1);
        }

        if (selected.size() < MAX_MODELS) {
            Set<Long> already = new HashSet<>(selected);
            for (Map.Entry<Long, List<OfferListing>> entry : sortedByOfferCount) {
                if (selected.size() >= MAX_MODELS) {
                    break;
                }
                if (already.add(entry.getKey())) {
                    selected.add(entry.getKey());
                }
            }
        }

        return selected;
    }

    /**
     * Copies a locally-hosted product image (served from this backend's own
     * static/product-images/, see scraper's BACKEND_IMAGE_BASE_URL) into the fixture's
     * images/ folder - downscaled and re-encoded as JPEG, since these are shipped inside a
     * git repo and the originals are full catalog-resolution PNGs/JPEGs far bigger than the
     * thumbnail/card sizes this app actually displays them at - and returns a
     * backend-independent relative URL for it. Externally hosted images (most sources) are
     * left pointing at the original retailer URL.
     */
    private String rewriteImageUrl(String imageUrl, Path imagesDir) {
        if (imageUrl == null || !imageUrl.contains(LOCAL_IMAGE_MARKER)) {
            return imageUrl;
        }

        String originalFilename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Resource resource = resourceLoader.getResource("classpath:static/product-images/" + originalFilename);
        if (!resource.exists()) {
            return imageUrl;
        }

        String baseName = originalFilename.contains(".")
                ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                : originalFilename;
        String outputFilename = baseName + ".jpg";

        try (InputStream in = resource.getInputStream()) {
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                return imageUrl;
            }
            writeResizedJpeg(original, imagesDir.resolve(outputFilename));
        } catch (IOException e) {
            return imageUrl;
        }

        return "/demo-data/images/" + outputFilename;
    }

    private void writeResizedJpeg(BufferedImage original, Path outputPath) throws IOException {
        int width = original.getWidth();
        int height = original.getHeight();
        double scale = Math.min(1.0, (double) MAX_IMAGE_DIMENSION / Math.max(width, height));
        int targetWidth = Math.max(1, Math.round((float) (width * scale)));
        int targetHeight = Math.max(1, Math.round((float) (height * scale)));

        // TYPE_INT_RGB has no alpha channel, so drawing a transparent PNG onto it flattens
        // it onto whatever we fill the background with first - white, matching every
        // product card's own background in this app.
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, targetWidth, targetHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputPath.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(resized, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(dir))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup - a leftover stale file just gets overwritten
                            // or ignored on the next export.
                        }
                    });
        }
    }

    private Map<String, Object> buildSyntheticPriceHistory(List<OfferListing> selectedListings) {
        Map<Long, List<OfferListing>> byModel = selectedListings.stream()
                .collect(Collectors.groupingBy(OfferListing::getPhoneModelId));

        Map<String, Object> history = new LinkedHashMap<>();
        for (Map.Entry<Long, List<OfferListing>> modelEntry : byModel.entrySet()) {
            Map<String, List<OfferListing>> bySource = modelEntry.getValue().stream()
                    .collect(Collectors.groupingBy(OfferListing::getSource, LinkedHashMap::new, Collectors.toList()));

            List<Map<String, Object>> series = new ArrayList<>();
            for (Map.Entry<String, List<OfferListing>> sourceEntry : bySource.entrySet()) {
                int anchorPrice = sourceEntry.getValue().stream()
                        .mapToInt(OfferListing::getPrice)
                        .min()
                        .orElseThrow();

                Map<String, Object> seriesEntry = new LinkedHashMap<>();
                seriesEntry.put("source", sourceEntry.getKey());
                seriesEntry.put("points", generateSyntheticSeries(anchorPrice, modelEntry.getKey(), sourceEntry.getKey()));
                series.add(seriesEntry);
            }
            history.put(String.valueOf(modelEntry.getKey()), series);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("synthetic", true);
        result.put("note", "Real price history in this dataset is only ever a day or two deep. "
                + "This 60-day series is synthetically generated from the latest real price, for demo purposes only.");
        result.put("history", history);
        return result;
    }

    /** Deterministic per (model, source) so re-exporting an unchanged database is stable. */
    private List<Map<String, Object>> generateSyntheticSeries(int anchorPrice, long modelId, String source) {
        Random random = new Random(Objects.hash(modelId, source));

        double[] prices = new double[HISTORY_DAYS];
        prices[HISTORY_DAYS - 1] = anchorPrice;
        for (int i = HISTORY_DAYS - 2; i >= 0; i--) {
            double dailyDrift = (random.nextDouble() - 0.5) * 0.03; // +/- 1.5% day over day
            prices[i] = prices[i + 1] * (1 - dailyDrift);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < HISTORY_DAYS; i++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", now.minusDays(HISTORY_DAYS - 1L - i));
            point.put("price", (int) (Math.round(prices[i] / 10.0) * 10));
            points.add(point);
        }
        // Anchor the most recent point exactly to the real latest price.
        points.get(points.size() - 1).put("price", anchorPrice);
        return points;
    }
}
