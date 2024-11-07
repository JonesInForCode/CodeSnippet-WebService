package platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import platform.model.CodeSnippet;

import java.util.List;
import java.util.UUID;

public interface CodeRepository extends JpaRepository<CodeSnippet, UUID> {
    List<CodeSnippet> findTop10ByRestrictedFalseOrderByDateDesc();
}