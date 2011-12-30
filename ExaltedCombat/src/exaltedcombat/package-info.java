/**
 * Contains the main frame and the entry point of ExaltedCombat.
 *
 * <h3>Entities</h3>
 * Entities in ExaltedCombat are the objects able to participate in a combat.
 * Every entity is allowed to join a combat without restriction. The class
 * defining the entity is {@link exaltedcombat.models.impl.CombatEntity CombatEntity}.
 *
 * <h3>Models and the Event Manager</h3>
 * ExaltedCombat uses backing models to store the information and displays
 * this model. Since modifications to the model can scattered through the code
 * models can notify users through listeners. However to avoid possible infinite
 * loops when modifying the model only the main frame registers listeners with
 * the model directly and those registered listeners only forward the events to
 * an {@link exaltedcombat.events.EventManager event manager}.
 * <P>
 * The event manager is used to actually track which event caused which event,
 * so using it makes avoiding infinite loop in events relatively easy.
 * <P>
 * There is only a single instance of the event manager and there is also a
 * single {@link exaltedcombat.models.impl.CombatEntityWorldModel world model}.
 * These are created in the main frame of ExaltedCombat.
 *
 * <h3>Undo actions</h3>
 * To allow undoing an action possible ExaltedCombat uses an
 * {@link javax.swing.undo.UndoManager} and adds the action as an edit to it.
 * <P>
 * Only actions directly related to a combat can be undone as of the current
 * implementation. Such as entering the combat, leaving the combat, taking an
 * action in the combat. Deleting and creating a new entity is not tracked and
 * cannot be undone.
 *
 * <h3>Panels</h3>
 * The main frame completely relies panels found in the
 * {@code exaltedcombat.panels} package and without them it is only an empty
 * frame with a menubar. These panels are connected by the models and the event
 * manager.
 *
 * <h3>Saving the state</h3>
 * The state of ExaltedCombat can be stored in a single serializable object:
 * {@link exaltedcombat.save.SaveInfo}. This object is stored in a file in a
 * serialized form.
 * <P>
 * As of the current implementation, ExaltedCombat is paranoid when it comes to
 * saving its state. Once a combat was named (which effectively specifies the
 * file to use), it will automatically be saved before starting a new combat or
 * exiting the application. If the combat was not yet named, it will nag the
 * user to save it before discarding it and there is only one way for the user
 * to discard an unnamed combat: exiting the application and ignoring every
 * warning.
 *
 * <h3>Terminating the application</h3>
 * Under normal circumstances, {@code Sytem.exit} is not called to terminate the
 * application. Instead forcefully terminating the application when the main
 * frame is closed, it shuttowns the used executors and waits for every
 * non-daemon thread to terminate.
 * <P>
 * However if not everything goes smoothly and an error does not allow
 * ExaltedCombat to terminate, it would be a major issue. This is major issue
 * because the only indiciation of this happening is that the java process is
 * still running. This can be quite hard detect for the casual user and would
 * waste valuable resources (lots of memory, especially a problem on 32 bit
 * machines). To workaround this issue ExaltedCombat is able to detect that it
 * has not terminated and notify the user, allowing him/her to forcefully
 * ({@code System.exit}) terminate the JVM. For further reference on how it is
 * done see {@link exaltedcombat.dialogs.TooLongTerminateFrame}.
 *
 * @see exaltedcombat.TickCombatFrame
 */
package exaltedcombat;
