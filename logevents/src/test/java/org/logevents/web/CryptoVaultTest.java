package org.logevents.web;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class CryptoVaultTest {

    @Test
    public void shouldDecryptEncryptedValue() throws GeneralSecurityException {
        CryptoVault vault = new CryptoVault(Optional.empty());
        String clearString = "This is a test";
        assertEquals(clearString, vault.decrypt(vault.encrypt(clearString)));
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldThrowWhenValueIsTampered() throws GeneralSecurityException {
        CryptoVault vault = new CryptoVault(Optional.empty());
        String clearText = "This is a test with different characters that will be encrypted";
        String cryptoText = vault.encrypt(clearText);
        try {
            String decrypted = vault.decrypt(cryptoText.replaceAll("[0-9]", "a"));
            assertNotEquals(decrypted, clearText);
        } catch (GeneralSecurityException ignored) {
        }
    }

    @Test
    public void shouldThrowOnWrongKey() throws GeneralSecurityException {
        CryptoVault vault = new CryptoVault(Optional.empty());
        String cryptoText = vault.encrypt("This is a test");
        CryptoVault otherVault = new CryptoVault(Optional.empty());
        expectedException.expect(GeneralSecurityException.class);
        otherVault.decrypt(cryptoText);
    }
}