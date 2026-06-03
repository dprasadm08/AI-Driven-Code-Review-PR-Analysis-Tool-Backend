package com.aiprreview.ai;

import java.util.Map;

public interface AiProvider {

	String getProviderName();

	AiProviderResponse analyze(String prompt);

	ConnectivityResult testConnectivity();

	record AiProviderResponse(String provider, String content, Map<String, Object> usage) {
	}

	record ConnectivityResult(boolean success, String provider, String model, String message, String rawReply) {
		public static ConnectivityResult success(String provider, String model, String rawReply) {
			return new ConnectivityResult(true, provider, model, "Connected successfully", rawReply);
		}

		public static ConnectivityResult failure(String provider, String model, String message) {
			return new ConnectivityResult(false, provider, model, message, null);
		}
	}
}
