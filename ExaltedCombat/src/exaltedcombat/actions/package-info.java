/**
 * Contains classes defining the actions of an
 * {@link exaltedcombat.models.impl.CombatEntity entity}. These classes are
 * intended to be used to display the user the history of an entity in a combat
 * and are not for reverting those actions.
 * <P>
 * The base interface for every action is: {@link exaltedcombat.actions.CombatEntityAction}.
 * <P>
 * <B>Important note</B>: Instances of these classes are need to be serializable
 * because they are saved with the entities in a file. It is therefore
 * especially important to keep the serialization of these classes backward
 * compatible.
 * <P>
 * The best and easiest way to maintain backward compatibilty: <B>Use a
 * serialization proxy unless there is a very good reason to do otherwise.</B>
 */
package exaltedcombat.actions;
