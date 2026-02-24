package net.Gabou.identity2.progression;

import java.util.HashMap;
import java.util.Map;

public final class ProgressionChargeCodec {
    private ProgressionChargeCodec() {
    }

    public static String serialize(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            int amount = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (amount <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('=').append(amount);
        }
        return builder.toString();
    }

    public static Map<String, Integer> deserialize(String serialized) {
        Map<String, Integer> out = new HashMap<>();
        if (serialized == null || serialized.isBlank()) {
            return out;
        }
        String[] pairs = serialized.split(";");
        for (String pair : pairs) {
            if (pair == null || pair.isBlank()) {
                continue;
            }
            int split = pair.indexOf('=');
            if (split <= 0 || split >= pair.length() - 1) {
                continue;
            }
            String key = pair.substring(0, split).trim();
            String raw = pair.substring(split + 1).trim();
            if (key.isEmpty() || raw.isEmpty()) {
                continue;
            }
            try {
                int amount = Integer.parseInt(raw);
                if (amount > 0) {
                    out.put(key, amount);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }
}
