package com.aiprreview.ai;

import com.aiprreview.dto.pullrequest.PullRequestFileResponse;
import com.aiprreview.dto.pullrequest.PullRequestWithFilesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PromptBuilderService {

	private static final int CHARS_PER_TOKEN_ESTIMATE = 4;
	private static final String TEMPLATE_FALLBACK = """
			Repository: {{repositoryFullName}}
			PR: #{{prNumber}} - {{title}}
			Base: {{baseBranch}}
			Head: {{headBranch}}
			Author: {{author}}

			Description:
			{{description}}

			Files:
			{{filesSummary}}

			Diff:
			{{diffInput}}
			""";

	@Value("classpath:prompts/pr-review-template.txt")
	private Resource promptTemplateResource;

	@Value("${app.ai.prompt.max-tokens:7000}")
	private int maxPromptTokens;

	@Value("${app.ai.prompt.reserved-output-tokens:1000}")
	private int reservedOutputTokens;

	public PromptBuildResult buildPrompt(PullRequestWithFilesResponse pullRequest) {
		String template = loadTemplate();
		String filesSummary = buildFilesSummary(pullRequest.getFiles());
		String formattedDiff = formatPrDiff(pullRequest.getFiles(), pullRequest.getDiff());

		String prompt = template
				.replace("{{repositoryFullName}}", safe(pullRequest.getRepositoryFullName()))
				.replace("{{prNumber}}", String.valueOf(pullRequest.getPrNumber()))
				.replace("{{title}}", safe(pullRequest.getTitle()))
				.replace("{{baseBranch}}", safe(pullRequest.getBaseBranch()))
				.replace("{{headBranch}}", safe(pullRequest.getHeadBranch()))
				.replace("{{author}}", safe(pullRequest.getAuthor()))
				.replace("{{description}}", safe(pullRequest.getDescription()))
				.replace("{{filesSummary}}", filesSummary)
				.replace("{{diffInput}}", formattedDiff);

		int allowedPromptTokens = Math.max(1000, maxPromptTokens - reservedOutputTokens);
		int estimatedTokens = estimateTokens(prompt);

		if (estimatedTokens <= allowedPromptTokens) {
			return new PromptBuildResult(prompt, estimatedTokens, false, 0);
		}

		String truncatedDiff = truncateDiffToTokenBudget(formattedDiff, allowedPromptTokens, prompt.length() - formattedDiff.length());
		String truncatedPrompt = prompt.replace(formattedDiff, truncatedDiff);
		int finalTokens = estimateTokens(truncatedPrompt);
		int omittedChars = Math.max(0, formattedDiff.length() - truncatedDiff.length());

		log.info("Prompt exceeded token budget. originalTokens={}, allowedTokens={}, finalTokens={}, omittedChars={}",
				estimatedTokens, allowedPromptTokens, finalTokens, omittedChars);

		return new PromptBuildResult(truncatedPrompt, finalTokens, true, omittedChars);
	}

	public String formatPrDiff(List<PullRequestFileResponse> files, String fullDiff) {
		if (fullDiff != null && !fullDiff.isBlank()) {
			return fullDiff;
		}

		if (files == null || files.isEmpty()) {
			return "No diff content was provided.";
		}

		StringBuilder builder = new StringBuilder();
		for (PullRequestFileResponse file : files) {
			builder.append("diff --git a/")
					.append(safe(file.getFilename()))
					.append(" b/")
					.append(safe(file.getFilename()))
					.append('\n');
			builder.append("status: ").append(safe(file.getStatus()))
					.append(" (+").append(file.getAdditions() != null ? file.getAdditions() : 0)
					.append(" -").append(file.getDeletions() != null ? file.getDeletions() : 0)
					.append(")\n");
			builder.append(safe(file.getPatch())).append("\n\n");
		}
		return builder.toString();
	}

	private String buildFilesSummary(List<PullRequestFileResponse> files) {
		if (files == null || files.isEmpty()) {
			return "No changed files metadata available.";
		}

		List<String> rows = new ArrayList<>();
		for (PullRequestFileResponse file : files) {
			rows.add("- " + safe(file.getFilename())
					+ " [" + safe(file.getStatus()) + "]"
					+ " additions=" + (file.getAdditions() != null ? file.getAdditions() : 0)
					+ " deletions=" + (file.getDeletions() != null ? file.getDeletions() : 0));
		}
		return String.join("\n", rows);
	}

	private String truncateDiffToTokenBudget(String diffInput, int allowedPromptTokens, int nonDiffCharsCount) {
		int nonDiffTokens = Math.max(1, nonDiffCharsCount / CHARS_PER_TOKEN_ESTIMATE);
		int remainingDiffTokens = Math.max(500, allowedPromptTokens - nonDiffTokens);
		int remainingDiffChars = remainingDiffTokens * CHARS_PER_TOKEN_ESTIMATE;

		if (diffInput.length() <= remainingDiffChars) {
			return diffInput;
		}

		String truncated = diffInput.substring(0, remainingDiffChars);
		return truncated + "\n\n[TRUNCATED] Diff was shortened to stay within token limits.";
	}

	private int estimateTokens(String content) {
		return Math.max(1, content.length() / CHARS_PER_TOKEN_ESTIMATE);
	}

	private String loadTemplate() {
		try {
			byte[] bytes = promptTemplateResource.getInputStream().readAllBytes();
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException ex) {
			log.warn("Failed to load prompt template from resources. Using fallback template. reason={}", ex.getMessage());
			return TEMPLATE_FALLBACK;
		}
	}

	private String safe(String value) {
		return Objects.toString(value, "");
	}

	public record PromptBuildResult(String prompt, int estimatedTokens, boolean truncated, int omittedChars) {
	}
}
