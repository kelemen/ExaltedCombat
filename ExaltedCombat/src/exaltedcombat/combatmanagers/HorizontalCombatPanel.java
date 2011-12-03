package exaltedcombat.combatmanagers;

import exaltedcombat.components.*;
import exaltedcombat.models.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jtrim.concurrent.*;
import org.jtrim.swing.concurrent.*;
import org.jtrim.utils.*;

/**
 * Defines a swing component displaying the ticks of a tick based combat
 * in a horizontal queue.
 * <P>
 * The ticks are represented by rectangles and within each rectangle the
 * entities acting on the same tick share this rectangle by horizontally
 * splitting the rectangle of the tick. The ticks are ordered horizontally
 * with small gaps between each other (currently this gap cannot be set). Note
 * that currently only left to right ordering of ticks is supported.
 * <P>
 * When displaying the ticks this class relies on a {@link CombatPanelCreator}
 * instance to provide Swing components which can display entities or empty
 * ticks. Therefore the
 * {@link #setPanelCreator(exaltedcombat.combatmanagers.CombatPanelCreator) setPanelCreator(CombatPanelCreator&lt;EntityType, EntityComponent&gt;)}
 * method must be called on new instances to be effectively usable. If this
 * method is not called entities will be visually indistinguishable by the user.
 * <P>
 * Newly created instances of this class contains no entites on the combat
 * panel. The entities on the combat panel can be organized by calling the
 * methods derived from
 * {@link CombatPositionModel CombatPositionModel&lt;EntityType&gt;}.
 * <P>
 * The combat panel can notify the user of events when the state of the combat
 * changes. To be notified of such changes register a new listener by calling
 * {@link #addCombatPosListener(exaltedcombat.models.CombatPosEventListener) addCombatPosListener(CombatPosEventListener&lt;EntityType&gt;)}
 * It is explicitly allowed to modify the state of the calling combat panel
 * from the registered event handlers.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread (even the methods
 * of {@code CombatPositionModel<EntityType>}).
 * <P>
 * TODO: This component needs some refactoring, so that a vertical component
 * can be easily implemented. Also the code is way too complex due to the
 * synchronization requirement (since it is not documented why it is needed).
 *
 * @param <EntityType> the type of an entity of the combat panel
 * @param <EntityComponent> the type of the component used to display a combat
 *   entity on the combat panel. This type must subclass {@link Component}.
 *
 * @author Kelemen Attila
 */
public class HorizontalCombatPanel<EntityType, EntityComponent extends Component>
extends
        JComponent
implements
        CombatPositionModel<EntityType> {

    private static final long serialVersionUID = 5602715938842371757L;

    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    private static final int ENTITY_GAP = 2;

    private int boxWidth = 75;
    private int hGap = 5;
    private int vGap = 5;

    private final UpdateTaskExecutor rebuildExecutor;
    private final Map<EntityType, EntityComponent> components;

    private final CombatPositionModel<EntityType> model;
    private CombatPanelCreator<? super EntityType, ? extends EntityComponent> panelCreator;

    /**
     * Creates a combat panel with no entities currently on the panel
     * (i.e.: with only empty ticks). The current newly created combat panel
     * is at tick zero.
     * <P>
     * Note that the
     * {@link #setPanelCreator(exaltedcombat.combatmanagers.CombatPanelCreator) setPanelCreator(CombatPanelCreator&lt;EntityType, EntityComponent&gt;)}
     * method should be called to effectively display the ticks of this
     * combat panel.
     */
    public HorizontalCombatPanel() {
        this.components = new HashMap<>();
        this.model = new TickBasedEventManager<>();
        this.rebuildExecutor = new SwingUpdateTaskExecutor(true);
        this.panelCreator = new DefaultPanelCreator<>();

        setLayout(new RelaxedFlowLayoutManager(hGap));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateAllTicks();
            }
        });
    }

    /**
     * Sets or overwrites a previously associated panel creator used to display
     * the ticks of this combat. This method should be called on every new
     * instance this combat panel to effectively display the ticks of the
     * combat.
     *
     * @param panelCreator the object providing the Swing components which can
     *   be used to display the ticks of the combat. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the passed argument is
     *   {@code null}
     */
    public void setPanelCreator(CombatPanelCreator<? super EntityType, ? extends EntityComponent> panelCreator) {
        ExceptionHelper.checkNotNullArgument(panelCreator, "panelCreator");

        this.panelCreator = panelCreator;
        removeAll();
        updateAllTicks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeCombatPosListener(CombatPosEventListener<EntityType> listener) {
        model.removeCombatPosListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addCombatPosListener(CombatPosEventListener<EntityType> listener) {
        model.addCombatPosListener(listener);
    }

    private void revalidateAndRepaint() {
        revalidate();
        repaint();
    }

    private void ensureDefined(EntityType entity) {
        if (!components.containsKey(entity)) {
            defineEntity(entity, panelCreator.createEntityPanel(entity));
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<EntityType> getEntities(int tick) {
        return model.getEntities(tick);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Map<Integer, List<EntityType>> getEntities() {
        return model.getEntities();
    }

    private GridLayout createTickLayout(int entityCount) {
        return new GridLayout(entityCount > 0 ? entityCount : 1, 1, 0, 2);
    }

    private void setTickLayout(TickContainer tickContainer, int entityCount) {
        int cellCount = Math.max(entityCount, 1);
        LayoutManager currentLayout = tickContainer.getLayout();
        if (currentLayout instanceof GridLayout) {
            GridLayout gridLayout = (GridLayout)currentLayout;
            if (gridLayout.getRows() * gridLayout.getColumns() != cellCount) {
                tickContainer.setLayout(createTickLayout(cellCount));
            }
        }
        else {
            tickContainer.setLayout(createTickLayout(cellCount));
        }
    }

    private boolean replaceComponent(JComponent parent, int index, Component newComponent) {
        boolean validIndex;
        synchronized (parent.getTreeLock()) {
            validIndex = index >= 0 && index < parent.getComponentCount();
            if (validIndex) {
                parent.remove(index);
                parent.add(newComponent, index);
            }
        }

        if (!validIndex) {
            rebuildEverything();
        }

        return validIndex;
    }

    private int getComponentCountSync() {
        synchronized (getTreeLock()) {
            return getComponentCount();
        }
    }

    private TickContainer createTickContainer() {
        TickContainer tickContainer = new TickContainer();
        updateTickContainerSize(tickContainer);
        return tickContainer;
    }

    private void updateTickContainerSize(TickContainer tickContainer) {
        int panelHeight = Math.max(getHeight() - 2 * vGap, 1);
        int panelWidth = boxWidth;

        if (tickContainer.getHeight() != panelHeight || tickContainer.getWidth() != panelWidth) {
            tickContainer.setSize(panelWidth, panelHeight);
            tickContainer.setPreferredSize(new Dimension(panelWidth, panelHeight));
        }
    }

    private void rebuildEverything() {
        rebuildExecutor.execute(new Runnable() {
            @Override
            public void run() {
                removeAll();
                updateAllTicks();
            }
        });
    }

    private void updateTickComponent(int tick) {
        int index = tick - model.getCurrentTick();
        if (index < 0) {
            return;
        }

        Component subComponent = null;
        synchronized (getTreeLock()) {
            if (index < getComponentCount()) {
                subComponent = getComponent(index);
            }
        }

        if (subComponent != null) {
            updateTickComponent(subComponent, index);
        }
    }

    private static int getComponentCountSync(JComponent component) {
        synchronized (component.getTreeLock()) {
            return component.getComponentCount();
        }
    }

    private static Component getSubComponentSync(JComponent parent, int index) {
        if (index < 0) {
            return null;
        }

        synchronized (parent.getTreeLock()) {
            if (parent.getComponentCount() > index) {
                return parent.getComponent(index);
            }
        }

        return null;
    }

    private void addEntityToTickContainer(TickContainer container, EntityComponent component) {
        container.add(component != null ? component : new DummyPanel());
        container.setEmptyCell(false);
    }

    private void setEmptyTick(TickContainer container) {
        if (!container.isEmptyCell() || getComponentCountSync(container) != 1) {
            Component emptyPanel = panelCreator.createEmptyPanel();
            if (emptyPanel == null) {
                emptyPanel = new DefaultEmptyTickPanel();
            }
            container.removeAll();
            container.add(emptyPanel);
            container.setEmptyCell(true);
        }
    }

    private boolean updateTickComponent(Component subComponent, int index) {
        TickContainer tickContainer;
        if (subComponent instanceof TickContainer) {
            tickContainer = (TickContainer)subComponent;
        }
        else {
            tickContainer = createTickContainer();
            if (!replaceComponent(this, index, tickContainer)) {
                return false;
            }
        }

        updateTickContainerSize(tickContainer);

        int tickOffset = model.getCurrentTick();
        List<EntityType> entities = model.getEntities(tickOffset + index);
        int entityCount = entities.size();
        setTickLayout(tickContainer, entityCount);

        Component[] subComponents;
        synchronized (tickContainer.getTreeLock()) {
            subComponents = tickContainer.getComponents();
        }

        int currentIndex = 0;
        for (EntityType entity: entities) {
            if (currentIndex >= subComponents.length) {
                break;
            }

            Component currentComponent = getSubComponentSync(tickContainer, currentIndex);
            EntityComponent entityComponent = components.get(entity);
            if (currentComponent == null) {
                int currentComponentCount;
                synchronized (tickContainer.getTreeLock()) {
                    currentComponentCount = tickContainer.getComponentCount();
                }

                if (currentComponentCount != currentIndex) {
                    rebuildEverything();
                    return false;
                }

                addEntityToTickContainer(tickContainer, entityComponent);
            }
            else if (currentComponent != entityComponent) {
                tickContainer.setEmptyCell(false);
                if (!replaceComponent(tickContainer, currentIndex, entityComponent)) {
                    return false;
                }
            }

            currentIndex++;
        }

        if (entityCount == 0) {
            setEmptyTick(tickContainer);
        }
        else if (entityCount < subComponents.length) {
            synchronized (tickContainer.getTreeLock()) {
                if (subComponents.length != tickContainer.getComponentCount()) {
                    rebuildEverything();
                    return false;
                }

                for (int i = subComponents.length - 1; i >= entityCount; i--) {
                    tickContainer.remove(i);
                }
            }
        }
        else if (entityCount > subComponents.length) {
            List<EntityType> remEntities = entities.subList(subComponents.length, entityCount);
            for (EntityType entity: remEntities) {
                addEntityToTickContainer(tickContainer, components.get(entity));
            }
        }

        return true;
    }

    private void updateAllTicks() {
        int tickCount = getWidth() / (hGap + boxWidth) + 1;
        if (tickCount == 0) {
            removeAll();
            revalidateAndRepaint();
            return;
        }

        while (getComponentCountSync() != tickCount) {
            int componentCount;
            synchronized (getTreeLock()) {
                componentCount = getComponentCount();
                if (componentCount > tickCount) {
                    remove(componentCount - 1);
                }
            }

            if (componentCount < tickCount) {
                add(createTickContainer());
            }
        }

        Component[] subComponents;
        synchronized (getTreeLock()) {
            subComponents = getComponents();
        }

        if (subComponents.length != tickCount) {
            // Concurrent component modification.
            // At least try to redo everything.
            rebuildEverything();
            return;
        }

        for (int i = 0; i < subComponents.length; i++) {
            if (!updateTickComponent(subComponents[i], i)) {
                return;
            }
        }

        revalidateAndRepaint();
    }

    /**
     * Returns the Swing component used to display the given entity. Note that
     * it is possible that the returned component is currently visible and
     * displayed but can also be hidden.
     *
     * @param entity the entity whose displaying component is to be retrieved.
     *   This argument cannot be {@code null}.
     * @return the Swing component used to display the given entity or
     *   {@code null} if there is no such component associated with the
     *   specified entity
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public EntityComponent getEntityComponent(EntityType entity) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        return components.get(entity);
    }

    private void defineEntity(EntityType entity, EntityComponent component) {
        components.put(entity, component);

        int tick = getTickOfEntity(entity);
        if (tick >= 0) {
            updateTickComponent(tick);
            revalidateAndRepaint();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getNumberOfEntities() {
        return model.getNumberOfEntities();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterable<List<EntityType>> getTicks() {
        return model.getTicks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterable<List<EntityType>> getTicks(int startTick) {
        return model.getTicks(startTick);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getCurrentTick() {
        return model.getCurrentTick();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getTickOfEntity(EntityType entity) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");
        return model.getTickOfEntity(entity);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int moveToTick(EntityType entity, int tick) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        int prevCurrentTick = model.getCurrentTick();
        ensureDefined(entity);
        int prevTick = model.moveToTick(entity, tick);

        if (prevCurrentTick == model.getCurrentTick()) {
            updateTickComponent(tick);
            if (prevTick >= 0) {
                updateTickComponent(prevTick);
            }
        }
        else {
            updateAllTicks();
        }

        revalidateAndRepaint();
        return prevTick;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeAllEntities() {
        if (model.getNumberOfEntities() > 0) {
            components.clear();
            model.removeAllEntities();
            updateAllTicks();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int removeEntity(EntityType entity) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        int prevCurrentTick = model.getCurrentTick();
        components.remove(entity);
        int tick = model.removeEntity(entity);
        if (tick >= 0) {
            if (prevCurrentTick == model.getCurrentTick()) {
                updateTickComponent(tick);
            }
            else {
                updateAllTicks();
            }

            revalidateAndRepaint();
        }
        return tick;
    }

    private static class TickContainer extends JPanel {
        private static final long serialVersionUID = 5059244710936016180L;
        private boolean emptyCell;

        public TickContainer() {
            setLayout(new GridLayout(1, 1, 0, ENTITY_GAP));
            setBackground(TRANSPARENT_COLOR);
            setOpaque(false);
            this.emptyCell = false;
        }

        public boolean isEmptyCell() {
            return emptyCell;
        }

        public void setEmptyCell(boolean emptyCell) {
            this.emptyCell = emptyCell;
        }
    }

    private static class DummyPanel extends JPanel {
        private static final long serialVersionUID = -6735151643398741753L;

        public DummyPanel() {
            super(new GridLayout(1, 1, 0, 0));

            setBackground(Color.WHITE);
            setForeground(Color.BLACK);

            JLabel label = new JLabel("NULL");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setForeground(Color.BLACK);
            add(label);
        }
    }

    private static class DefaultPanelCreator<EntityType, ComponentType extends Component>
    implements
            CombatPanelCreator<EntityType, ComponentType> {

        @Override
        public ComponentType createEntityPanel(EntityType entity) {
            return null;
        }

        @Override
        public Component createEmptyPanel() {
            Component result = new DefaultEmptyTickPanel();
            result.setBackground(Color.RED);
            return result;
        }
    }
}
