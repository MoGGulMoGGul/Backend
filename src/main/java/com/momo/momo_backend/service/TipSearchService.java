package com.momo.momo_backend.service;

import com.momo.momo_backend.dto.TipResponse;
import com.momo.momo_backend.entity.*;
import com.momo.momo_backend.repository.GroupMemberRepository;
import com.momo.momo_backend.repository.GroupRepository;
import com.momo.momo_backend.repository.StorageRepository;
import com.momo.momo_backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipSearchService {

    @PersistenceContext
    private final EntityManager em;

    private final StorageRepository storageRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    /* ============================ Public ============================ */
    public List<TipResponse> searchPublic(String keyword, String mode, int page, int size) {
        String base = """
            SELECT DISTINCT t
            FROM Tip t
              JOIN t.storageTips st
              JOIN FETCH t.user u
            WHERE t.isPublic = true
        """;
        var params = new HashMap<String, Object>();
        String jpql = appendKeywordClause(base, "t", keyword, mode, params)
                + " ORDER BY t.createdAt DESC";
        return queryTips(jpql, params, page, size);
    }

    /* ============================ My (user-wide) ============================ */
    public List<TipResponse> searchMy(Long userNo, String keyword, String mode, int page, int size) {
        String base = """
            SELECT DISTINCT t
            FROM Tip t
              JOIN t.storageTips st
              JOIN st.storage s
              JOIN FETCH t.user u
            WHERE s.user.no = :userNo
        """;
        var params = new HashMap<String, Object>();
        params.put("userNo", userNo);
        String jpql = appendKeywordClause(base, "t", keyword, mode, params)
                + " ORDER BY t.createdAt DESC";
        return queryTips(jpql, params, page, size);
    }

    /* ============================ Group ============================ */
    public List<TipResponse> searchGroup(Long groupNo, Long requestingUserNo,
                                         String keyword, String mode, int page, int size) {

        Group group = groupRepository.findById(groupNo)
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));
        User requester = userRepository.findById(requestingUserNo)
                .orElseThrow(() -> new IllegalArgumentException("요청 사용자가 존재하지 않습니다."));

        if (!groupMemberRepository.existsByGroupAndUser(group, requester)) {
            throw new AccessDeniedException("그룹 멤버만 그룹 검색이 가능합니다.");
        }

        String base = """
            SELECT DISTINCT t
            FROM Tip t
              JOIN t.storageTips st
              JOIN st.storage s
              JOIN FETCH t.user u
            WHERE s.group.no = :groupNo
        """;
        var params = new HashMap<String, Object>();
        params.put("groupNo", groupNo);
        String jpql = appendKeywordClause(base, "t", keyword, mode, params)
                + " ORDER BY t.createdAt DESC";
        return queryTips(jpql, params, page, size);
    }

    /* ============================ Storage ============================ */
    public List<TipResponse> searchStorage(Long storageNo, Long requestingUserNo,
                                           String keyword, String mode, int page, int size) {

        Storage storage = storageRepository.findById(storageNo)
                .orElseThrow(() -> new IllegalArgumentException("보관함이 존재하지 않습니다."));
        User requester = userRepository.findById(requestingUserNo)
                .orElseThrow(() -> new IllegalArgumentException("요청 사용자가 존재하지 않습니다."));

        if (storage.getGroup() == null) {
            if (!Objects.equals(storage.getUser().getNo(), requestingUserNo)) {
                throw new AccessDeniedException("개인 보관함은 소유자만 조회할 수 있습니다.");
            }
        } else {
            if (!groupMemberRepository.existsByGroupAndUser(storage.getGroup(), requester)) {
                throw new AccessDeniedException("그룹 보관함은 해당 그룹 멤버만 조회할 수 있습니다.");
            }
        }

        String base = """
            SELECT DISTINCT t
            FROM StorageTip st
              JOIN st.tip t
              JOIN FETCH t.user u
            WHERE st.storage.no = :storageNo
        """;
        var params = new HashMap<String, Object>();
        params.put("storageNo", storageNo);
        String jpql = appendKeywordClause(base, "t", keyword, mode, params)
                + " ORDER BY t.createdAt DESC";
        return queryTips(jpql, params, page, size);
    }

    /* ============================ Tag ============================ */
    public List<TipResponse> searchByTag(String tagName, String keyword, String mode, int page, int size) {
        String base = """
            SELECT DISTINCT t
            FROM Tip t
              JOIN t.storageTips st
              JOIN t.tipTags tt
              JOIN tt.tag tag
              JOIN FETCH t.user u
            WHERE tag.name = :tagName
              AND t.isPublic = true
        """;
        var params = new HashMap<String, Object>();
        params.put("tagName", tagName);
        String jpql = appendKeywordClause(base, "t", keyword, mode, params)
                + " ORDER BY t.createdAt DESC";
        return queryTips(jpql, params, page, size);
    }

    /* ============================ 내부 유틸 ============================ */

    private List<TipResponse> queryTips(String jpql, Map<String, Object> params, int page, int size) {
        var q = em.createQuery(jpql, Tip.class);
        params.forEach(q::setParameter);
        if (page >= 0 && size > 0) {
            q.setFirstResult(page * size);
            q.setMaxResults(size);
        }
        List<Tip> tips = q.getResultList();
        return tips.stream()
                .map(TipResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * keyword를 공백으로 분리해 OR / AND 로 묶어 JPQL에 추가.
     * - LOWER(COALESCE(field,'')) LIKE :wX ESCAPE '\\'
     * - %/_를 이스케이프 처리하여 오동작 방지
     */
    private String appendKeywordClause(String base, String alias,
                                       String keyword, String mode,
                                       Map<String, Object> params) {
        if (keyword == null || keyword.isBlank()) return base;

        String[] words = Arrays.stream(keyword.trim().split("\\s+"))
                .filter(s -> !s.isBlank()).distinct().toArray(String[]::new);
        if (words.length == 0) return base;

        boolean andMode = "AND".equalsIgnoreCase(mode);

        StringBuilder sb = new StringBuilder(base);
        sb.append(" AND (");

        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(andMode ? " AND " : " OR ");
            String p = "w" + i;
            sb.append("(")
                    .append("LOWER(COALESCE(").append(alias).append(".title,'')) LIKE :").append(p).append(" ESCAPE '\\' ")
                    .append("OR LOWER(COALESCE(").append(alias).append(".contentSummary,'')) LIKE :").append(p).append(" ESCAPE '\\' ")
                    .append("OR LOWER(COALESCE(").append(alias).append(".url,'')) LIKE :").append(p).append(" ESCAPE '\\'")
                    .append(")");
            params.put(p, toLikeParam(words[i]));
        }
        sb.append(")");
        return sb.toString();
    }

    private String toLikeParam(String raw) {
        String escaped = raw.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
