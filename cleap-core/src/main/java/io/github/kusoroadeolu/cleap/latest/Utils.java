package io.github.kusoroadeolu.cleap.latest;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public final class Utils {
    public static final int MAX_POW2 = 1 << 30;
    public static final double MERGE_RATIO = 0.1;
    public static final long MERGE_CAP = 1000;
    public static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public static final int NCPU = Runtime.getRuntime().availableProcessors();


    /*
     * Stolen from JCTools
     * */
    public static int roundToPowerOfTwo(final int value)
    {
        if (value > MAX_POW2)
        {
            throw new IllegalArgumentException("There is no larger power of 2 int for value:" + value +
                    " since it exceeds 2^31.");
        }
        if (value < 0)
        {
            throw new IllegalArgumentException("Given value:" + value + ". Expecting value >= 0.");
        }
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    public static VarHandle fieldOffset(Class<?> host, String fieldName, Class<?> type) {
        try {
           return LOOKUP.findVarHandle(host, fieldName, type);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static VarHandle arrayOffset() {
        try {
            return MethodHandles.arrayElementVarHandle(Object[].class);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long mergeLimit(long capacity) {
        long mergeLimit = Math.min(MERGE_CAP, (long) (MERGE_RATIO * capacity));
        return Math.max(1, mergeLimit);
    }

    public static int offset(long index, long mask) {
        return (int) (index & mask);
    }


}
