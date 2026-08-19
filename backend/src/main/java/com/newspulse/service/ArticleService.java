package com.newspulse.service;

import com.newspulse.domain.Sentiment;
import com.newspulse.dto.ArticleResponse;
import com.newspulse.dto.PageResponse;
import com.newspulse.mapper.NewsPulseMapper;
import com.newspulse.repository.ArticleRepository;
import com.newspulse.repository.ArticleSpecifications;
import com.newspulse.web.NotFoundException;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ArticleService {

	private final ArticleRepository articleRepository;

	public ArticleService(ArticleRepository articleRepository) {
		this.articleRepository = articleRepository;
	}

	public PageResponse<ArticleResponse> search(
			Long topicId,
			String source,
			Sentiment sentiment,
			Instant from,
			Instant to,
			Pageable pageable
	) {
		Page<ArticleResponse> page = articleRepository
				.findAll(ArticleSpecifications.withFilters(topicId, source, sentiment, from, to), pageable)
				.map(NewsPulseMapper::toResponse);
		return PageResponse.from(page);
	}

	public ArticleResponse get(Long id) {
		return articleRepository.findWithDetailsById(id)
				.map(NewsPulseMapper::toResponse)
				.orElseThrow(() -> new NotFoundException("Article %d was not found".formatted(id)));
	}
}
