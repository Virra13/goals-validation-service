package ru.virra.kkd.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.virra.kkd.entity.KkdRule;

import java.util.List;
import java.util.Optional;

public interface KkdRuleRepository extends JpaRepository<KkdRule, Long> {
    Optional<KkdRule> findByCode(String code);
    List<KkdRule> findAllByActiveTrue();
}