package com.kltyton.one_enough_creative_tab.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Bounded GZIP and chunking utilities shared by both loader transports.
 */
public final class PayloadChunks {
    /** Maximum bytes carried by one chunk payload. */
    public static final int MAX_CHUNK_BYTES = 24 * 1024;

    /** Maximum combined compressed size for one logical payload. */
    public static final int MAX_COMPRESSED_BYTES = 4 * 1024 * 1024;

    /** Maximum number of chunks that can fit inside the combined compressed limit. */
    public static final int MAX_CHUNKS =
            (MAX_COMPRESSED_BYTES + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES;

    /** Maximum expanded size accepted while decoding GZIP data. */
    public static final int MAX_UNCOMPRESSED_BYTES = 16 * 1024 * 1024;

    private static final int COPY_BUFFER_BYTES = 8 * 1024;

    private PayloadChunks() {
    }

    /**
     * Compresses bytes using GZIP while enforcing both input and output limits.
     *
     * @param uncompressed bytes to compress
     * @return the compressed bytes
     * @throws IllegalArgumentException when a protocol limit is exceeded
     */
    public static byte[] compress(byte[] uncompressed) {
        Objects.requireNonNull(uncompressed, "uncompressed");
        checkUncompressedSize(uncompressed.length);

        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(uncompressed.length, MAX_COMPRESSED_BYTES));
        try (GZIPOutputStream gzip = new GZIPOutputStream(new BoundedOutputStream(output, MAX_COMPRESSED_BYTES))) {
            gzip.write(uncompressed);
        } catch (PayloadSizeException exception) {
            throw new IllegalArgumentException("Compressed payload exceeds " + MAX_COMPRESSED_BYTES + " bytes", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to GZIP payload", exception);
        }

        byte[] compressed = output.toByteArray();
        checkCompressedSize(compressed.length);
        return compressed;
    }

    /**
     * Decompresses GZIP bytes with an absolute expansion limit.
     *
     * @param compressed compressed bytes
     * @return decompressed bytes
     * @throws IllegalArgumentException for invalid GZIP data or a protocol limit violation
     */
    public static byte[] decompress(byte[] compressed) {
        return decompress(compressed, -1);
    }

    /**
     * Decompresses GZIP bytes and verifies the exact expected output size.
     *
     * @param compressed compressed bytes
     * @param expectedUncompressedSize expected expanded byte count
     * @return decompressed bytes
     * @throws IllegalArgumentException for invalid GZIP data, a size mismatch, or a protocol limit violation
     */
    public static byte[] decompress(byte[] compressed, int expectedUncompressedSize) {
        Objects.requireNonNull(compressed, "compressed");
        checkCompressedSize(compressed.length);
        if (expectedUncompressedSize >= 0) {
            checkUncompressedSize(expectedUncompressedSize);
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(
                expectedUncompressedSize >= 0 ? expectedUncompressedSize : Math.min(compressed.length * 2, MAX_UNCOMPRESSED_BYTES)
        );
        byte[] copyBuffer = new byte[COPY_BUFFER_BYTES];

        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            int read;
            while ((read = gzip.read(copyBuffer)) != -1) {
                if ((long) output.size() + read > MAX_UNCOMPRESSED_BYTES) {
                    throw new IllegalArgumentException("Uncompressed payload exceeds " + MAX_UNCOMPRESSED_BYTES + " bytes");
                }
                if (expectedUncompressedSize >= 0 && (long) output.size() + read > expectedUncompressedSize) {
                    throw new IllegalArgumentException("Uncompressed payload exceeds declared size " + expectedUncompressedSize);
                }
                output.write(copyBuffer, 0, read);
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid GZIP payload", exception);
        }

        if (expectedUncompressedSize >= 0 && output.size() != expectedUncompressedSize) {
            throw new IllegalArgumentException(
                    "Uncompressed payload size mismatch: expected " + expectedUncompressedSize + ", got " + output.size()
            );
        }
        return output.toByteArray();
    }

    /**
     * Splits compressed data into protocol-sized chunks.
     */
    public static List<byte[]> split(byte[] compressed) {
        Objects.requireNonNull(compressed, "compressed");
        checkCompressedSize(compressed.length);

        int total = (compressed.length + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES;
        checkTotal(total);

        List<byte[]> chunks = new ArrayList<>(total);
        for (int offset = 0; offset < compressed.length; offset += MAX_CHUNK_BYTES) {
            int size = Math.min(MAX_CHUNK_BYTES, compressed.length - offset);
            byte[] chunk = new byte[size];
            System.arraycopy(compressed, offset, chunk, 0, size);
            chunks.add(chunk);
        }
        return List.copyOf(chunks);
    }

    /**
     * Joins chunks after validating their count, individual sizes, and aggregate compressed size.
     */
    public static byte[] join(List<byte[]> chunks) {
        Objects.requireNonNull(chunks, "chunks");
        checkTotal(chunks.size());

        int combinedSize = 0;
        for (int index = 0; index < chunks.size(); index++) {
            byte[] chunk = Objects.requireNonNull(chunks.get(index), "chunks[" + index + "]");
            checkChunkLength(chunk.length);
            if ((long) combinedSize + chunk.length > MAX_COMPRESSED_BYTES) {
                throw new IllegalArgumentException("Combined compressed payload exceeds " + MAX_COMPRESSED_BYTES + " bytes");
            }
            combinedSize += chunk.length;
        }

        byte[] compressed = new byte[combinedSize];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, compressed, offset, chunk.length);
            offset += chunk.length;
        }
        return compressed;
    }

    /** Compresses and splits one logical payload. */
    public static List<byte[]> compressAndSplit(byte[] uncompressed) {
        return split(compress(uncompressed));
    }

    /** Joins and safely decompresses one logical payload. */
    public static byte[] joinAndDecompress(List<byte[]> chunks) {
        return decompress(join(chunks));
    }

    /** Joins and safely decompresses one logical payload with an exact output-size check. */
    public static byte[] joinAndDecompress(List<byte[]> chunks, int expectedUncompressedSize) {
        return decompress(join(chunks), expectedUncompressedSize);
    }

    static byte[] copyAndValidateChunk(byte[] chunk) {
        Objects.requireNonNull(chunk, "chunk");
        checkChunkLength(chunk.length);
        return chunk.clone();
    }

    static void validateChunkMetadata(int index, int total) {
        checkTotal(total);
        if (index < 0 || index >= total) {
            throw new IllegalArgumentException("Chunk index " + index + " is outside [0, " + total + ")");
        }
    }

    static void checkUncompressedSize(int size) {
        if (size < 0 || size > MAX_UNCOMPRESSED_BYTES) {
            throw new IllegalArgumentException(
                    "Uncompressed payload size " + size + " is outside [0, " + MAX_UNCOMPRESSED_BYTES + "]"
            );
        }
    }

    private static void checkCompressedSize(int size) {
        if (size <= 0 || size > MAX_COMPRESSED_BYTES) {
            throw new IllegalArgumentException(
                    "Compressed payload size " + size + " is outside [1, " + MAX_COMPRESSED_BYTES + "]"
            );
        }
    }

    private static void checkChunkLength(int size) {
        if (size <= 0 || size > MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException("Chunk size " + size + " is outside [1, " + MAX_CHUNK_BYTES + "]");
        }
    }

    private static void checkTotal(int total) {
        if (total <= 0 || total > MAX_CHUNKS) {
            throw new IllegalArgumentException("Chunk count " + total + " is outside [1, " + MAX_CHUNKS + "]");
        }
    }

    private static final class BoundedOutputStream extends OutputStream {
        private final OutputStream delegate;
        private final int maximum;
        private int written;

        private BoundedOutputStream(OutputStream delegate, int maximum) {
            this.delegate = delegate;
            this.maximum = maximum;
        }

        @Override
        public void write(int value) throws IOException {
            ensureCapacity(1);
            delegate.write(value);
            written++;
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            ensureCapacity(length);
            delegate.write(bytes, offset, length);
            written += length;
        }

        private void ensureCapacity(int additional) throws PayloadSizeException {
            if ((long) written + additional > maximum) {
                throw new PayloadSizeException();
            }
        }
    }

    private static final class PayloadSizeException extends IOException {
        private PayloadSizeException() {
            super("Payload size limit exceeded");
        }
    }
}
