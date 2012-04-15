package exaltedcombat.save;

import java.nio.file.Path;

/**
 * The interface to be notified when saving the state of ExaltedCombat has been
 * completed. Saving may either complete
 * {@link #onSuccessfulSave(java.nio.file.Path) successfully} or
 * {@link #onFailedSave(java.lang.Throwable) fails} and only one of the methods
 * of this interface is allowed to be called by a single task.
 * <P>
 * The method of this interface does not need to be safe to be called from
 * multiple threads concurrently but must return as fast as possible without
 * blocking to avoid causing artificial throttle.
 *
 * @see ExaltedSaveHelper#createSaveRewTask(Path, SaveInfo, boolean, SaveDoneListener) ExaltedSaveHelper.createSaveRewTask(Path, SaveInfo, boolean, SaveDoneListener)
 * @see ExaltedSaveHelper#createSaveRewTask(String, SaveInfo, boolean, SaveDoneListener) ExaltedSaveHelper.createSaveRewTask(String, SaveInfo, boolean, SaveDoneListener)
 * @author Kelemen Attila
 */
public interface SaveDoneListener {
    /**
     * Invoked when saving the state of ExaltedCombat has failed.
     * <P>
     * Note that if this method was called the
     * {@link #onSuccessfulSave(java.nio.file.Path) onSuccessfulSave(Path)}
     * method will not be called.
     *
     * @param saveError the error describing the failure occurred while saving
     *   the state of ExaltedCombat. This argument cannot be {@code null}.
     */
    public void onFailedSave(Throwable saveError);

    /**
     * Invoked when saving the state of ExaltedCombat has been completed
     * successfully.
     * <P>
     * Note that if this method was called the
     * {@link #onFailedSave(java.lang.Throwable) onFailedSave(Throwable)} method will not be called.
     *
     * @param path the path to the file where the saved state is being stored.
     *   This argument cannot be {@code null}.
     */
    public void onSuccessfulSave(Path path);
}
