package exaltedcombat.utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.*;
import org.jtrim.concurrent.*;
import org.jtrim.utils.*;

/**
 * Defines a task which is able to load the first serialized object from a given
 * file and return this object deserialized.
 * This task is cancelable and will respond to cancel requests even if it is
 * currently reading the file. Although this task should respond immediatelly,
 * it is still wrong to assume that it will do actually do so. Responding to a
 * cancel request must still be assumed to be only a best effort.
 * <P>
 * This task will not attempt to read the given file more than once. Once the
 * file has been loaded it will return the same deserialized object even if the
 * file has been changed since then. Canceling this task is also permanent, a
 * canceled task will never try to read the file (again) and attempting to
 * {@link #get() get} the deserialized object will result in a
 * {@code CancellationException} to be thrown.
 * <P>
 * Actually reading the file is done in the first {@link #get() get()} or
 * {@link #get(long, java.util.concurrent.TimeUnit) get(long, TimeUnit)} method
 * call (note however that this class does not honor the timeout argument in the
 * {@code get} method). Subsequent get calls will just return the same value
 * over and over (or throw the same exception).
 * <P>
 * Every methods of this class is safe to call from multiple concurrent threads
 * but they are not required to be synchronization transparent.
 *
 * @author Kelemen Attila
 */
public final class CancelableDeserializer implements Future<Object> {
    private static final Object CANCEL_TOKEN = new Object();

    private final Runnable idempotentTask; // wraps "futureTask"
    private final RunnableFuture<Object> futureTask;
    private final LoaderTask loaderTask;

    /**
     * Creates a new task reading a serialized object from the given file.
     * <P>
     * To get the deserialized object, call {@link #get() get()}.
     *
     * @param path the path to file containing the serialized object. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public CancelableDeserializer(Path path) {
        this.loaderTask = new LoaderTask(path);
        this.futureTask = new FutureTask<>(loaderTask);
        this.idempotentTask = new IdempotentTask(futureTask);
    }

    private static Object unwrapResult(Object result) {
        if (result == CANCEL_TOKEN) {
            throw new CancellationException();
        }
        else {
            return result;
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: Interrupt will have no effect on this task.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        loaderTask.cancel();
        return futureTask.cancel(false);
    }

    /**
     * Reads the first serialized object from the file specified at construction
     * time, deserializes it and returns it.
     * <P>
     * If this method has already been called, it will just wait for the
     * previously called {@code get} method to return and return the same
     * object.
     *
     * @return the serialized object read from the file in its deserialzied
     *   form. Since {@code null} values cannot be serialized, this method
     *   never returns {@code null}.
     *
     * @throws CancellationException thrown if this task was canceled
     * @throws ExecutionException thrown if there was an error while trying to
     *   load the file. The actual exception thrown when reading the file is
     *   the cause of this exception and the cause of this exception is never
     *   {@code null}.
     * @throws InterruptedException thrown if the current thread was
     *   interrupted. Note that the {@code get} method actually reading the
     *   file will not respond to intertupts. To cancel loading the file
     *   invoke the {@link #cancel(boolean) cancel} method on a separate thread.
     */
    @Override
    public Object get() throws ExecutionException, InterruptedException {
        idempotentTask.run();
        return unwrapResult(futureTask.get());
    }

    /**
     * Does the exact same thing as the {@link #get() get()} method and ignores
     * the timeout argument.
     * <P>
     * Reads the first serialized object from the file specified at construction
     * time, deserializes it and returns it.
     * <P>
     * If this method has already been called, it will just wait for the
     * previously called {@code get} method to return and return the same
     * object.
     *
     * @param timeout this argument is ignored
     * @param unit this argument is ignored
     * @return the serialized object read from the file in its deserialzied
     *   form. Since {@code null} values cannot be serialized, this method
     *   never returns {@code null}.
     *
     * @throws CancellationException thrown if this task was canceled
     * @throws ExecutionException thrown if there was an error while trying to
     *   load the file. The actual exception thrown when reading the file is
     *   the cause of this exception and the cause of this exception is never
     *   {@code null}.
     * @throws InterruptedException thrown if the current thread was
     *   interrupted. Note that the {@code get} method actually reading the
     *   file will not respond to intertupts. To cancel loading the file
     *   invoke the {@link #cancel(boolean) cancel} method on a separate thread.
     */
    @Override
    public Object get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException {
        return get();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isCancelled() {
        return futureTask.isCancelled();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isDone() {
        return futureTask.isDone();
    }

    private static class LoaderTask implements Callable<Object> {
        private final Path path;
        private final AtomicReference<Object> inputRef;

        public LoaderTask(Path path) {
            ExceptionHelper.checkNotNullArgument(path, "path");

            this.path = path;
            this.inputRef = new AtomicReference<>();
        }

        @Override
        public Object call() throws IOException, ClassNotFoundException {
            try (InputStream fileInput = Files.newInputStream(path)) {
                if (inputRef.compareAndSet(null, fileInput)) {
                    return new ObjectInputStream(fileInput).readObject();
                }
                else {
                    return CANCEL_TOKEN;
                }
            } finally {
                // Do not retain unnecessary references.
                inputRef.set(null);
            }
        }

        public void cancel() {
            Object rawInput = inputRef.getAndSet(CANCEL_TOKEN);
            if (rawInput instanceof InputStream) {
                final InputStream inputStream = (InputStream)rawInput;
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            // I'm not sure if closing this file asynchronously
                            // is safe to do, however I have found no other way
                            // to cancel reading a file.
                            //
                            // Actually if the implementation simply relies on
                            // closing the underlying file handle and expect
                            // the read methods to fail with invalid handle
                            // value, then this is not safe.
                            inputStream.close();
                        } catch (IOException ex) {
                            // Ignore and hide this exception as there is nothing to do
                            // Perhaps it should be logged.
                        }
                    }
                }.start();
            }
        }
    }
}
