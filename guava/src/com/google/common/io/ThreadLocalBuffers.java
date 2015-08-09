package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.base.FinalizableReferenceQueue;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Provides thread-local buffers in the form of either a byte-array of a {@link
 * FastByteArrayOutputStream}. To circumvent any memory-leak issues, the implementation uses {@link
 * SoftReference} internally, so the Garbage Collection may collect any memory allocated by this
 * class that is not referenced outside the class itself at the time. This class is package-local
 * for it is intended to be used by guava's io-classes, but can maybe eventually be made public.
 *
 * @author Bernd Hopp
 * @since 19.0
 */
@Beta
final class ThreadLocalBuffers {

    private static final int INIT_BYTE_SIZE = 8192;

    private static final ThreadLocal<Reference<FastByteArrayOutputStream>> BYTE_ARRAY_OUTPUTSTREAM_REFERENCE_THREADLOCAL =
            new ThreadLocal<Reference<FastByteArrayOutputStream>>();

    private static final ThreadLocal<Reference<byte[]>> BYTE_ARRAY_REFERENCE_THREADLOCAL =
            new ThreadLocal<Reference<byte[]>>();

    private ThreadLocalBuffers() {
    }

    /**
     * Returns a thread-local byte-array of arbitrary data, that can be used for buffering. It is
     * guaranteed that the byte-array's length is a power of 2 greater than or equal 8192. If the
     * array must have a minimum size larger than 8192 or should not contain arbitrary data but just
     * zeros, see {@link ThreadLocalBuffers#getByteArray(int, boolean)}. Note that calls to this
     * method from the same thread will return the same byte-array. <p>keep in mind, that {@link
     * #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a byte-array for buffering.
     * @since 19.0
     */
    public static byte[] getByteArray() {
        return getByteArray(0, false);
    }

    /**
     * Returns a thread-local byte-array that can be used for buffering. It is guaranteed that the
     * byte-array's length is a power of 2 greater than or equal minSize. <p>keep in mind, that
     * {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()},
     * {@link #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned byte-array, must not be negative.
     * @param zeroed  set to true if the byte-array should contain just zeros
     * @return a byte-array for buffering
     * @since 19.0
     */
    public static byte[] getByteArray(int minSize, boolean zeroed) {
        checkArgument(minSize >= 0, "minSize must not be negative");

        Reference<byte[]> byteArrayReference = BYTE_ARRAY_REFERENCE_THREADLOCAL.get();

        if (byteArrayReference == null) {
            return createAndSetByteArray(minSize);
        }

        byte[] byteArray = byteArrayReference.get();

        if (byteArray == null) {
            return createAndSetByteArray(minSize);
        }

        if (byteArray.length < minSize) {
            return createAndSetByteArray(minSize);
        }

        if (zeroed) {
            Arrays.fill(byteArray, (byte) 0);
        }

        return byteArray;
    }

    private static byte[] createAndSetByteArray(int minSize) {
        int size = INIT_BYTE_SIZE;

        while (minSize > size) {
            size *= 2;
        }

        byte[] byteArray = new byte[size];

        Reference<byte[]> byteArraySoftReference = referenceByteArray(byteArray);

        BYTE_ARRAY_REFERENCE_THREADLOCAL.set(byteArraySoftReference);

        return byteArray;
    }

    private static Reference<byte[]> referenceByteArray(byte[] byteArray) {
        long softReferencedMemoryAfter = MemorySizeTrackingByteArraySoftReference.getAllocatedMemoryTotal() + byteArray.length;
        long twentyPercentOfTotalMemory = Runtime.getRuntime().totalMemory() / 5;

        //soft-referenced byte-array buffers must not make up for more than 20% of the total available memory
        boolean canBeSoftReferenced = twentyPercentOfTotalMemory >= softReferencedMemoryAfter;

        return canBeSoftReferenced
                ? new MemorySizeTrackingByteArraySoftReference(byteArray)
                : new WeakReference<byte[]>(byteArray);
    }

    /**
     * Returns a thread-local {@link ByteBuffer} of arbitrary data, that can be used for buffering.
     * It is guaranteed that the {@link ByteBuffer}'s length is a power of 2 greater than or equal
     * 8192. If the array must have a minimum size larger than 8192 or should not contain arbitrary
     * data but just zeros, see {@link ThreadLocalBuffers#getByteBuffer(int, boolean)}. Note that
     * calls to this method from the same thread will return the same byte-array. <p>keep in mind,
     * that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()},
     * {@link #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a {@link ByteBuffer} for buffering.
     * @since 19.0
     */
    public static ByteBuffer getByteBuffer() {
        return getByteBuffer(0, false);
    }

    /**
     * Returns a thread-local {@link ByteBuffer} that can be used for buffering. It is guaranteed
     * that the {@link ByteBuffer}'s length is a power of 2 greater than or equal minSize. <p>keep
     * in mind, that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link
     * #getByteBuffer()}, {@link #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link
     * #getCharBuffer(int, boolean)} are supposed to not be used simultaneously, as they all use the
     * same underlying byte-array. This does not apply for {@link #getByteArrayOutputStream()} and
     * {@link #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned {@link ByteBuffer}, must not be negative.
     * @param zeroed  set to true if the {@link ByteBuffer} should contain just zeros
     * @return a {@link ByteBuffer} for buffering
     * @since 19.0
     */
    public static ByteBuffer getByteBuffer(int minSize, boolean zeroed) {
        return ByteBuffer.wrap(getByteArray(minSize, zeroed));
    }

    /**
     * Returns a thread-local {@link CharBuffer} of arbitrary data, that can be used for buffering.
     * It is guaranteed that the {@link CharBuffer}'s length is a power of 2 greater than or equal
     * 8192. If the array must have a minimum size larger than 8192 or should not contain arbitrary
     * data but just zeros, see {@link ThreadLocalBuffers#getCharBuffer(int, boolean)}. Note that
     * calls to this method from the same thread will return the same byte-array. <p>keep in mind,
     * that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()},
     * {@link #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a {@link CharBuffer} for buffering.
     * @since 19.0
     */
    public static CharBuffer getCharBuffer() {
        return getCharBuffer(0, false);
    }

    /**
     * Returns a thread-local {@link CharBuffer} that can be used for buffering. It is guaranteed
     * that the {@link CharBuffer}'s length is a power of 2 greater than or equal minSize. <p>keep
     * in mind, that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link
     * #getByteBuffer()}, {@link #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link
     * #getCharBuffer(int, boolean)} are supposed to not be used simultaneously, as they all use the
     * same underlying byte-array. This does not apply for {@link #getByteArrayOutputStream()} and
     * {@link #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned {@link CharBuffer}, must not be negative.
     * @param zeroed  set to true if the {@link CharBuffer} should contain just zeros
     * @return a {@link CharBuffer} for buffering
     * @since 19.0
     */
    public static CharBuffer getCharBuffer(int minSize, boolean zeroed) {
        return getByteBuffer(minSize, zeroed).asCharBuffer();
    }

    /**
     * Returns a {@link FastByteArrayOutputStream}. It is guaranteed that the ByteArrayOutputStream
     * will not be accessed outside the current thread. Note that calls to this method from the same
     * thread will return the same ByteArrayOutputStream. <p>keep in mind, that {@link
     * #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a {@link FastByteArrayOutputStream} for buffering and stream-capturing.
     * @since 19.0
     */
    public static FastByteArrayOutputStream getByteArrayOutputStream() {
        return getByteArrayOutputStream(0);
    }

    /**
     * Returns a {@link FastByteArrayOutputStream}. It is guaranteed that the ByteArrayOutputStream
     * will not be accessed outside the current thread. Note that calls to this method from the same
     * thread will return the same ByteArrayOutputStream. <p>keep in mind, that {@link
     * #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum internal array size ot the returned {@link
     *                FastByteArrayOutputStream}, must not be negative.
     * @return a {@link FastByteArrayOutputStream} for buffering and stream-capturing.     *
     * @since 19.0
     */
    public static FastByteArrayOutputStream getByteArrayOutputStream(int minSize) {
        Reference<FastByteArrayOutputStream> byteArrayOutputStreamReference = BYTE_ARRAY_OUTPUTSTREAM_REFERENCE_THREADLOCAL
                .get();

        if (byteArrayOutputStreamReference == null) {
            return createAndSetByteArrayOutputStream(minSize);
        }

        FastByteArrayOutputStream baos = byteArrayOutputStreamReference.get();

        if (baos == null) {
            return createAndSetByteArrayOutputStream(minSize);
        }

        if (baos.size() < minSize) {
            return createAndSetByteArrayOutputStream(minSize);
        }

        baos.reset();

        return baos;
    }

    private static FastByteArrayOutputStream createAndSetByteArrayOutputStream(int minSize) {
        int size = Math.max(INIT_BYTE_SIZE, minSize);

        FastByteArrayOutputStream fastByteArrayOutputStream = new FastByteArrayOutputStream(size);

        Reference<FastByteArrayOutputStream> reference = new WeakReference<FastByteArrayOutputStream>(fastByteArrayOutputStream);

        BYTE_ARRAY_OUTPUTSTREAM_REFERENCE_THREADLOCAL.set(reference);

        return fastByteArrayOutputStream;
    }
}
