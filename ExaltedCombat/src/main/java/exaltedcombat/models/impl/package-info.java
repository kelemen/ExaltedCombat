/**
 * Contains implementations for the models in ExaltedCombat. The lone exception
 * for this is the {@link exaltedcombat.models.CombatPositionModel}
 * which implementation can be found in the {@code exaltedcombat.combatmanagers}
 * package).
 * <P>
 * Note that there is a semi-implementations in this package. Namely:
 * {@link exaltedcombat.models.impl.CombatEntityWorldModel}. This interface was
 * not generalized because there was no reason to do so yet (and probably never
 * will), so it would just add to the complexity. Also there are implementations
 * not having implemented interfaces because there was no good reason to
 * generalize them as well.
 *
 * <h3>Important note on models</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 */
package exaltedcombat.models.impl;
