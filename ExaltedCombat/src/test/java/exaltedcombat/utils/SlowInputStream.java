package exaltedcombat.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class SlowInputStream extends InputStream {
    private final InputStream wrapped;
    private final long sleepPerByteNanos;

    public SlowInputStream(InputStream wrapped, long sleepPerByte, TimeUnit unit) {
        this.wrapped = wrapped;
        this.sleepPerByteNanos = unit.toNanos(sleepPerByte);

        ExceptionHelper.checkArgumentInRange(sleepPerByteNanos, 0, Long.MAX_VALUE, "sleepPerByteNanos");
    }

    private static void sleep(long nanos) {
        try {
            TimeUnit.NANOSECONDS.sleep(nanos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static long mul(long a, long b) {
        assert a > 0 && b > 0;

        int lengthA = Long.SIZE - Long.numberOfLeadingZeros(a);
        int lengthB = Long.SIZE - Long.numberOfLeadingZeros(b);
        int maxMulSize = lengthA + lengthB;
        if (maxMulSize > Long.SIZE) {
            return Long.MAX_VALUE;
        }
        else if (maxMulSize == Long.SIZE) {
            // possible overflow
            long result = a * b;
            if (result < 0) {
                result = Long.MAX_VALUE;
            }
            return result;
        }
        else {
            return a * b;
        }
    }

    private static void sleep(long nanos, int sleepCount) {
        if (sleepCount <= 0 || nanos <= 0) {
            return;
        }

        sleep(mul(nanos, sleepCount));
    }

    @Override
    public int read() throws IOException {
        sleep(sleepPerByteNanos, 1);
        return wrapped.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        sleep(sleepPerByteNanos, b.length);
        return wrapped.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        sleep(sleepPerByteNanos, len);
        return wrapped.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return wrapped.available();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    @Override
    public void mark(int readlimit) {
        wrapped.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return wrapped.markSupported();
    }

    @Override
    public void reset() throws IOException {
        wrapped.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return wrapped.skip(n);
    }
}
