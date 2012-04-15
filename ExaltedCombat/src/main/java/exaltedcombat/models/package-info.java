/**
 * Contains interfaces for models and related classes and interfaces used in
 * ExaltedCombat. This package does not contain implementations of these models.
 * The implementation for models are found in the {@code exaltedcombat.models.impl}
 * package (except for the {@link exaltedcombat.models.CombatPositionModel}
 * which implementation can be found in the {@code exaltedcombat.combatmanagers}
 * package).
 * <P>
 * Models in ExaltedCombat must have a way to notify the user about changes
 * occurring in the model. This is important to do to enable remote parts of the
 * code to cooperate.
 *
 * <h3>Important note on models</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 */
package exaltedcombat.models;
