package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.exception.InvalidRequestException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamoCursorCodec {

    private static final int VERSION = 1;

    private static final int MAX_CURSOR_LENGTH = 8_192;

    public String encode(String queryIdentity, Map<String, AttributeValue> key) {
        if (key == null || key.isEmpty())
            return null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(VERSION);
                output.writeUTF(queryIdentity);
                output.writeInt(key.size());
                for (var entry : key.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                    output.writeUTF(entry.getKey());
                    writeValue(output, entry.getValue());
                }
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not encode DynamoDB cursor", exception);
        }
    }

    public Map<String, AttributeValue> decode(String queryIdentity, String cursor) {
        if (cursor == null || cursor.isBlank())
            return Map.of();
        if (cursor.length() > MAX_CURSOR_LENGTH)
            throw invalidCursor();
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
                if (input.readInt() != VERSION || !queryIdentity.equals(input.readUTF()))
                    throw invalidCursor();
                int size = input.readInt();
                if (size < 1 || size > 16)
                    throw invalidCursor();
                Map<String, AttributeValue> key = new LinkedHashMap<>();
                for (int index = 0; index < size; index++) {
                    if (key.put(input.readUTF(), readValue(input)) != null)
                        throw invalidCursor();
                }
                if (input.available() != 0)
                    throw invalidCursor();
                return Map.copyOf(key);
            }
        } catch (IllegalArgumentException | IOException exception) {
            throw invalidCursor();
        }
    }

    private static void writeValue(DataOutputStream output, AttributeValue value) throws IOException {
        if (value.s() != null) {
            output.writeByte('S');
            output.writeUTF(value.s());
        } else if (value.n() != null) {
            output.writeByte('N');
            output.writeUTF(value.n());
        } else if (value.b() != null) {
            output.writeByte('B');
            output.writeUTF(Base64.getEncoder().encodeToString(value.b().asByteArray()));
        } else {
            throw new IllegalArgumentException("Cursor contains an unsupported key type");
        }
    }

    private static AttributeValue readValue(DataInputStream input) throws IOException {
        return switch (input.readByte()) {
            case 'S' -> AttributeValue.builder().s(input.readUTF()).build();
            case 'N' -> AttributeValue.builder().n(input.readUTF()).build();
            case 'B' -> AttributeValue.builder().b(software.amazon.awssdk.core.SdkBytes.fromByteArray(
                    Base64.getDecoder().decode(input.readUTF()))).build();
            default -> throw invalidCursor();
        };
    }

    private static InvalidRequestException invalidCursor() {
        return new InvalidRequestException("Cursor is invalid or does not belong to this query");
    }

}
