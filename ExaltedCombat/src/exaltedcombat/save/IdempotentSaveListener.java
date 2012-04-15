package exaltedcombat.save;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim.utils.ExceptionHelper;

/**
 * This interface wraps a {@link SaveDoneListener} and forward calls to it
 * at most once. That is: This interface will only forward the first
 * {@link #onFailedSave(Throwable) onFailedSave(Throwable)} or
 * {@link #onSuccessfulSave(java.nio.file.Path) onSuccessfulSave(Path)}
 * method call (calling one of these methods will not allow the other method
 * to forward event).
 * <P>
 * This interface can be useful when a task need to canceled. Using this method
 * it is possible to notify the task immediately by invoking the appropriate
 * method of the {@code SaveDoneListener}.
 *
 * @see ExaltedSaveHelper#createSaveRewTask(Path, SaveInfo, boolean, SaveDoneListener) ExaltedSaveHelper.createSaveRewTask(Path, SaveInfo, boolean, SaveDoneListener)
 * @see ExaltedSaveHelper#createSaveRewTask(String, SaveInfo, boolean, SaveDoneListener) ExaltedSaveHelper.createSaveRewTask(String, SaveInfo, boolean, SaveDoneListener)
 * @author Kelemen Attila
 */
public final class IdempotentSaveListener implements SaveDoneListener {
    private final SaveDoneListener listener;
    private final AtomicBoolean done;

    /**
     * Creates a listener forwarding its method calls to the given listener.
     *
     * @param listener the listener to which method calls will be forwarded.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public IdempotentSaveListener(SaveDoneListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.listener = listener;
        this.done = new AtomicBoolean(false);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void onFailedSave(Throwable saveError) {
        if (done.compareAndSet(false, true)) {
            listener.onFailedSave(saveError);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void onSuccessfulSave(Path path) {
        if (done.compareAndSet(false, true)) {
            listener.onSuccessfulSave(path);
        }
    }

}
