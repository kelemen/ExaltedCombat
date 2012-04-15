package exaltedcombat.models;

/**
 * The interface to listen for changes in the state of a combat. When registered
 * with a {@link CombatModel}, the model will notify this listener when the
 * state in that combat changes.
 *
 * @see CombatModel
 * @author Kelemen Attila
 */
public interface CombatStateChangeListener {

    /**
     * Invoked when the state of the combat changes in a combat. When this
     * method is called the state of the combat has already changed.
     *
     * @param state the new state of the combat. This argument cannot be
     *   {@code null}.
     */
    public void onChangeCombatState(CombatState state);
}
