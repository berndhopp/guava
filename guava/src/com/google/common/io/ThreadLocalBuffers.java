package com.google.common.io;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

import java.io.ByteArrayOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Provides thread-local buffers in the form of either a byte-array of a {@link
 * ByteArrayOutputStream}. To circumvent any memory-leak issues, the implementation uses {@link
 * SoftReference} and {@link WeakReference} internally, so the Garbage Collection may collect any
 * memory allocated by this class that is not referenced outside the class itself at the time. This
 * class is package-local for it is intended to be used by guava's io-classes, but can maybe
 * eventually be made public.
 *
 * @author Bernd Hopp
 * @since 20.0
 */
@Beta
@GwtCompatible(emulated = true)
final class ThreadLocalBuffers {

    private static final int INIT_BYTE_SIZE = 8192;
    private static final int MAX_BYTE_SIZE = 65536;

    @GwtIncompatible// ThreadLocal, Reference, ByteArrayOutputStream
    private static final ThreadLocal<Reference<ByteArrayOutputStream>> BYTE_ARRAY_OUTPUTSTREAM_THREADLOCAL =
            new ThreadLocal<Reference<ByteArrayOutputStream>>();

    @GwtIncompatible //ThreadLocal, Reference"
    private static final ThreadLocal<Reference<byte[]>> BYTE_ARRAY_THREADLOCAL =
            new ThreadLocal<Reference<byte[]>>();

    private ThreadLocalBuffers() {
    }

    /**
     * Returns a thread-local byte-array of arbitrary data, that can be used for buffering. It is
     * guaranteed that the byte-array's length is even. If the
     * array must have a minimum size or should not contain arbitrary data but just
     * zeros, see {@link ThreadLocalBuffers#getByteArray(int, boolean)}. Note that calls to this
     * method from the same thread will return the same byte-array. <p>keep in mind, that
     * getByteArray(), {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a byte-array for buffering.
     * @since 20.0
     */
    public static byte[] getByteArray() {
        return getByteArray(0, false);
    }

    /**
     * Returns a thread-local byte-array that can be used for buffering. It is guaranteed that the
     * byte-array's length is even and greater than or equal minSize. <p>keep in mind, that
     * {@link #getByteArray()}, getByteArray(int, boolean), {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned byte-array, must be between 0 and 65536
     * @param zeroed  set to true if the byte-array should contain just zeros
     * @return a byte-array for buffering
     * @since 20.0
     */
    public static byte[] getByteArray(int minSize, boolean zeroed) {
        checkArgument(minSize >= 0, "minSize must not be negative");
        checkArgument(minSize <= MAX_BYTE_SIZE, "minSize must not exceed 2^16");

        Reference<byte[]> reference = BYTE_ARRAY_THREADLOCAL.get();

        if (reference == null) {
            return createAndCacheByteArray(minSize);
        }

        byte[] byteArray = reference.get();

        if (byteArray == null || byteArray.length < minSize) {
            return createAndCacheByteArray(minSize);
        }

        if (zeroed) {
            Arrays.fill(byteArray, (byte) 0);
        }

        return byteArray;
    }

    @GwtIncompatible // ThreadLocal, Reference
    private static byte[] createAndCacheByteArray(int minSize) {

        int size = INIT_BYTE_SIZE;

        while (size < minSize) {
            size *= 2;
        }

        byte[] byteArray = new byte[size];

        boolean sizeIsInitSize = (size == INIT_BYTE_SIZE);

        //only 8k-buffers are soft-referenced to avoid http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6912889
        Reference<byte[]> reference = sizeIsInitSize
                ? new SoftReference<byte[]>(byteArray)
                : new WeakReference<byte[]>(byteArray);

        BYTE_ARRAY_THREADLOCAL.set(reference);

        return byteArray;
    }

    /**
     * Returns a thread-local {@link ByteBuffer} of arbitrary data, that can be used for buffering.
     * It is guaranteed that the {@link ByteBuffer}'s length is greater than or equal
     * 8192. If the array must have a minimum size larger than 8192 or should not contain arbitrary
     * data but just zeros, see {@link ThreadLocalBuffers#getByteBuffer(int, boolean)}. Note that
     * calls to this method from the same thread will return the same byte-array. <p>keep in mind,
     * that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, getByteBuffer(), {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a {@link ByteBuffer} for buffering.
     * @since 20.0
     */
    @GwtIncompatible // ByteBuffer
    public static ByteBuffer getByteBuffer() {
        return getByteBuffer(0, false);
    }

    /**
     * Returns a thread-local {@link ByteBuffer} that can be used for buffering. It is guaranteed
     * that the {@link ByteBuffer}'s length is greater than or equal minSize. <p>keep
     * in mind, that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link
     * #getByteBuffer()}, getByteBuffer(int, boolean), {@link #getCharBuffer()} and {@link
     * #getCharBuffer(int, boolean)} are supposed to not be used simultaneously, as they all use the
     * same underlying byte-array. This does not apply for {@link #getByteArrayOutputStream()} and
     * {@link #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned {@link ByteBuffer}, must be between 0 and 65536
     * @param zeroed  set to true if the {@link ByteBuffer} should contain just zeros
     * @return a {@link ByteBuffer} for buffering
     * @since 20.0
     */
    @GwtIncompatible // ByteBuffer
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
     * {@link #getByteBuffer(int, boolean)}, getCharBuffer() and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a {@link CharBuffer} for buffering.
     * @since 20.0
     */
    @GwtIncompatible // CharBuffer
    public static CharBuffer getCharBuffer() {
        return getCharBuffer(0, false);
    }

    /**
     * Returns a thread-local {@link CharBuffer} that can be used for buffering. It is guaranteed
     * that the {@link CharBuffer}'s length is a power of 2 greater than or equal minSize. <p>keep
     * in mind, that {@link #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link
     * #getByteBuffer()}, {@link #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and
     * getCharBuffer(int, boolean) are supposed to not be used simultaneously, as they all use the
     * same underlying byte-array. This does not apply for {@link #getByteArrayOutputStream()} and
     * {@link #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned {@link CharBuffer}, must be between 0 and 65536
     * @param zeroed  set to true if the {@link CharBuffer} should contain just zeros
     * @return a {@link CharBuffer} for buffering
     * @since 20.0
     */
    @GwtIncompatible // CharBuffer
    public static CharBuffer getCharBuffer(int minSize, boolean zeroed) {
        return getByteBuffer(minSize, zeroed).asCharBuffer();
    }

    /**
     * Returns a {@link ByteArrayOutputStream}. It is guaranteed that the ByteArrayOutputStream
     * will not be accessed outside the current thread. Note that calls to this method from the same
     * thread will return the same ByteArrayOutputStream. <p>keep in mind, that {@link
     * #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for getByteArrayOutputStream() and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @return a {@link ByteArrayOutputStream} for buffering and stream-capturing.
     * @since 20.0
     */
    @GwtIncompatible // ByteArrayOutputStream
    public static ByteArrayOutputStream getByteArrayOutputStream() {
        return getByteArrayOutputStream(0);
    }

    /**
     * Returns a {@link ByteArrayOutputStream}. It is guaranteed that the ByteArrayOutputStream
     * will not be accessed outside the current thread. Note that calls to this method from the same
     * thread will return the same ByteArrayOutputStream. <p>keep in mind, that {@link
     * #getByteArray()}, {@link #getByteArray(int, boolean)}, {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and
     * getByteArrayOutputStream(int).</p>
     *
     * @param minSize the minimum internal array size ot the returned {@link
     *                ByteArrayOutputStream}, must not be negative.
     * @return a {@link ByteArrayOutputStream} for buffering and stream-capturing.     *
     * @since 20.0
     */
    @GwtIncompatible // ByteArrayOutputStream
    public static ByteArrayOutputStream getByteArrayOutputStream(int minSize) {
        checkArgument(minSize >= 0, "minSize must not be negative");

        Reference<ByteArrayOutputStream> reference = BYTE_ARRAY_OUTPUTSTREAM_THREADLOCAL
                .get();

        if (reference == null) {
            return createAndCacheByteArrayOutputStream(minSize);
        }

        ByteArrayOutputStream baos = reference.get();

        if (baos == null || baos.size() < minSize) {
            return createAndCacheByteArrayOutputStream(minSize);
        }

        baos.reset();

        return baos;
    }

    @GwtIncompatible // ByteArrayOutputStream, Reference, ThreadLocal
    private static ByteArrayOutputStream createAndCacheByteArrayOutputStream(int minSize) {
        int size = Math.max(INIT_BYTE_SIZE, minSize);

        ByteArrayOutputStream baos = new FastByteArrayOutputStream(size);

        //ByteArrayOutputStreams are weak-referenced to avoid http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6912889
        Reference<ByteArrayOutputStream> reference = new WeakReference<ByteArrayOutputStream>(baos);

        BYTE_ARRAY_OUTPUTSTREAM_THREADLOCAL.set(reference);

        return baos;
    }
}
