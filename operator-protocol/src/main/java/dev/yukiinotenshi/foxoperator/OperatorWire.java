package dev.yukiinotenshi.foxoperator;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

public final class OperatorWire {
    public static final String CHANNEL = "svc-fox-mobile:operator_v1";
    public static final int CHUNK = 24_000;
    public static final int MAX_REQUEST = 144_000;
    public static final int MAX_RESPONSE = 2_000_000;
    private static final int HEADER = 16;

    private OperatorWire() {}

    public static void send(long id, byte[] bytes, Consumer<byte[]> output) {
        for (int offset = 0; offset < bytes.length; offset += CHUNK) {
            int size = Math.min(CHUNK, bytes.length - offset);
            output.accept(ByteBuffer.allocate(HEADER + size).putLong(id)
                    .putInt(bytes.length).putInt(offset).put(bytes, offset, size).array());
        }
    }

    public record Frame(long id, int total, int offset, byte[] data) {
        public static Frame read(byte[] packet, int limit) {
            if (packet.length <= HEADER || packet.length > HEADER + CHUNK) return null;
            ByteBuffer buf = ByteBuffer.wrap(packet);
            long id = buf.getLong();
            int total = buf.getInt(), offset = buf.getInt(), size = buf.remaining();
            if (total <= 0 || total > limit || offset < 0 || offset >= total
                    || offset % CHUNK != 0 || size != Math.min(CHUNK, total - offset)) return null;
            return new Frame(id, total, offset, Arrays.copyOfRange(packet, HEADER, packet.length));
        }
    }

    public static final class Incoming {
        private final long id;
        private final byte[] bytes;
        private int received;

        public Incoming(Frame first) {
            id = first.id();
            bytes = new byte[first.total()];
        }

        public boolean append(Frame frame) {
            if (frame.id() != id || frame.total() != bytes.length || frame.offset() != received) return false;
            System.arraycopy(frame.data(), 0, bytes, received, frame.data().length);
            received += frame.data().length;
            return true;
        }

        public byte[] complete() {
            return received == bytes.length ? bytes : null;
        }
    }
}
