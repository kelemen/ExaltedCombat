package exaltedcombat.save;

/**
 * The interface to be notified when a saved state of ExaltedCombat has been
 * loaded or has failed to be loaded.
 * <P>
 * The methods of this interface does not need to be safe to be called from
 * multiple threads concurrently but must return as fast as possible without
 * blocking to avoid causing artificial throttle.
 *
 * @see ExaltedSaveHelper#createLoadRewTask(java.nio.file.Path, exaltedcombat.save.LoadDoneListener) ExaltedSaveHelper.createLoadRewTask(Path, LoadDoneListener)
 * @author Kelemen Attila
 */
public interface LoadDoneListener {
    /**
     * Invoked when a previously saved state of ExaltedCombat has failed to be
     * loaded.
     * <P>
     * If this method was called the
     * {@link #onSuccessfulLoad(exaltedcombat.save.SaveInfo) onSuccessfulLoad(SaveInfo)}
     * method will not be called.
     *
     * @param error the error describing the failure occurred while loading
     *   the previously saved state of ExaltedCombat. This argument cannot be
     *   {@code null}.
     */
    public void onFailedLoad(Throwable error);

    /**
     * Invoked when a saved state of ExaltedCombat has been loaded successfully.
     * <P>
     * If this method was called the
     * {@link #onFailedLoad(java.lang.Throwable) onFailedLoad(Throwable)}
     * method will not be called.
     *
     * @param saveInfo the saved state of ExaltedCombat. This argument cannot
     *   be {@code null}.
     */
    public void onSuccessfulLoad(SaveInfo saveInfo);
}
