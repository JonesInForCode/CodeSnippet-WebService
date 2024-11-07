package platform.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platform.exception.CodeSnippetNotFoundException;
import platform.model.CodeSnippet;
import platform.service.CodeService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class CodeController {

    private static final Logger logger = LoggerFactory.getLogger(CodeController.class);

    private final CodeService codeService;

    @Autowired
    public CodeController(CodeService codeService) {
        this.codeService = codeService;
    }

    @PostMapping(value = "/api/code/new", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> addNewCode(@RequestBody Map<String, Object> payload) {
        String code = (String) payload.get("code");
        long time = Long.parseLong(payload.getOrDefault("time", "0").toString());
        int views = Integer.parseInt(payload.getOrDefault("views", "0").toString());

        UUID id = codeService.addCodeSnippet(code, time, views);
        return ResponseEntity.ok(Map.of("id", id.toString()));
    }

    @GetMapping(value = "/code/new", produces = MediaType.TEXT_HTML_VALUE)
    public String getNewCodeForm() {
        return """
           <!DOCTYPE html>
           <html>
           <head>
               <title>Create</title>
           </head>
           <body>
               <textarea id="code_snippet"></textarea>
               <input id="time_restriction" type="text" placeholder="Time restriction (seconds)">
               <input id="views_restriction" type="text" placeholder="Views restriction">
               <button id="send_snippet" type="submit" onclick="send()">Submit</button>
               
               <script>
               function send() {
                   let object = {
                       "code": document.getElementById("code_snippet").value,
                       "time": parseInt(document.getElementById("time_restriction").value) || 0,
                       "views": parseInt(document.getElementById("views_restriction").value) || 0
                   };
                   
                   fetch('/api/code/new', {
                       method: 'POST',
                       headers: {
                           'Content-Type': 'application/json'
                       },
                       body: JSON.stringify(object)
                   })
                   .then(response => response.json())
                   .then(data => alert("Success! ID: " + data.id))
                   .catch(error => alert("Error: " + error));
               }
               </script>
           </body>
           </html>
           """;
    }

    @GetMapping(value = "/api/code/latest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CodeSnippetResponse>> getLatestSnippets() {
        List<CodeSnippet> latestSnippets = codeService.getLatestUnrestrictedSnippets();
        List<CodeSnippetResponse> response = latestSnippets.stream()
                .map(snippet -> new CodeSnippetResponse(snippet))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/code/latest", produces = MediaType.TEXT_HTML_VALUE)
    public String getLatestSnippetsHtml() {
        List<CodeSnippet> latestSnippets = codeService.getLatestUnrestrictedSnippets();
        StringBuilder snippetsHtml = new StringBuilder();

        for (CodeSnippet snippet : latestSnippets) {
            snippetsHtml.append(String.format("""
                <span id="load_date">%s</span>
                <pre id="code_snippet"><code>%s</code></pre>
                <br>
                """, formatDate(snippet.getDate()), snippet.getCode()));
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Latest</title>
                <link rel="stylesheet" href="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.2.1/build/styles/default.min.css">
                <script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.2.1/build/highlight.min.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>
            </head>
            <body>
                <h1>Latest</h1>
                %s
            </body>
            </html>
            """, snippetsHtml);
    }


    @GetMapping(value = "/code/{uuid}", produces = MediaType.TEXT_HTML_VALUE)
    public String getCodeHtml(@PathVariable UUID uuid) {
        CodeSnippet snippet = codeService.getCodeSnippet(uuid);

        String formattedDate = formatDate(snippet.getDate());

        StringBuilder restrictionsHtml = new StringBuilder();
        if (snippet.isRestrictedByTime()) {
            restrictionsHtml.append(String.format("<span id=\"time_restriction\">The code will be available for %d seconds</span><br>", snippet.getRemainingTime()));
        }
        if (snippet.isRestrictedByViews()) {
            restrictionsHtml.append(String.format("<span id=\"views_restriction\">%d more views allowed</span><br>", snippet.getViews()));
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Code</title>
                <link rel="stylesheet" href="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.2.1/build/styles/default.min.css">
                <script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@10.2.1/build/highlight.min.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>
            </head>
            <body>
                <span id="load_date">%s</span>
                %s
                <pre id="code_snippet"><code>%s</code></pre>
            </body>
            </html>
            """, formattedDate, restrictionsHtml, snippet.getCode());
    }


    @GetMapping(value = "/api/code/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CodeSnippetResponse> getCodeSnippet(@PathVariable UUID uuid) {
        try {
            CodeSnippet snippet = codeService.getCodeSnippet(uuid);

            if (snippet.isExpired() ||
                snippet.isRestrictedByTime() && snippet.getRemainingTime() <= 0 ||
                snippet.isRestrictedByViews() && snippet.getViews() <= 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            CodeSnippetResponse response = new CodeSnippetResponse(snippet);
            return ResponseEntity.ok(response);
        } catch (CodeSnippetNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    private static String formatDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static class CodeSnippetResponse {
        private final String code;
        private final String date;
        private final long time;
        private final int views;

        public CodeSnippetResponse(CodeSnippet snippet) {
            this.code = snippet.getCode();
            this.date = formatDate(snippet.getDate());
            this.time = snippet.isRestrictedByTime() ? snippet.getRemainingTime() : 0;
            this.views = snippet.isRestrictedByViews() ? snippet.getViews() : 0;
        }

        private static String formatDate(LocalDateTime dateTime) {
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        public String getCode() {
            return code;
        }

        public String getDate() {
            return date;
        }

        public long getTime() {
            return time;
        }

        public int getViews() {
            return views;
        }
    }

    @ExceptionHandler(CodeSnippetNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleCodeSnippetNotFound(CodeSnippetNotFoundException ex) {
        return ex.getMessage();
    }
}