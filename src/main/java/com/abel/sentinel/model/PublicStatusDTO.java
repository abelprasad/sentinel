package com.abel.sentinel.model;

import java.util.List;

public record PublicStatusDTO(
        long activeTracksNow,
        long anomaliesLastHour,
        long totalEntities,
        List<RecentAnomaly> recentAnomalies,
        List<PublicPosition> positions
) {
    public record RecentAnomaly(
            String callsign,
            String icaoHex,
            String classification,
            double score,
            String explanation,
            String flaggedAt
    ) {}

    public record PublicPosition(
            long entityId,
            String callsign,
            String icaoHex,
            double lat,
            double lon,
            Double altitude,
            Double speed,
            Double heading,
            boolean anomalous,
            double score,
            String classification
    ) {}
}