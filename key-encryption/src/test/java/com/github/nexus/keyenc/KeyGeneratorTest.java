package com.github.nexus.keyenc;

import com.github.nexus.argon2.ArgonOptions;
import com.github.nexus.config.KeyData;
import com.github.nexus.config.PrivateKey;
import com.github.nexus.config.PrivateKeyType;
import com.github.nexus.keyenc.*;
import com.github.nexus.nacl.Key;
import com.github.nexus.nacl.KeyPair;
import com.github.nexus.nacl.NaclFacade;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.*;

public class KeyGeneratorTest {

    private static final String PRIVATE_KEY = "privateKey";

    private static final String PUBLIC_KEY = "publicKey";

    private KeyPair keyPair;

    private Path keygenPath;

    private NaclFacade nacl;

    private KeyEncryptor keyEncryptor;

    private KeyGenerator generator;

    @Before
    public void init() throws IOException {

        this.keyPair = new KeyPair(
            new Key(PUBLIC_KEY.getBytes(UTF_8)),
            new Key(PRIVATE_KEY.getBytes(UTF_8))
        );

        this.keygenPath = Files.createTempDirectory(UUID.randomUUID().toString());

        this.nacl = mock(NaclFacade.class);
        this.keyEncryptor = mock(KeyEncryptor.class);

        this.generator = new KeyGeneratorImpl(nacl, keyEncryptor);

    }

    @After
    public void onTearDown() {
        verifyNoMoreInteractions(nacl, keyEncryptor);
    }

    @Test
    public void generateFromKeyDataUnlockedPrivateKey() throws Exception {

        when(nacl.generateNewKeys()).thenReturn(keyPair);

        Path privateKeyPath = Paths.get(keygenPath.toString(), "privateKey.key");

        PrivateKey privateKey = mock(PrivateKey.class);
        when(privateKey.getPath()).thenReturn(privateKeyPath);

        when(privateKey.getType()).thenReturn(PrivateKeyType.UNLOCKED);

        KeyData keyData = new KeyData(privateKey, "publicKey");

        generator.generate(keyData);

        assertThat(privateKeyPath).exists();

        String privateKeyData = Files.lines(privateKeyPath).collect(Collectors.joining());

        JsonObject privateKeyJson = Json.createReader(new StringReader(privateKeyData)).readObject();

        Assertions.assertThat(privateKeyJson).containsOnlyKeys("data", "type");

        Assertions.assertThat(privateKeyJson.getJsonObject("data")).containsOnlyKeys("bytes");
        Assertions.assertThat(privateKeyJson.getJsonObject("data").getString("bytes")).isEqualTo("cHJpdmF0ZUtleQ==");

        verify(nacl).generateNewKeys();

    }

    @Test
    public void generateFromKeyDataLockedPrivateKey() throws Exception {

        when(nacl.generateNewKeys()).thenReturn(keyPair);

        ArgonOptions argonOptions = new ArgonOptions("ib", 1, 1, 1);

        KeyConfig encrypedPrivateKey = KeyConfig.Builder.create()
            .asalt("ASALT".getBytes())
            .sbox("SBOX".getBytes())
            .snonce("SNONCE".getBytes())
            .argonOptions(argonOptions)
            .build();

        when(keyEncryptor.encryptPrivateKey(any(Key.class), anyString())).thenReturn(encrypedPrivateKey);

        Path privateKeyPath = Paths.get(keygenPath.toString(), "privateKey.key");

        PrivateKey privateKey = mock(PrivateKey.class);
        when(privateKey.getPath()).thenReturn(privateKeyPath);
        when(privateKey.getPassword()).thenReturn("PASSWORD");
        when(privateKey.getType()).thenReturn(PrivateKeyType.LOCKED);

        KeyData keyData = new KeyData(privateKey, "publicKey");

        generator.generate(keyData);

        assertThat(privateKeyPath).exists();

        String privateKeyData = Files.lines(privateKeyPath).collect(Collectors.joining());

        JsonObject privateKeyJson = Json.createReader(new StringReader(privateKeyData)).readObject();

        Assertions.assertThat(privateKeyJson).containsOnlyKeys("data", "type");
        Assertions.assertThat(privateKeyJson.getString("type")).isEqualTo("argon2sbox");

        Assertions.assertThat(privateKeyJson.getJsonObject("data"))
            .containsOnlyKeys("aopts", "snonce", "sbox", "asalt");

        Assertions.assertThat(privateKeyJson.getJsonObject("data").getJsonObject("aopts"))
            .containsOnlyKeys("variant", "memory", "iterations", "parallelism");

        verify(nacl).generateNewKeys();
        verify(keyEncryptor).encryptPrivateKey(any(Key.class), anyString());
    }

    @Test
    public void generateIOException() throws Exception {

        when(nacl.generateNewKeys()).thenReturn(keyPair);

        //Should throw IO as file exists
        Path privateKeyPath = Files.createTempFile(keygenPath, "privateKey", ".key");

        PrivateKey privateKey = mock(PrivateKey.class);
        when(privateKey.getPath()).thenReturn(privateKeyPath);

        when(privateKey.getType()).thenReturn(PrivateKeyType.UNLOCKED);

        KeyData keyData = new KeyData(privateKey, "publicKey");

        try {
            generator.generate(keyData);
            failBecauseExceptionWasNotThrown(KeyGeneratorException.class);
        } catch (KeyGeneratorException ex) {
            assertThat(ex).hasCauseInstanceOf(IOException.class);
        }

        verify(nacl).generateNewKeys();

    }

}