package exaltedcombat.panels;

import exaltedcombat.actions.CombatEntityAction;
import exaltedcombat.actions.EntityJoinCombatAction;
import exaltedcombat.components.ColoredListCell;
import exaltedcombat.components.ColoredListCellRenderer;
import exaltedcombat.components.UpdatableListModel;
import exaltedcombat.dialogs.DefineNewEntityDialog;
import exaltedcombat.dialogs.EntityStorageFrame;
import exaltedcombat.dialogs.ExaltedDialogHelper;
import exaltedcombat.events.EntitySelectChangeArgs;
import exaltedcombat.events.ExaltedEvent;
import exaltedcombat.events.WorldEvent;
import exaltedcombat.models.impl.CombatEntity;
import exaltedcombat.models.impl.CombatEntityWorldModel;
import exaltedcombat.undo.AbstractEntityActionUndoableEdit;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.text.Collator;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.jtrim.event.EventTracker;
import org.jtrim.event.LocalEventTracker;
import org.jtrim.event.TrackedEvent;
import org.jtrim.event.TrackedEventListener;
import org.jtrim.utils.ExceptionHelper;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Defines a Swing panel allowing the user to organize the population of
 * world of ExaltedCombat. This panel allows to do various things with entities:
 * <ul>
 *  <li>Define new entities and add them to the population.</li>
 *  <li>Remove entities from the population, moving them to another storage.</li>
 *  <li>Make an entity join the combat.</li>
 * </ul>
 * The entities of the population are shown in a list control.
 * <P>
 * Before actually using instances of this class, they must be initialized by
 * calling all of the following methods:
 * <ul>
 *  <li>
 *   {@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}:
 *   Sets the world model which is to be modified by this panel and from which
 *   information needs to be retrieved.
 *  </li>
 *  <li>
 *   {@link #setEventTracker(EventTracker) setEventTracker(EventTracker)}:
 *   Sets the event tracker which will notify this panel of the changes in the
 *   world of ExaltedCombat. The events fired by this event tracker must be
 *   consistent with the world model.
 *  </li>
 *  <li>
 *   {@link #setUndoManager(UndoManager) setUndoManager(UndoManager)}:
 *   Sets the undo manager to which undoable join combat actions are registered.
 *  </li>
 *  <li>
 *   {@link #setStoreFrame(EntityStorageFrame) setStoreFrame(EntityStorageFrame)}:
 *   Sets the frame which is used to display entities removed from the
 *   population.
 *  </li>
 * </ul>
 * Forgetting to call these methods and displaying this panel may cause
 * unchecked exceptions to be thrown. The methods can be called in any order.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class EntityOrganizerPanel extends JPanel {
    private static final long serialVersionUID = 4915394946569780759L;

    private static final Logger LOGGER = Logger.getLogger(EntityOrganizerPanel.class.getName());

    private static final Collator STR_CMP = StringContainer.getDefault().getStringCollator();

    private static final LocalizedString ENTER_COMBAT_EDIT_TEXT = StringContainer.getDefaultString("ENTER_COMBAT_EDIT_TEXT");
    private static final LocalizedString ENTITY_LIST_NOT_IN_COMBAT_CAPTION = StringContainer.getDefaultString("ENTITY_LIST_NOT_IN_COMBAT_CAPTION");
    private static final LocalizedString ENTITY_LIST_IN_COMBAT_CAPTION = StringContainer.getDefaultString("ENTITY_LIST_IN_COMBAT_CAPTION");
    private static final LocalizedString CONFIRM_REMOVE_FROM_COMBAT_CAPTION = StringContainer.getDefaultString("CONFIRM_REMOVE_FROM_COMBAT_CAPTION");
    private static final LocalizedString CONFIRM_REMOVE_FROM_COMBAT_TEXT = StringContainer.getDefaultString("CONFIRM_REMOVE_FROM_COMBAT_TEXT");

    private final Comparator<EntityListElement> listElementComparator;
    private EntityStorageFrame storeFrame;
    private CombatEntityWorldModel worldModel;
    private LocalEventTracker eventTracker;
    private UndoManager undoManager;

    /**
     * Creates a new panel without displaying any entities. After creating this
     * instance don't forget to initialize it by calling these methods:
     * <ul>
     *  <li>{@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}</li>
     *  <li>{@link #setEventTracker(EventTracker) setEventTracker(EventTracker)}</li>
     *  <li>{@link #setUndoManager(UndoManager) setUndoManager(UndoManager)}</li>
     *  <li>{@link #setStoreFrame(EntityStorageFrame) setStoreFrame(EntityStorageFrame)}</li>
     * </ul>
     */
    public EntityOrganizerPanel() {
        this.worldModel = null;
        this.eventTracker = null;
        this.undoManager = null;
        this.storeFrame = null;
        this.listElementComparator = new ListElementComparator();

        initComponents();
        setComponentProperties();
    }

    /**
     * Sets the frame which is used to display entities removed from the
     * population. The specified frame will only be used to be displayed by
     * this panel.
     * <P>
     * Note that it is the caller's responsibility to
     * {@link EntityStorageFrame#dispose() dispose} the specified frame. This
     * panel will not dispose the specified frame.
     *
     * @param storeFrame the frame which is used to display entities removed
     *   from the population. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified frame is
     *   {@code null}
     */
    public void setStoreFrame(EntityStorageFrame storeFrame) {
        ExceptionHelper.checkNotNullArgument(storeFrame, "storeFrame");

        this.storeFrame = storeFrame;
    }

    private void setComponentProperties() {
        jEntityList.setBackground(Color.WHITE);
        jEntityList.setCellRenderer(new ColoredListCellRenderer<>(Color.BLACK, 2));
        jEntityList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    triggerEvent(ControlEvent.LIST_SELECT_CHANGE, e);
                }
            }
        });

        onChangeSelection(null);
    }

    /**
     * Sets the undo manager to which undoable join combat actions are
     * registered.
     *
     * @param undoManager the undo manager to which undoable join combat actions
     *   are registered. This argument cannot be {@code null}.
     */
    public void setUndoManager(UndoManager undoManager) {
        ExceptionHelper.checkNotNullArgument(undoManager, "undoManager");

        this.undoManager = undoManager;
    }

    /**
     * Sets the world model to use by this panel. This panel will not register
     * event listeners with the given model and will use the model only to
     * retrieve information and modify it (e.g.: add new entities to the
     * population). Instead it will use events received from the
     * {@link #setEventTracker(EventTracker) event tracker}.
     * <P>
     * This panel will only use the specified model from the AWT event
     * dispatching thread.
     *
     * @param worldModel the world model to use by this panel. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified world model is
     *   {@code null}
     */
    public void setWorldModel(CombatEntityWorldModel worldModel) {
        ExceptionHelper.checkNotNullArgument(worldModel, "worldModel");

        this.worldModel = worldModel;
        onChangeSelection(getSelectedEntity());
    }

    /**
     * Sets the event tracker from which this panel will be notified of changes
     * in the "world" of ExaltedCombat. The events fired by this event tracker
     * must be consistent with the
     * {@link #setWorldModel(CombatEntityWorldModel) world model}.
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
        registerListener(WorldEvent.ENTITY_SELECT_CHANGE, new TrackedEventListener<EntitySelectChangeArgs>() {
            @Override
            public void onEvent(TrackedEvent<EntitySelectChangeArgs> trackedEvent) {
                onChangeSelection(trackedEvent.getEventArg().getNewSelection());
            }
        });

        registerListener(WorldEvent.ENTITY_ENTER_COMBAT, new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                updateEnableState();
                updateEntityList();
            }
        });

        registerListener(WorldEvent.ENTITIES_LEAVE_COMBAT, new TrackedEventListener<Collection<?>>() {
            @Override
            public void onEvent(TrackedEvent<Collection<?>> trackedEvent) {
                updateEnableState();
                updateEntityList();
            }
        });

        registerListener(WorldEvent.ENTITY_COLOR_CHANGE, new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                redrawEntityListLines();
            }
        });

        registerListener(WorldEvent.ENTITY_NAME_CHANGE, new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                updateEntityList();
            }
        });

        registerListener(WorldEvent.ENTITY_LIST_CHANGE, new TrackedEventListener<Void>() {
            @Override
            public void onEvent(TrackedEvent<Void> trackedEvent) {
                updateEntityList();
            }
        });

        registerListener(ControlEvent.LIST_SELECT_CHANGE, new TrackedEventListener<ListSelectionEvent>() {
            @Override
            public void onEvent(TrackedEvent<ListSelectionEvent> trackedEvent) {
                if (worldModel == null) {
                    return;
                }

                EntityListElement selected = jEntityList.getSelectedValue();
                if (selected != null) {
                    worldModel.getPopulationModel().setSelection(selected.getEntity());
                }
                else {
                    worldModel.getPopulationModel().setSelection(null);
                }
            }
        });
    }

    private CombatEntity getSelectedEntity() {
        return worldModel != null
                ? worldModel.getPopulationModel().getSelection()
                : null;
    }

    private boolean isInCombat(CombatEntity entity) {
        if (entity == null || worldModel == null) {
            return false;
        }

        return worldModel.getCombatModel().getPositionModel().getTickOfEntity(entity) >= 0;
    }

    private void updateEnableState() {
        CombatEntity selected = getSelectedEntity();
        boolean inCombat = isInCombat(selected);

        jEnterCombatTick.setEnabled(selected != null && !inCombat);
        jEnterCombatButton.setEnabled(selected != null && !inCombat);
        jJoinDescription.setEnabled(selected != null && !inCombat);
    }

    private void onChangeSelection(CombatEntity selected) {
        selectInEntityList(selected);
        updateEnableState();
    }

    private void selectInEntityList(CombatEntity entity) {
        if (entity == null) {
            jEntityList.clearSelection();
            return;
        }

        EntityListElement selected = jEntityList.getSelectedValue();
        if (selected != null && Objects.equals(entity, selected.getEntity())) {
            return;
        }

        ListModel<EntityListElement> model = jEntityList.getModel();
        int entityCount = model.getSize();
        for (int i = 0; i < entityCount; i++) {
            CombatEntity current = model.getElementAt(i).getEntity();
            if (Objects.equals(current, entity)) {
                jEntityList.setSelectedIndex(i);
                jEntityList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    private void redrawEntityListLines() {
        ListModel<EntityListElement> model = jEntityList.getModel();
        if (model instanceof UpdatableListModel) {
            ((UpdatableListModel<?>)model).updateContent();
        }
        else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Unexpected list model: {0}", model.getClass());
            }
            jEntityList.repaint();
        }
    }

    private void updateEntityList() {
        CombatEntity selected = getSelectedEntity();

        Collection<CombatEntity> currentEntities = worldModel != null
                ? worldModel.getPopulationModel().getEntities()
                : Collections.<CombatEntity>emptySet();

        List<EntityListElement> elements = new ArrayList<>(currentEntities.size());
        for (CombatEntity entity: currentEntities) {
            elements.add(new EntityListElement(entity));
        }

        Collections.sort(elements, listElementComparator);

        ListModel<EntityListElement> model = jEntityList.getModel();
        int oldSize = model.getSize();
        boolean changed = oldSize != elements.size();
        if (!changed) {
            for (int i = 0; i < oldSize; i++) {
                CombatEntity newEntity = elements.get(i).getEntity();
                CombatEntity oldEntity = model.getElementAt(i).getEntity();

                if (newEntity != oldEntity) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed || !(jEntityList.getModel() instanceof UpdatableListModel)) {
            jEntityList.getSelectionModel().setValueIsAdjusting(true);
            try {
                jEntityList.setModel(new UpdatableListModel<>(elements));
                selectInEntityList(selected);
            } finally {
                jEntityList.getSelectionModel().setValueIsAdjusting(false);
            }
        }

        redrawEntityListLines();
    }

    private class ListElementComparator implements Comparator<EntityListElement> {
        @Override
        public int compare(EntityListElement o1, EntityListElement o2) {
            CombatEntity entity1 = o1.getEntity();
            CombatEntity entity2 = o2.getEntity();

            boolean inCombat1 = isInCombat(entity1);
            boolean inCombat2 = isInCombat(entity2);

            if (inCombat1 && !inCombat2) {
                return -1;
            }

            if (!inCombat1 && inCombat2) {
                return 1;
            }

            String name1 = entity1.getShortName();
            String name2 = entity2.getShortName();

            int result = STR_CMP.compare(name1, name2);
            if (result != 0) {
                return result;
            }

            // Be as exact as possible, so entities with the same name will be
            // ordered in the same order always.
            return Integer.compare(
                    System.identityHashCode(entity1),
                    System.identityHashCode(entity2));
        }
    }

    private class EntityListElement implements ColoredListCell {
        private final CombatEntity entity;

        public EntityListElement(CombatEntity entity) {
            assert entity != null;
            this.entity = entity;
        }

        public CombatEntity getEntity() {
            return entity;
        }

        @Override
        public String toString() {
            return getCaption();
        }

        @Override
        public Color getColor() {
            return entity.getColor();
        }

        @Override
        public String getCaption() {
            return isInCombat(entity)
                    ? ENTITY_LIST_IN_COMBAT_CAPTION.format(entity.getShortName())
                    : ENTITY_LIST_NOT_IN_COMBAT_CAPTION.format(entity.getShortName());
        }
    }

    private class EnterCombatUndoableEdit extends AbstractEntityActionUndoableEdit {
        private static final long serialVersionUID = 2963488239006818211L;

        private final int roll;

        public EnterCombatUndoableEdit(
                CombatEntity entity,
                int roll,
                CombatEntityAction action) {

            super(worldModel.getPopulationModel(), entity, action);

            this.roll = roll;
        }

        @Override
        public boolean canRedo() {
            return super.canRedo()
                    && worldModel != null;
        }

        @Override
        public boolean canUndo() {
            return super.canUndo()
                    && worldModel != null;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();

            CombatEntity entity = getEntity();

            entity.removeLastAction();
            worldModel.getCombatModel().getPositionModel().removeEntity(entity);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();

            CombatEntity entity = getEntity();
            CombatEntityAction action = getAction();

            worldModel.getCombatModel().joinCombat(entity, roll);
            entity.addAction(action);
        }

        @Override
        public String getPresentationName() {
            return ENTER_COMBAT_EDIT_TEXT.format(getEntity().getShortName(), roll);
        }
    }

    private static class ControlEvent {
        public static final ExaltedEvent<ListSelectionEvent> LIST_SELECT_CHANGE
                = ExaltedEvent.Helper.createExaltedEvent(ListSelectionEvent.class);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jEntityListCaption = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jEntityList = new javax.swing.JList<EntityListElement>();
        jEnterCombatTick = new javax.swing.JSpinner();
        jEnterCombatButton = new javax.swing.JButton();
        jOtherEntitiesButton = new javax.swing.JButton();
        jDefineNewButton = new javax.swing.JButton();
        jJoinDescription = new javax.swing.JTextField();

        jEntityListCaption.setText("Entities:");

        jEntityList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jEntityList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jEntityListKeyPressed(evt);
            }
        });
        jScrollPane4.setViewportView(jEntityList);

        jEnterCombatButton.setText("Join Combat");
        jEnterCombatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jEnterCombatButtonActionPerformed(evt);
            }
        });

        jOtherEntitiesButton.setText("Other Entities");
        jOtherEntitiesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOtherEntitiesButtonActionPerformed(evt);
            }
        });

        jDefineNewButton.setText("Define New");
        jDefineNewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jDefineNewButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jEntityListCaption)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jEnterCombatTick, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jEnterCombatButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jOtherEntitiesButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                .addComponent(jDefineNewButton))
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
            .addComponent(jJoinDescription, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jEntityListCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jEnterCombatTick, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jEnterCombatButton)
                    .addComponent(jOtherEntitiesButton)
                    .addComponent(jDefineNewButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jJoinDescription, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jEnterCombatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jEnterCombatButtonActionPerformed
        CombatEntity selected = getSelectedEntity();
        if (selected == null) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Join Combat button was pressed when there was no selection.");
            }
            return;
        }

        if (isInCombat(selected)) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Join Combat button was pressed while in combat");
            }
            return;
        }

        int roll = (Integer)jEnterCombatTick.getValue();

        CombatEntityAction action = new EntityJoinCombatAction(
                worldModel.getCombatModel().getPositionModel().getCurrentTick(),
                jJoinDescription.getText(),
                roll);

        worldModel.getCombatModel().joinCombat(selected, roll);
        undoManager.addEdit(new EnterCombatUndoableEdit(selected, roll, action));
        selected.addAction(action);

        jJoinDescription.setText("");
    }//GEN-LAST:event_jEnterCombatButtonActionPerformed

    private void jOtherEntitiesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jOtherEntitiesButtonActionPerformed
        if (storeFrame != null) {
            storeFrame.setVisible(true);
        }
    }//GEN-LAST:event_jOtherEntitiesButtonActionPerformed

    private JFrame getParentWindow() {
        Component parent = getParent();
        while (parent != null && !(parent instanceof JFrame)) {
            parent = parent.getParent();
        }

        return (JFrame)parent;
    }

    private void jDefineNewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jDefineNewButtonActionPerformed
        if (worldModel == null) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "World model was not set.");
            }
            return;
        }

        JFrame parent = getParentWindow();
        if (parent == null) {
            return;
        }

        DefineNewEntityDialog dlg = new DefineNewEntityDialog(parent, true);
        dlg.setLocationRelativeTo(getParentWindow());
        dlg.setVisible(true);
        if (dlg.isAccepted() && worldModel != null) {
            worldModel.getPopulationModel().addEntity(new CombatEntity(
                    dlg.getEntityName(), dlg.getEntityColor(), ""));
        }
    }//GEN-LAST:event_jDefineNewButtonActionPerformed

    private void jEntityListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jEntityListKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_DELETE && worldModel != null) {
            EntityListElement selected = jEntityList.getSelectedValue();
            if (selected != null) {
                if (isInCombat(selected.getEntity())) {
                    if (!ExaltedDialogHelper.askYesNoQuestion(getParentWindow(),
                            CONFIRM_REMOVE_FROM_COMBAT_CAPTION.toString(),
                            CONFIRM_REMOVE_FROM_COMBAT_TEXT.format(selected.getEntity().getShortName()),
                            false)) {
                        return;
                    }

                    worldModel.getCombatModel().getPositionModel().removeEntity(selected.getEntity());
                }
                worldModel.hideEntity(selected.getEntity());
            }
        }
    }//GEN-LAST:event_jEntityListKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jDefineNewButton;
    private javax.swing.JButton jEnterCombatButton;
    private javax.swing.JSpinner jEnterCombatTick;
    private javax.swing.JList<EntityListElement> jEntityList;
    private javax.swing.JLabel jEntityListCaption;
    private javax.swing.JTextField jJoinDescription;
    private javax.swing.JButton jOtherEntitiesButton;
    private javax.swing.JScrollPane jScrollPane4;
    // End of variables declaration//GEN-END:variables
}
