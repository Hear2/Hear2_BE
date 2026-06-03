package com.hear2.judge.support;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.regex.Pattern;

public final class JudgeParticipantFormatter {

    private static final Pattern MY_PREFIX_PATTERN = Pattern.compile("^(A|나|내가|저|제가)(는|은|가|이)?\\s*");
    private static final Pattern PARTNER_PREFIX_PATTERN = Pattern.compile("^(B|상대|상대방|파트너)(는|은|가|이)?\\s*");

    private JudgeParticipantFormatter() {
    }

    public static String formatMySummary(String summary, String userName) {
        return formatSummary(summary, userName, MY_PREFIX_PATTERN);
    }

    public static String formatPartnerSummary(String summary, String partnerName) {
        return formatSummary(summary, partnerName, PARTNER_PREFIX_PATTERN);
    }

    public static String normalizeRequesterSummary(String summary, String requesterName) {
        return normalizeSummary(summary, MY_PREFIX_PATTERN, requesterName, "요청자");
    }

    public static String normalizePartnerSummary(String summary, String partnerName) {
        return normalizeSummary(summary, PARTNER_PREFIX_PATTERN, partnerName, "상대", "상대방", "파트너");
    }

    public static String formatSharedSummary(String summary, String displayName) {
        if (!StringUtils.hasText(summary)) {
            return summary;
        }

        String strippedSummary = normalizeSummary(summary, null, displayName);
        if (!StringUtils.hasText(strippedSummary)) {
            return displayName;
        }
        return displayName + topicParticle(displayName) + " " + strippedSummary;
    }

    private static String formatSummary(String summary, String displayName, Pattern prefixPattern) {
        if (!StringUtils.hasText(summary)) {
            return summary;
        }

        String strippedSummary = normalizeSummary(summary, prefixPattern, displayName);
        if (!StringUtils.hasText(strippedSummary)) {
            return displayName;
        }
        return displayName + topicParticle(displayName) + " " + strippedSummary;
    }

    private static String normalizeSummary(String summary, Pattern prefixPattern, String... aliases) {
        if (!StringUtils.hasText(summary)) {
            return summary;
        }

        String normalized = summary.trim();
        if (prefixPattern != null) {
            normalized = prefixPattern.matcher(normalized).replaceFirst("").trim();
        }

        for (String alias : Arrays.stream(aliases).filter(StringUtils::hasText).map(String::trim).distinct().toList()) {
            normalized = stripParticipantAlias(normalized, alias);
        }

        return normalized;
    }

    private static String stripParticipantAlias(String text, String alias) {
        String normalized = text;
        for (String suffix : new String[]{"는", "은", "가", "이"}) {
            normalized = stripPrefix(normalized, alias + suffix);
        }
        return stripPrefix(normalized, alias);
    }

    private static String stripPrefix(String text, String prefix) {
        if (text.equals(prefix)) {
            return "";
        }
        if (text.startsWith(prefix + " ")) {
            return text.substring(prefix.length()).trim();
        }
        return text;
    }

    private static String topicParticle(String value) {
        if (!StringUtils.hasText(value)) {
            return "는";
        }

        char lastChar = value.charAt(value.length() - 1);
        if (lastChar < 0xAC00 || lastChar > 0xD7A3) {
            return "는";
        }

        int jongseongIndex = (lastChar - 0xAC00) % 28;
        return jongseongIndex == 0 ? "는" : "은";
    }
}
