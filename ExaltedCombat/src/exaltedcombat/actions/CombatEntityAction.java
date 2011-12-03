package exaltedcombat.actions;

import java.io.*;

/**
 * Describes an action what an entity in Exalted combat can make.
 * This includes actions like joining a combat, attacking, etc. Note that this
 * action is for providing history for the user and not for undoing actions.
 * <P>
 * All the instances must be serializable and maintain backward compatibility
 * because instances of this class will be saved. Failing to maintain backward
 * compatibility will cause save files failing to load. It is not required
 * to maintain forward compatibility (older versions of the code being able
 * to load newer version of the serialized instance) but desirable if possible.
 * <P>
 * The best and easiest way to maintain backward compatibilty: <B>Use a
 * serialization proxy unless there is a very good reason to do otherwise.</B>
 * <P>
 * Note that instances of this class are required to be thread-safe and
 * transparent to any synchronization. So classes implementing this interface
 * are recommended to be immutable.
 * <P>
 * Classes implementing this interface should consider extending
 * {@link AbstractEntityAction} instead.
 *
 * @see AbstractEntityAction
 * @author Kelemen Attila
 */
public interface CombatEntityAction extends Serializable {
    /**
     * The tick in which the action occured. This tick may not be the tick in
     * which the entity were before doing this particular action but the time
     * of the combat when it was done.
     *
     * @return the tick in which this action occured. This method always returns
     *   a non-negative integer.
     */
    public int getActionTick();

    /**
     * Returns a description provided by the user. Since the description was
     * entered by a human user, this {@code String} does not need to be
     * localizable.
     * <P>
     * In case the returned string contains multiple lines, the expected line
     * separating character is a single {@code '\n'}.
     *
     * @return the user defined description of this action. This method never
     *   returns {@code null} (returns an empty string instead).
     */
    public String getUserDescription();

    /**
     * Returns a short description of this action. The description must not be
     * longer than a few words so it can be used as a caption. The returned
     * string is expected to be localized and can be displayed to the user
     * directly.
     * <P>
     * The returned string must avoid containing multiple lines.
     *
     * @return a concise description of this action. This method never
     *   returns {@code null} (returns an empty string instead).
     */
    public String getPresentationCaption();

    /**
     * Returns a possibly longer description of this action. This description
     * should contain every possible information relevant to a human user.
     * The returned string is expected to be localized and can be displayed to
     * the user directly.
     * <P>
     * In case the returned string contains multiple lines, the expected line
     * separating character is a single {@code '\n'}.
     *
     * @return a possibly longer description of this action. This method never
     *   returns {@code null} (returns an empty string instead).
     */
    public String getPresentationText();
}
