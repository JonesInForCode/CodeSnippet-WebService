package platform.exception;

public class CodeSnippetNotFoundException extends RuntimeException {
    public CodeSnippetNotFoundException(String message) {
        super(message);
    }
}