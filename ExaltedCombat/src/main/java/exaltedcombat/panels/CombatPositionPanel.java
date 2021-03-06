package exaltedcombat.panels;

import exaltedcombat.combatmanagers.CombatPanelCreator;
import exaltedcombat.combatmanagers.DefaultEmptyTickPanel;
import exaltedcombat.combatmanagers.HorizontalCombatPanel;
import exaltedcombat.components.BorderedBorder;
import exaltedcombat.components.JSolidPanel;
import exaltedcombat.dialogs.ExaltedDialogHelper;
import exaltedcombat.events.EntitySelectChangeArgs;
import exaltedcombat.events.ExaltedEvent;
import exaltedcombat.events.WorldEvent;
import exaltedcombat.models.CombatPosEventListener;
import exaltedcombat.models.CombatPositionModel;
import exaltedcombat.models.DelegatedCombatPositionModel;
import exaltedcombat.models.impl.CombatEntities;
import exaltedcombat.models.impl.CombatEntity;
import exaltedcombat.utils.ExaltedConsts;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import org.jtrim.event.EventTracker;
import org.jtrim.event.LocalEventTracker;
import org.jtrim.event.TrackedEvent;
import org.jtrim.event.TrackedEventListener;
import org.jtrim.utils.ExceptionHelper;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Defines a Swing panel displaying the ticks of a combat. This panel displays
 * ticks from a left to right order; entities on the same tick are above
 * each other.
 * <P>
 * This implementation needs a {@link CombatEntities population model}
 * to allow selecting an entity and an {@link EventTracker event tracker} to
 * detect changes made to the model. Therefore every instance of this class
 * needs to be initialized after creating by calling the following methods:
 * <ul>
 *  <li>{@link #setPopulationModel(CombatEntities) setPopulationModel(CombatEntities)}</li>
 *  <li>{@link #setEventTracker(EventTracker) setEventTracker(EventTracker)}</li>
 * </ul>
 * Forgetting to call these methods and displaying this panel may cause
 * unchecked exceptions to be thrown. The methods can be called in any order.
 * <P>
 * Notice that this panel can also act as a
 * {@link CombatPositionModel position model}. To view this panel as a combat
 * model use the {@link #getCombatPositionModel() getCombatPositionModel()}
 * method.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class CombatPositionPanel extends JPanel {
    private static final long serialVersionUID = 808077572348694286L;

    private static final LocalizedString COMBAT_PANEL_CAPTION = StringContainer.getDefaultString("COMBAT_PANEL_CAPTION");

    private final HorizontalCombatPanel<CombatEntity, EntityPanel> jCombatPanel;
    private final Border selectedBorder;
    private final Border highlightedBorder;
    private final Border unselectedBorder;

    private LocalEventTracker eventTracker;
    private CombatEntities populationModel;

    /**
     * Creates a new panel without displaying any entities. After creating this
     * instance don't forget to initialize it by calling these methods:
     * <ul>
     *  <li>{@link #setPopulationModel(CombatEntities) setPopulationModel(CombatEntities)}</li>
     *  <li>{@link #setEventTracker(EventTracker) setEventTracker(EventTracker)}</li>
     * </ul>
     */
    public CombatPositionPanel() {
        final int borderSize = 4;
        this.selectedBorder = new BorderedBorder(borderSize, Color.BLACK, Color.WHITE);
        this.highlightedBorder = new BorderedBorder(borderSize, Color.GREEN, Color.RED);
        //this.selectedBorder = new DashedBorder(borderSize, 10, Color.BLACK, Color.WHITE);
        //this.highlightedBorder = new DashedBorder(borderSize, 10, Color.GREEN, Color.RED);
        this.unselectedBorder = BorderFactory.createEmptyBorder(
                borderSize, borderSize, borderSize, borderSize);
        this.jCombatPanel = new HorizontalCombatPanel<>();
        this.eventTracker = null;
        this.populationModel = null;

        initComponents();
        setComponentProperties();
    }

    /**
     * Sets the population model to use by this panel. This panel will not
     * register event listeners with the given model and will use the model only
     * to retrieve information and modify it. Instead it will use events
     * received from the
     * {@link #setEventTracker(EventTracker) event tracker}.
     * <P>
     * This panel will only use the specified model from the AWT event
     * dispatching thread.
     *
     * @param populationModel the population model to use by this panel. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified population model is
     *   {@code null}
     */
    public void setPopulationModel(CombatEntities populationModel) {
        ExceptionHelper.checkNotNullArgument(populationModel, "populationModel");

        this.populationModel = populationModel;
        for (List<CombatEntity> tickEntities: jCombatPanel.getEntities().values()) {
            for (CombatEntity entity: tickEntities) {
                setSelectionOfPanel(entity);
            }
        }
    }

    private void setSelectionOfPanel(CombatEntity entity) {
        EntityPanel panel = getPanelOfEntity(entity);
        if (panel != null) {
            if (populationModel == null) {
                panel.setSelectPanel(false);
            }
            else {
                panel.setSelectPanel(entity == populationModel.getSelection());
            }
        }
    }

    private void setComponentProperties() {
        updateTickLabel();

        jCombatHolderPanel.add(jCombatPanel);
        jCombatPanel.setPanelCreator(new CombatPanelCreator<CombatEntity, EntityPanel>(){
            @Override
            public EntityPanel createEntityPanel(CombatEntity entity) {
                return new EntityPanel(entity);
            }

            @Override
            public Component createEmptyPanel() {
                Component result = new DefaultEmptyTickPanel();
                result.setBackground(Color.RED);
                return result;
            }
        });

        jCombatPanel.addCombatPosListener(new CombatPosEventListener<CombatEntity>() {
            @Override
            public void enterCombat(CombatEntity entity, int tick) {
                updateTickLabel();
                setSelectionOfPanel(entity);
            }

            @Override
            public void leaveCombat(Collection<? extends CombatEntity> entity) {
                updateTickLabel();
            }

            @Override
            public void move(CombatEntity entity, int srcTick, int destTick) {
                updateTickLabel();
            }
        });
    }


    private void updateTickLabel() {
        int displayedtick = jCombatPanel.getCurrentTick() + ExaltedConsts.TICK_OFFSET;
        jCurrentTickLabel.setText(COMBAT_PANEL_CAPTION.format(displayedtick));
    }

    /**
     * Returns a model which reflects this panel. Modifying the returned model
     * will also modify the content this panel displays.
     *
     * @return a model which reflecting this panel. This method never returns
     *   {@code null}.
     */
    public CombatPositionModel<CombatEntity> getCombatPositionModel() {
        // Protect jCombatPanel from unwanted access.
        // Actually the result could be stored in a field to avoid creating
        // new instance with every get but there is not much gain to do so now.
        return new DelegatedCombatPositionModel<>(jCombatPanel);
    }

    private EntityPanel getPanelOfEntity(CombatEntity entity) {
        if (entity == null) {
            return null;
        }

        return jCombatPanel.getEntityComponent(entity);
    }

    /**
     * Sets the event tracker from which this panel will be notified of changes
     * in the population of ExaltedCombat. The events fired by this event
     * tracker must be consistent with the
     * {@link #setPopulationModel(CombatEntities) population model}.
     *
     * @param eventTracker the event tracker from which this panel will be
     *   notified of changes. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified event tracker is
     *   {@code null}
     */
    public void setEventTracker(EventTracker eventTracker) {
        ExceptionHelper.checkNotNullArgument(eventTracker, "eventTracker");

        if (this.eventTracker != null) {
            this.eventTracker.removeAllListeners();
        }

        this.eventTracker = new LocalEventTracker(eventTracker);
        registerEventTracker();
    }

    private <ArgType> void registerListener(ExaltedEvent<ArgType> eventKind,
                TrackedEventListener<ArgType> eventListener) {
        ExaltedEvent.Helper.register(eventTracker, eventKind, eventListener);
    }

    private <ArgType> void triggerEvent(
            ExaltedEvent<ArgType> eventKind,
            ArgType eventArgument) {
        ExaltedEvent.Helper.triggerEvent(eventTracker, eventKind, eventArgument);
    }

    private void registerEventTracker() {
        registerListener(WorldEvent.ENTITY_COLOR_CHANGE, new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                CombatEntity entity = trackedEvent.getEventArg();
                EntityPanel panel = getPanelOfEntity(entity);
                if (panel != null) {
                    panel.setEntityColor(entity.getColor());
                }
            }
        });

        registerListener(WorldEvent.ENTITY_NAME_CHANGE, new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                CombatEntity entity = trackedEvent.getEventArg();
                EntityPanel panel = getPanelOfEntity(entity);
                if (panel != null) {
                    panel.setShortName(entity.getShortName());
                }
            }
        });

        registerListener(WorldEvent.ENTITY_SELECT_CHANGE, new TrackedEventListener<EntitySelectChangeArgs>() {
            @Override
            public void onEvent(TrackedEvent<EntitySelectChangeArgs> trackedEvent) {
                EntitySelectChangeArgs changeArgs = trackedEvent.getEventArg();

                setSelectionOfPanel(changeArgs.getOldSelection());
                setSelectionOfPanel(changeArgs.getNewSelection());
            }
        });

        registerListener(PositionPanelEvent.ENTITYPANEL_SELECTED, new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                if (populationModel != null) {
                    populationModel.setSelection(trackedEvent.getEventArg());
                }
            }
        });
    }

    private class EntityPanel extends JSolidPanel {
        private static final long serialVersionUID = 2552280791343940480L;

        private final CombatEntity entity;
        private final JLabel label;
        private boolean highlighted;
        private boolean selected;

        public EntityPanel(CombatEntity entity) {
            super(new GridLayout(1, 1, 0, 0));

            assert entity != null;

            this.entity = entity;
            this.highlighted = false;
            this.selected = false;

            Color color = entity.getColor();
            setBackground(color);
            setBorder(unselectedBorder);
            initListener();

            this.label = new JLabel(entity.getShortName());
            this.label.setHorizontalAlignment(SwingConstants.CENTER);
            this.label.setForeground(ExaltedDialogHelper.getVisibleTextColor(color));

            add(this.label);
        }

        public CombatEntity getEntity() {
            return entity;
        }

        public void setShortName(String shortName) {
            if (!Objects.equals(shortName, label.getText())) {
                label.setText(shortName);
            }
        }

        public void setEntityColor(Color color) {
            if (!Objects.equals(getBackground(), color)) {
                label.setForeground(ExaltedDialogHelper.getVisibleTextColor(color));
                setBackground(color);
            }
        }

        public void setSelectPanel(boolean select) {
            if (select != selected) {
                selected = select;
                setBorder();

                if (select) {
                    triggerEvent(PositionPanelEvent.ENTITYPANEL_SELECTED, getEntity());
                }
            }
        }

        public boolean isSelected() {
            return selected;
        }

        private void initListener() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    setSelectPanel(true);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    highlighted = true;
                    setBorder();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    highlighted = false;
                    setBorder();
                }
            });
        }

        private void setBorder() {
            if (isSelected()) {
                setBorder(selectedBorder);
            }
            else if (highlighted) {
                setBorder(highlightedBorder);
            }
            else {
                setBorder(unselectedBorder);
            }
        }
    }

    private static class PositionPanelEvent {
        public static final ExaltedEvent<CombatEntity> ENTITYPANEL_SELECTED
                = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCurrentTickLabel = new javax.swing.JLabel();
        jCombatHolderPanel = new javax.swing.JPanel();

        setMinimumSize(new java.awt.Dimension(50, 50));

        jCurrentTickLabel.setText("Current tick: ?");

        jCombatHolderPanel.setBackground(new java.awt.Color(153, 153, 153));
        jCombatHolderPanel.setLayout(new java.awt.GridLayout(1, 0, 1, 0));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jCurrentTickLabel)
                .addContainerGap(522, Short.MAX_VALUE))
            .addComponent(jCombatHolderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 590, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jCurrentTickLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCombatHolderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jCombatHolderPanel;
    private javax.swing.JLabel jCurrentTickLabel;
    // End of variables declaration//GEN-END:variables
}
