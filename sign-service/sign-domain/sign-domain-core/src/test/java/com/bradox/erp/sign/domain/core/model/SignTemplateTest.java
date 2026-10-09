package com.bradox.erp.sign.domain.core.model;

import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import com.bradox.erp.sign.domain.core.valueobject.FieldType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignTemplateTest {

    private static SignTemplate.Field field(int page, FieldType type) {
        return new SignTemplate.Field(UUID.randomUUID(), page, 0.1, 0.1, 0.3, 0.05, type);
    }

    private static SignTemplate template(List<SignTemplate.Field> fields) {
        return new SignTemplate(UUID.randomUUID(), UUID.randomUUID(), " Contract ", UUID.randomUUID(), "abc", 2, fields, Instant.now());
    }

    @Test
    void trimsTheName() {
        assertThat(template(List.of()).name()).isEqualTo("Contract");
    }

    @Test
    void needsAName() {
        assertThatThrownBy(() -> template(List.of()).edit(" ", List.of())).isInstanceOf(SignDomainException.class);
    }

    @Test
    void fieldsMustBeOnAnExistingPageAndInsideIt() {
        assertThatThrownBy(() -> template(List.of(field(3, FieldType.TEXT)))).isInstanceOf(SignDomainException.class);
        assertThatThrownBy(() -> new SignTemplate.Field(UUID.randomUUID(), 1, 0.9, 0.1, 0.3, 0.05, FieldType.TEXT))
                .isInstanceOf(SignDomainException.class);
    }

    @Test
    void signingNeedsASignatureBox() {
        assertThatThrownBy(() -> template(List.of(field(1, FieldType.NAME))).checkCanSign()).isInstanceOf(SignDomainException.class);
        assertThatCode(() -> template(List.of(field(2, FieldType.SIGNATURE))).checkCanSign()).doesNotThrowAnyException();
    }
}
