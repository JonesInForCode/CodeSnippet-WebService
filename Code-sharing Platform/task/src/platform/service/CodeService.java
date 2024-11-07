package platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import platform.exception.CodeSnippetNotFoundException;
import platform.model.CodeSnippet;
import platform.repository.CodeRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CodeService {

    private final CodeRepository codeRepository;

    @Autowired
    public CodeService(CodeRepository codeRepository) {
        this.codeRepository = codeRepository;
    }

    public CodeSnippet getCodeSnippet(UUID id) {
        CodeSnippet snippet = codeRepository.findById(id)
                .orElseThrow(() -> new CodeSnippetNotFoundException("Code snippet not found with id: " + id));

        if (snippet.isExpired()) {
            codeRepository.delete(snippet);
            throw new CodeSnippetNotFoundException("Code snippet has expired");
        }

        snippet.updateAndReturn();
        codeRepository.save(snippet);

        return snippet;
    }


    public UUID addCodeSnippet(String code, long time, int views) {
        CodeSnippet snippet = new CodeSnippet(code, time, views);
        return codeRepository.save(snippet).getId();
    }

    public List<CodeSnippet> getLatestUnrestrictedSnippets() {
        return codeRepository.findTop10ByRestrictedFalseOrderByDateDesc();
    }

    public void saveCodeSnippet(CodeSnippet snippet) {
        codeRepository.save(snippet);
    }
}