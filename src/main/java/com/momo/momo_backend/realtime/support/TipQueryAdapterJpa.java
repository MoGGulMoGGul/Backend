package com.momo.momo_backend.realtime.support;

import com.momo.momo_backend.entity.Tag;
import com.momo.momo_backend.entity.Tip;
import com.momo.momo_backend.entity.TipTag;
import com.momo.momo_backend.repository.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TipQueryAdapterJpa implements TipQueryPort {

    private final TipRepository tipRepository;

    @Override
    @Transactional(readOnly = true)
    public TipSummaryView findSummaryById(Long tipId) {
        Tip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팁입니다. id=" + tipId));

        String author = tip.getUser() != null
                ? (tip.getUser().getNickname() != null ? tip.getUser().getNickname()
                : String.valueOf(tip.getUser().getNo()))
                : null;

        List<String> tags = tip.getTipTags() == null ? List.of()
                : tip.getTipTags().stream()
                .map(TipTag::getTag)
                .map(Tag::getName)
                .toList();

        Instant created = tip.getCreatedAt() == null
                ? Instant.now()
                : tip.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();

        return new TipSummaryView(
                tip.getNo(),
                tip.getTitle(),
                author,
                tags,
                created,
                tip.getThumbnailUrl()
        );
    }
}
