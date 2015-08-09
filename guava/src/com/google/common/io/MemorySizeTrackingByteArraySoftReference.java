package com.google.common.io;

import com.google.common.base.FinalizableReferenceQueue;
import com.google.common.base.FinalizableSoftReference;

import java.util.concurrent.atomic.AtomicLong;

class MemorySizeTrackingByteArraySoftReference extends FinalizableSoftReference<byte[]> {

    private static final FinalizableReferenceQueue QUEUE = new FinalizableReferenceQueue();
    private static final AtomicLong ALLOCATED_MEMORY_TOTAL = new AtomicLong(0l);
    private final int allocatedMemoryThisInstance;

    public MemorySizeTrackingByteArraySoftReference(byte[] referent) {
        super(referent, QUEUE);
        allocatedMemoryThisInstance = referent.length;
        ALLOCATED_MEMORY_TOTAL.addAndGet(allocatedMemoryThisInstance);
    }

    public static long getAllocatedMemoryTotal() {
        return ALLOCATED_MEMORY_TOTAL.get();
    }

    @Override
    public void finalizeReferent() {
        ALLOCATED_MEMORY_TOTAL.addAndGet(-allocatedMemoryThisInstance);
    }
}
