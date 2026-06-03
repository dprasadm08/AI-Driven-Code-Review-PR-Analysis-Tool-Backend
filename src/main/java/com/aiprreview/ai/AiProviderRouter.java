package com.aiprreview.ai;

import com.aiprreview.config.AiProviderConfig;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiProviderRouter {

    private final Map<String, AiProvider> providers;
    private final AiProviderConfig aiProviderConfig;

    public AiProviderRouter(List<AiProvider> providers, AiProviderConfig aiProviderConfig) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        provider -> provider.getProviderName().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));
        this.aiProviderConfig = aiProviderConfig;
    }

    public AiProvider resolveProvider(String preferredProvider) {
        String selected = normalize(preferredProvider);
        if (!selected.isBlank() && providers.containsKey(selected)) {
            return providers.get(selected);
        }

        String configured = normalize(aiProviderConfig.getProvider());
        if (providers.containsKey(configured)) {
            return providers.get(configured);
        }

        if (providers.containsKey("openai")) {
            return providers.get("openai");
        }

        throw new IllegalStateException("No AI providers available");
    }

    public List<String> getAvailableProviders() {
        return providers.keySet().stream().sorted().toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
