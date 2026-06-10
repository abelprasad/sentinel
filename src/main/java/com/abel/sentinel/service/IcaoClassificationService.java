package com.abel.sentinel.service;

import org.springframework.stereotype.Service;

@Service
public class IcaoClassificationService {

    /**
     * Classifies an aircraft based on its ICAO 24-bit hex address prefix.
     *
     * ICAO address blocks are allocated by country/organization:
     *   A00000-AFFFFF  -- United States civil
     *   ADF000-AFFFFF  -- overlaps US block, AE prefix specifically US Military
     *   800000-83FFFF  -- India
     *   400000-43FFFF  -- United Kingdom
     *   380000-3BFFFF  -- France
     *   3C0000-3FFFFF  -- Germany
     *   340000-37FFFF  -- Spain
     *   300000-33FFFF  -- Italy
     *   700000-73FFFF  -- Russia
     *   780000-7BFFFF  -- China
     *   C00000-C3FFFF  -- Canada
     *   E00000-E3FFFF  -- Argentina/Brazil region
     */
    public String classify(String icaoHex) {
        if (icaoHex == null || icaoHex.length() < 2) return "UNKNOWN";

        String hex = icaoHex.toUpperCase().trim();

        // US Military -- AE prefix is specifically allocated to US DoD
        if (hex.startsWith("AE")) return "MILITARY";

        // US civil
        if (hex.startsWith("A")) return "CIVIL-US";

        // Canada
        if (hex.startsWith("C")) return "CIVIL-CA";

        // United Kingdom
        if (inRange(hex, "400000", "43FFFF")) return "CIVIL-UK";

        // France
        if (inRange(hex, "380000", "3BFFFF")) return "CIVIL-FR";

        // Germany
        if (inRange(hex, "3C0000", "3FFFFF")) return "CIVIL-DE";

        // Spain
        if (inRange(hex, "340000", "37FFFF")) return "CIVIL-ES";

        // Italy
        if (inRange(hex, "300000", "33FFFF")) return "CIVIL-IT";

        // Russia
        if (inRange(hex, "700000", "73FFFF")) return "CIVIL-RU";

        // China
        if (inRange(hex, "780000", "7BFFFF")) return "CIVIL-CN";

        // India
        if (inRange(hex, "800000", "83FFFF")) return "CIVIL-IN";

        // Latin America rough block
        if (hex.startsWith("E")) return "CIVIL-LATAM";

        return "UNKNOWN";
    }

    private boolean inRange(String hex, String low, String high) {
        try {
            long val  = Long.parseLong(hex, 16);
            long lo   = Long.parseLong(low, 16);
            long hi   = Long.parseLong(high, 16);
            return val >= lo && val <= hi;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}