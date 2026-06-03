package com.aiprreview.ai;

public class AiResponseParser {

	private AiResponseParser() {
	}

	public static String extractSummary(String content, int maxChars) {
		if (content == null || content.isBlank()) {
			return "";
		}
		if (content.length() <= maxChars) {
			return content;
		}
		return content.substring(0, maxChars) + "...";
	}
}
