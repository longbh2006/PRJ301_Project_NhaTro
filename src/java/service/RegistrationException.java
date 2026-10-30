package service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RegistrationException extends Exception {

    private static final long serialVersionUID = 1L;
    private final Map<String, String> errors;

    public RegistrationException(Map<String, String> errors) {
        super("Registration data is invalid.");
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
