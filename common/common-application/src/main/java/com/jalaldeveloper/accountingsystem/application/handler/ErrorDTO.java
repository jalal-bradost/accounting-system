package com.jalaldeveloper.accountingsystem.application.handler;

public class ErrorDTO {
    private String code;
    private String message;

    public ErrorDTO() {}

    public ErrorDTO(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ErrorDTOBuilder builder() {
        return new ErrorDTOBuilder();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static final class ErrorDTOBuilder {
        private String code;
        private String message;

        public ErrorDTOBuilder code(String code) { this.code = code; return this; }
        public ErrorDTOBuilder message(String message) { this.message = message; return this; }
        public ErrorDTO build() { return new ErrorDTO(code, message); }
    }
}