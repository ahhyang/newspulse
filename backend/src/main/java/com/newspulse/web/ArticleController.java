package com.newspulse.web;

import com.newspulse.domain.Sentiment;
import com.newspulse.dto.ArticleResponse;
import com.newspulse.dto.PageResponse;
import com.newspulse.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@Tag(name = "Articles")
public class ArticleController {

	private final ArticleService articleService;

	public ArticleController(ArticleService articleService) {
		this.articleService = articleService;
	}

	@GetMapping
	@Operation(summary = "List articles with optional filters")
	public PageResponse<ArticleResponse> search(
			@RequestParam(required = false) Long topicId,
			@RequestParam(required = false) String source,
			@RequestParam(required = false) Sentiment sentiment,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return articleService.search(topicId, source, sentiment, from, to, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a single article")
	public ArticleResponse get(@PathVariable Long id) {
		return articleService.get(id);
	}
}
