package com.bradox.erp.sign.domain.core.rule;

import com.bradox.erp.sign.domain.core.exception.SignDomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignRulesTest {

    @Test
    void tokensAreWellFormedAndHashedDeterministically() {
        String token = Tokens.generate();
        assertTrue(Tokens.wellFormed(token));
        assertEquals(Tokens.hash(token), Tokens.hash(token));
        assertNotEquals(token, Tokens.hash(token));
        assertNotEquals(token, Tokens.generate());
    }

    @Test
    void malformedTokensAreRejected() {
        assertFalse(Tokens.wellFormed(null));
        assertFalse(Tokens.wellFormed("short"));
        assertFalse(Tokens.wellFormed("!".repeat(43)));
    }

    @Test
    void hashChainDependsOnPreviousHashAndContent() {
        String a = HashChain.next(null, "one");
        String b = HashChain.next(a, "two");
        assertEquals(HashChain.next(HashChain.GENESIS, "one"), a);
        assertNotEquals(b, HashChain.next(HashChain.GENESIS, "two"));
        assertNotEquals(b, HashChain.next(a, "tampered"));
    }

    @Test
    void fieldGeometryAcceptsInsidePageAndRejectsOutside() {
        new FieldGeometry(1, 0.1, 0.1, 0.3, 0.05);
        assertThrows(SignDomainException.class, () -> new FieldGeometry(0, 0.1, 0.1, 0.3, 0.05));
        assertThrows(SignDomainException.class, () -> new FieldGeometry(1, 0.9, 0.1, 0.3, 0.05));
        assertThrows(SignDomainException.class, () -> new FieldGeometry(1, 0.1, 0.1, 0.001, 0.05));
        assertThrows(SignDomainException.class, () -> new FieldGeometry(1, Double.NaN, 0.1, 0.3, 0.05));
    }
}
