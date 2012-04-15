package exaltedcombat.models;

/**
 * Defines the possible states of a combat in Exalted. The starting state of
 * a combat in Exalted is the {@link #JOIN_PHASE join phase}.
 *
 * @see CombatModel
 * @author Kelemen Attila
 */
public enum CombatState {
    /**
     * The starting state of a combat in Exalted. In this state entities are
     * rolling for join combat and a special rule defines when the entities
     * will act depending on the number of success they roll. Once they finish
     * rolling for join combat the battle begins and the combat enters into the
     * next state: {@link #COMBAT_PHASE}.
     */
    JOIN_PHASE,

    /**
     * The second and last phase of the combat when entities act. Besides
     * the first join combat rolls, every action of the entities takes place in
     * this phase. Note that entities may also join the combat in this phase
     * using a different rule. This phase is only finished when the combat
     * actually ends.
     */
    COMBAT_PHASE
}
