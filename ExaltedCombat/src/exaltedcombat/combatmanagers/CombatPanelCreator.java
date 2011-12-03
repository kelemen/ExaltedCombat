package exaltedcombat.combatmanagers;

import java.awt.Component;

/**
 * Provides the required panels for a {@link HorizontalCombatPanel}. There
 * are actually two kinds of panel required by the combat panel:
 * <ul>
 *  <li>The empty panel which displays ticks when no entity acts.</li>
 *  <li>The panel displaying a given entity.</li>
 * </ul>
 * Both kind of panels need to be implemented so that they look good when
 * resized (although they may need a certain minimum size).
 * <P>
 * Methods of this interface are always expected to be called on the AWT event
 * dispatching thread.
 *
 * @param <EntityType> the type of an entity of the combat panel
 * @param <EntityComponent> the type of the component used to display a combat
 *   entity on the combat panel. This type must subclass {@link Component}.
 *
 * @author Kelemen Attila
 */
public interface CombatPanelCreator<EntityType, EntityComponent extends Component> {

    /**
     * Creates a swing component which displays a given entity. Usually it is
     * important for such components to display a short name, so it can easily
     * be identified by the user.
     *
     * @param entity the combat entity which the returned panel need to display.
     *   This argument can be {@code null} if and only if {@code null} is
     *   an allowed combat entity (although this is not recommended).
     *
     * @return the component displaying the specified entity. This method must
     *   never return {@code null}.
     */
    public EntityComponent createEntityPanel(EntityType entity);

    /**
     * Creates a swing component which shows ticks when no entity acts. This in
     * general should be a panel which is easily distinguishable from the
     * components displaying the entities.
     * <P>
     * Note: This method may change to take the tick as the argument, so the
     * panel is able to display the tick.
     *
     * @return the swing component displaying a tick when no entity acts. This
     *   method must never return {@code null}.
     */
    public Component createEmptyPanel();
}
