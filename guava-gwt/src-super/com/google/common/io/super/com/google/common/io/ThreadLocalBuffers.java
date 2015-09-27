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
final class ThreadLocalBuffers {

    private static final int MAX_BYTE_SIZE = 65536;

    //since javascript is effectively single threaded, one buffer is sufficient
    private static byte[] buffer = new byte[2048];

    /**
     * Returns a thread-local byte-array of arbitrary data, that can be used for buffering. It is
     * guaranteed that the byte-array's length is greater than or equal 8192. If the
     * array must have a minimum size larger than 8192 or should not contain arbitrary data but just
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
     * byte-array's length is greater than or equal minSize. <p>keep in mind, that
     * {@link #getByteArray()}, getByteArray(int, boolean), {@link #getByteBuffer()}, {@link
     * #getByteBuffer(int, boolean)}, {@link #getCharBuffer()} and {@link #getCharBuffer(int,
     * boolean)} are supposed to not be used simultaneously, as they all use the same underlying
     * byte-array. This does not apply for {@link #getByteArrayOutputStream()} and {@link
     * #getByteArrayOutputStream(int)}.</p>
     *
     * @param minSize the minimum size ot the returned byte-array, must not be negative.
     * @param zeroed  set to true if the byte-array should contain just zeros
     * @return a byte-array for buffering
     * @since 20.0
     */
    public static byte[] getByteArray(int minSize, boolean zeroed) {
        checkArgument(minSize >= 0, "minSize must not be negative");
        checkArgument(minSize <= MAX_BYTE_SIZE, "minSize must not exceed 2^16");

        if(buffer.length < minSize){
            if((minSize % 2) != 0){
                ++minSize;
            }

            buffer = new byte[minSize];
        } else if(zeroed){
            Arrays.fill(buffer, (byte)0);
        }

        return buffer;
    }
}
