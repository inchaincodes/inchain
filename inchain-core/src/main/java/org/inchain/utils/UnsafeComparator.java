package org.inchain.utils;

import java.nio.ByteOrder;
import java.util.Comparator;

import sun.misc.Unsafe;

public enum UnsafeComparator implements Comparator<byte[]> {
    INSTANCE;

    public static final int BYTES = Long.SIZE / Byte.SIZE;
    private static final int UNSIGNED_MASK = 0xFF;
    static final boolean BIG_ENDIAN =
        ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);

    /*
     * The following static final fields exist for performance reasons.
     *
     * In UnsignedBytesBenchmark, accessing the following objects via static
     * final fields is the fastest (more than twice as fast as the Java
     * implementation, vs ~1.5x with non-final static fields, on x86_32)
     * under the Hotspot server compiler. The reason is obviously that the
     * non-final fields need to be reloaded inside the loop.
     *
     * And, no, defining (final or not) local variables out of the loop still
     * isn't as good because the null check on the theUnsafe object remains
     * inside the loop and BYTE_ARRAY_BASE_OFFSET doesn't get
     * constant-folded.
     *
     * The compiler can treat static final fields as compile-time constants
     * and can constant-fold them while (final or not) local variables are
     * run time values.
     */

    static final Unsafe theUnsafe;

    /** The offset to the first element in a byte array. */
    static final int BYTE_ARRAY_BASE_OFFSET;

    static {
      theUnsafe = getUnsafe();

      BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);

      // sanity check - this should never fail
      if (theUnsafe.arrayIndexScale(byte[].class) != 1) {
        throw new AssertionError();
      }
    }
    
    /**
     * Returns a sun.misc.Unsafe.  Suitable for use in a 3rd party package.
     * Replace with a simple call to Unsafe.getUnsafe when integrating
     * into a jdk.
     *
     * @return a sun.misc.Unsafe
     */
    private static sun.misc.Unsafe getUnsafe() {
        try {
            return sun.misc.Unsafe.getUnsafe();
        } catch (SecurityException tryReflectionInstead) {}
        try {
            return java.security.AccessController.doPrivileged
            (new java.security.PrivilegedExceptionAction<sun.misc.Unsafe>() {
                public sun.misc.Unsafe run() throws Exception {
                    Class<sun.misc.Unsafe> k = sun.misc.Unsafe.class;
                    for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                        f.setAccessible(true);
                        Object x = f.get(null);
                        if (k.isInstance(x))
                            return k.cast(x);
                    }
                    throw new NoSuchFieldError("the Unsafe");
                }});
        } catch (java.security.PrivilegedActionException e) {
            throw new RuntimeException("Could not initialize intrinsics",
                                       e.getCause());
        }
    }

    @Override public int compare(byte[] left, byte[] right) {
      int minLength = Math.min(left.length, right.length);
      int minWords = minLength / BYTES;

      /*
       * Compare 8 bytes at a time. Benchmarking shows comparing 8 bytes at a
       * time is no slower than comparing 4 bytes at a time even on 32-bit.
       * On the other hand, it is substantially faster on 64-bit.
       */
      for (int i = 0; i < minWords * BYTES; i += BYTES) {
        long lw = theUnsafe.getLong(left, BYTE_ARRAY_BASE_OFFSET + (long) i);
        long rw = theUnsafe.getLong(right, BYTE_ARRAY_BASE_OFFSET + (long) i);
        if (lw != rw) {
          if (BIG_ENDIAN) {
            return (int) (lw & UNSIGNED_MASK - UNSIGNED_MASK & rw);
          }

          /*
           * We want to compare only the first index where left[index] != right[index].
           * This corresponds to the least significant nonzero byte in lw ^ rw, since lw
           * and rw are little-endian.  Long.numberOfTrailingZeros(diff) tells us the least 
           * significant nonzero bit, and zeroing out the first three bits of L.nTZ gives us the 
           * shift to get that least significant nonzero byte.
           */
          int n = Long.numberOfTrailingZeros(lw ^ rw) & ~0x7;
          return (int) (((lw >>> n) & UNSIGNED_MASK) - ((rw >>> n) & UNSIGNED_MASK));
        }
      }

      // The epilogue to cover the last (minLength % 8) elements.
      for (int i = minWords * BYTES; i < minLength; i++) {
        int result = left[i] & UNSIGNED_MASK - right[i] & UNSIGNED_MASK;
        if (result != 0) {
          return result;
        }
      }
      return left.length - right.length;
    }
}
