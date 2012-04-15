/**
 * Contains the panels of the main frame of ExaltedCombat. These panels can
 * be connected by {@link exaltedcombat.events.EventManager event managers},
 * models and {@link javax.swing.undo.UndoManager undo managers}.
 * <P>
 * Note that these panels are not necessary to be displayed on a single frame
 * and not all need to be displayed at all. It would be completely feasible to
 * connect them even if they were added to different frames. Also except for
 * the {@link exaltedcombat.panels.CombatPositionPanel} they can be added
 * multiple times to a frame and they would still work.
 */
package exaltedcombat.panels;
