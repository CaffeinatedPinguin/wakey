package dev.wakey.checks.internal;

import dev.wakey.checks.Check;
import dev.wakey.checks.CheckFilter;
import dev.wakey.checks.CheckPosition;
import dev.wakey.checks.CheckState;
import dev.wakey.checks.CheckType;
import dev.wakey.checks.PausedReason;
import dev.wakey.checks.StateCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;

/** Builds the filtered list and the four state counters with Spring Data criteria, never SQL. */
@Component
@RequiredArgsConstructor
public class CheckQueries {

    private final CheckTagRepository tags;

    private final JdbcAggregateOperations aggregates;

    List<Check> list(UUID spaceId, CheckFilter filter, @Nullable CheckPosition after, int rowLimit) {
        Criteria criteria = filterCriteria(spaceId, filter);
        Query query = pageQuery(criteria, after, rowLimit);
        return withTags(findRows(query));
    }

    StateCounts counts(UUID spaceId, CheckFilter filter) {
        Criteria criteria = filterCriteria(spaceId, filter);
        long up = count(criteria, CheckState.UP);
        long degraded = count(criteria, CheckState.DEGRADED);
        long down = count(criteria, CheckState.DOWN);
        long unknown = count(criteria, CheckState.UNKNOWN);
        return new StateCounts(up, degraded, down, unknown);
    }

    List<CheckEntity> entities(UUID spaceId, List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Criteria criteria = Criteria.where("spaceId").is(spaceId).and("id").in(ids);
        return entities(Query.query(criteria));
    }

    List<CheckEntity> entities(UUID spaceId, CheckFilter filter) {
        Criteria criteria = filterCriteria(spaceId, filter);
        return entities(Query.query(criteria));
    }

    private List<CheckEntity> entities(Query query) {
        List<CheckRow> rows = findRows(query);
        if (rows.isEmpty()) {
            return List.of();
        }
        return CheckMapper.toEntities(rows, tagsOf(rows));
    }

    /**
     * Deletes the roots by id in one statement. {@link CheckRow} has no mapped collection, so Spring
     * Data JDBC skips the per-root lock it would otherwise take for the tag rows; the foreign keys
     * cascade the tags and results.
     */
    void deleteByIds(List<UUID> ids) {
        aggregates.deleteAllById(ids, CheckRow.class);
    }

    private Criteria filterCriteria(UUID spaceId, CheckFilter filter) {
        Criteria criteria = Criteria.where("spaceId").is(spaceId);
        @Nullable String q = filter.q();
        if (q != null && !q.isBlank()) {
            criteria = criteria.and(textCriteria(q));
        }
        @Nullable CheckState state = filter.state();
        if (state != null) {
            criteria = criteria.and("lastState").is(state);
        }
        @Nullable CheckType type = filter.type();
        if (type != null) {
            criteria = criteria.and("type").is(type);
        }
        @Nullable String group = filter.group();
        if (group != null) {
            criteria = criteria.and("groupName").is(group);
        }
        @Nullable PausedReason pausedReason = filter.pausedReason();
        if (pausedReason != null) {
            criteria = criteria.and("pausedReason").is(pausedReason);
        }
        @Nullable String tag = filter.tag();
        if (tag != null) {
            criteria = criteria.and(tagCriteria(spaceId, tag));
        }
        return criteria;
    }

    private static Criteria textCriteria(String q) {
        String pattern = likePattern(q);
        Criteria name = Criteria.where("name").like(pattern).ignoreCase(true);
        Criteria target = Criteria.where("target").like(pattern).ignoreCase(true);
        return name.or(target);
    }

    private Criteria tagCriteria(UUID spaceId, String tag) {
        List<UUID> ids = tagIds(spaceId, tag);
        if (ids.isEmpty()) {
            return Criteria.where("id").isNull();
        }
        return Criteria.where("id").in(ids);
    }

    private List<UUID> tagIds(UUID spaceId, String tag) {
        List<CheckTagRow> rows = tags.findBySpaceIdAndKeyTag(spaceId, tag);
        List<UUID> ids = new ArrayList<>(rows.size());
        for (CheckTagRow row : rows) {
            CheckTagKey key = row.key();
            ids.add(key.checkId());
        }
        return ids;
    }

    private static Query pageQuery(Criteria criteria, @Nullable CheckPosition after, int rowLimit) {
        Criteria page = criteria;
        if (after != null) {
            Criteria laterName = Criteria.where("name").greaterThan(after.name());
            Criteria sameName =
                    Criteria.where("name").is(after.name()).and("id").greaterThan(after.id());
            page = page.and(laterName.or(sameName));
        }
        Sort sort = Sort.by("name", "id");
        return Query.query(page).sort(sort).limit(rowLimit);
    }

    private long count(Criteria criteria, CheckState state) {
        Criteria stateCriteria = criteria.and("lastState").is(state);
        Query query = Query.query(stateCriteria);
        return aggregates.count(query, CheckEntity.class);
    }

    private List<Check> withTags(List<CheckRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        return CheckMapper.toChecks(rows, tagsOf(rows));
    }

    private List<CheckTagRow> tagsOf(List<CheckRow> rows) {
        List<UUID> checkIds = new ArrayList<>(rows.size());
        for (CheckRow row : rows) {
            checkIds.add(row.id());
        }
        return tags.findByKeyCheckIdIn(checkIds);
    }

    private List<CheckRow> findRows(Query query) {
        List<CheckRow> rows = new ArrayList<>();
        Iterable<CheckRow> found = aggregates.findAll(query, CheckRow.class);
        found.forEach(rows::add);
        return rows;
    }

    private static String likePattern(String q) {
        String escaped = escapeLike(q.strip());
        return "%" + escaped + "%";
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
