/**
 * Contains classes and interfaces used for the event handling mechanism in
 * ExaltedCombat.
 * <P>
 * The main and most import interface of this package is
 * {@link exaltedcombat.events.EventManager}. It is intended to be used for
 * the real event handling: Event handlers actually doing some work are need
 * to be registered with an {@code EventManager} and every other event listener
 * registered with Swing components need to just forward their events to this
 * {@code EventManager}. For these to work it is also important that there
 * should be only one {@code EventManager} instance in the JVM otherwise it
 * cannot manage events effectievly.
 * <P>
 * The actual reason to use an {@code EventManager} instead just handling
 * Swing events directly is because {@code EventManager} can keep track of
 * which event cause which events. This is information is essential to detect
 * circular updates. Circular updates can occur for example because there are
 * multiple places on the main frame to select an entity. Selecting the entity
 * will select the entity in all components eventually and since this means that
 * the selection has changed on these other components, these other components
 * may eventually (implicitly) reselect the entity on the originating component.
 * Of course, this could be avoided if components would check first if they
 * really need to update the selection. However I have found this way (requiring
 * the need for to check, if they really to update) more error prone. Also it
 * may not always be straightforward to implement if there would be rounding
 * involved (obviously not with selecting an entity and there is no such thing
 * in ExaltedCombat yet).
 * <P>
 * Note that many of these classes and interfaces need to be redesigned. Once
 * their were redesigned and prooved to be good, they should be moved to JTrim.
 *
 * @see exaltedcombat.events.RecursionStopperEventManager
 */
package exaltedcombat.events;
