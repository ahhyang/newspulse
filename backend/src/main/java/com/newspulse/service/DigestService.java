package com.newspulse.service;

import com.newspulse.dto.DigestResponse;
import com.newspulse.mapper.NewsPulseMapper;
import com.newspulse.repository.DigestRepository;
import com.newspulse.web.NotFoundException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DigestService {

	private final DigestRepository digestRepository;

	public DigestService(DigestRepository digestRepository) {
		this.digestRepository = digestRepository;
	}

	public DigestResponse getLatest(Long topicId) {
		return (topicId == null
				? digestRepository.findFirstByOrderByDigestDateDescGeneratedAtDesc()
				: digestRepository.findFirstByTopicIdOrderByDigestDateDescGeneratedAtDesc(topicId))
				.map(NewsPulseMapper::toResponse)
				.orElseThrow(() -> new NotFoundException("No digest has been generated yet"));
	}

	public DigestResponse getByDate(LocalDate date, Long topicId) {
		if (topicId == null) {
			throw new NotFoundException("Query parameter topicId is required when fetching a digest by date");
		}
		return digestRepository.findByTopicIdAndDigestDate(topicId, date)
				.map(NewsPulseMapper::toResponse)
				.orElseThrow(() -> new NotFoundException("No digest found for %s".formatted(date)));
	}
}
