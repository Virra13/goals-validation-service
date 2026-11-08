package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.entity.KkdRule;

import java.util.Optional;

/**
 * Репозиторий справочника правил ККД.
 * Позволяет находить запись справочника по коду правила.
 */
public interface KkdRuleRepository extends JpaRepository<KkdRule, Long> {
    Optional<KkdRule> findByCode(String code);
}
