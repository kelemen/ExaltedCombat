package exaltedcombat.panels;

import exaltedcombat.actions.*;
import exaltedcombat.dialogs.*;
import exaltedcombat.events.*;
import exaltedcombat.models.*;
import exaltedcombat.models.impl.*;
import exaltedcombat.undo.*;

import java.util.logging.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;

import org.jtrim.utils.ExceptionHelper;
import resources.strings.*;

/**
 * Defines a Swing panel allowing the user to manipulate entities in combat.
 * This panel allows to do two things with entities in combat:
 * <ul>
 *  <li>Let them do an action with a certain speed.</li>
 *  <li>Remove an entity from combat.</li>
 * </ul>
 * <P>
 * Before actually using instances of this class, they must be intialized by
 * calling all of the following methods:
 * <ul>
 *  <li>
 *   {@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}:
 *   Sets the world model which is to be modifed by this panel and from which
 *   information needs to be retrieved.
 *  </li>
 *  <li>
 *   {@link #setEventManager(exaltedcombat.events.EventManager) setEventManager(EventManager)}:
 *   Sets the event manager which will notify this panel of the changes in the
 *   world of ExaltedCombat. The events fired by this event manager must be
 *   consistent with the world model.
 *  </li>
 *  <li>
 *   {@link #setUndoManager(UndoManager) setUndoManager(UndoManager)}:
 *   Sets the undo manager to which undoable actions of entities are registered.
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
public class InCombatActionPanel extends JPanel {
    private static final long serialVersionUID = 7224079342207812005L;

    private static final Logger LOGGER = Logger.getLogger(InCombatActionPanel.class.getName());

    private static final LocalizedString MOVE_BUTTON_CAPTION = StringContainer.getDefaultString("MOVE_BUTTON_CAPTION");
    private static final LocalizedString LEAVE_COMBAT_CAPTION = StringContainer.getDefaultString("LEAVE_COMBAT_CAPTION");
    private static final LocalizedString CONFIRM_START_COMBAT_CAPTION = StringContainer.getDefaultString("CONFIRM_START_COMBAT_CAPTION");
    private static final LocalizedString CONFIRM_START_COMBAT_TEXT = StringContainer.getDefaultString("CONFIRM_START_COMBAT_TEXT");
    private static final LocalizedString START_COMBAT_EDIT_TEXT = StringContainer.getDefaultString("START_COMBAT_EDIT_TEXT");
    private static final LocalizedString MOVE_ENTITY_EDIT_TEXT = StringContainer.getDefaultString("MOVE_ENTITY_EDIT_TEXT");
    private static final LocalizedString LEAVE_COMBAT_EDIT_TEXT = StringContainer.getDefaultString("LEAVE_COMBAT_EDIT_TEXT");

    private CombatEntityWorldModel worldModel;
    private LocalEventManager<ExaltedEvent> eventManager;
    private UndoManager undoManager;

    /**
     * Creates a new panel which will be only effective after initialzied. After
     * creating this instance don't forget to initialize it by calling these
     * methods:
     * <ul>
     *  <li>{@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}</li>
     *  <li>{@link #setEventManager(exaltedcombat.events.EventManager) setEventManager(EventManager)}</li>
     *  <li>{@link #setUndoManager(UndoManager) setUndoManager(UndoManager)}</li>
     * </ul>
     */
    public InCombatActionPanel() {
        this.worldModel = null;
        this.eventManager = null;
        this.undoManager = null;

        initComponents();
        setComponentProperties();
    }

    private void setComponentProperties() {
        jMoveToTickButton.setText(MOVE_BUTTON_CAPTION.toString());
        jRemoveButton.setText(LEAVE_COMBAT_CAPTION.toString());

        jTickEdit.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateEnableState();
            }
        });

        onChangeSelection(null, null);
        startListeningEntityPropertyEdits();
    }

    /**
     * Sets the undo manager to which undoable actions of entities are
     * registered.
     *
     * @param undoManager the undo manager to which undoable actions of entities
     *   are registered. This argument cannot be {@code null}.
     */
    public void setUndoManager(UndoManager undoManager) {
        ExceptionHelper.checkNotNullArgument(undoManager, "undoManager");

        this.undoManager = undoManager;
    }

    /**
     * Sets the world model to use by this panel. This panel will not register
     * event listeners with the given model and will use the model only to
     * retrieve information and modify it (e.g.: remove an entity from combat).
     * Instead it will use events received from the
     * {@link #setEventManager(exaltedcombat.events.EventManager) event manager}.
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
        onChangeSelection(null, getSelectedEntity());
    }

    /**
     * Sets the event manager from which this panel will be notified of changes
     * in the "world" of ExaltedCombat. The events fired by this event manager
     * must be consistent with the
     * {@link #setWorldModel(CombatEntityWorldModel) world model}.
     *
     * @param eventManager the event manager from which this panel will be
     *   notified of changes. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified event manager is
     *   {@code null}
     */
    public void setEventManager(EventManager<ExaltedEvent> eventManager) {
        ExceptionHelper.checkNotNullArgument(eventManager, "eventManager");

        if (this.eventManager != null) {
            this.eventManager.removeAllListeners();
        }

        this.eventManager = new LocalEventManager<>(eventManager);
        registerEventManager();
    }

    private void registerEventManager() {
        eventManager.registerListener(WorldEvent.ENTITY_SELECT_CHANGE, new GeneralEventListener<ExaltedEvent>() {
            @Override
            public void onEvent(EventCauses<ExaltedEvent> causes, Object eventArg) {
                EntitySelectChangeArgs changeArgs = (EntitySelectChangeArgs)eventArg;

                stopListeningEntityPropertyEdits();
                try {
                    onChangeSelection(causes, changeArgs.getNewSelection());
                } finally {
                    startListeningEntityPropertyEdits();
                }
            }
        });

        eventManager.registerListener(WorldEvent.ENTITY_ENTER_COMBAT, new GeneralEventListener<ExaltedEvent>() {
            @Override
            public void onEvent(EventCauses<ExaltedEvent> causes, Object eventArg) {
                updateEnableState();
            }
        });

        eventManager.registerListener(WorldEvent.ENTITIES_LEAVE_COMBAT, new GeneralEventListener<ExaltedEvent>() {
            @Override
            public void onEvent(EventCauses<ExaltedEvent> causes, Object eventArg) {
                updateEnableState();
            }
        });

        eventManager.registerListener(ControlEvent.NAME_CHANGE, new GeneralEventListener<ExaltedEvent>() {
            @Override
            public void onEvent(EventCauses<ExaltedEvent> causes, Object eventArg) {
                CombatEntity selected = getSelectedEntity();
                if (selected == null) {
                    return;
                }

                selected.setShortName(jEntityNameEdit.getText());
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

        return getTickOfEntity(entity) >= 0;
    }

    private void updateEnableState() {
        CombatEntity selected = getSelectedEntity();
        boolean inCombat = isInCombat(selected);
        int moveToTickPos = 0;
        if (selected != null) {
            moveToTickPos = getTickOfEntity(selected) + getTickEditValue();
        }

        jEntityNameEdit.setEnabled(selected != null);
        jTickEdit.setEnabled(inCombat);
        jMoveToTickButton.setEnabled(inCombat && moveToTickPos >= 0);
        jRemoveButton.setEnabled(inCombat && isCombatStarted());
        jCurrentActionText.setEnabled(inCombat);
    }

    private void updateNameText(EventCauses<ExaltedEvent> causes, String text) {
        if (causes == null || !causes.isIndirectCause(ControlEvent.NAME_CHANGE)) {
            jEntityNameEdit.setText(text);
        }
    }

    private void onChangeSelection(EventCauses<ExaltedEvent> causes, CombatEntity selected) {
        updateNameText(causes, selected != null ? selected.getShortName() : "");
        jCurrentActionText.setText("");

        int defaultSpeed = 0;
        if (selected != null) {
            CombatEntityAction lastAction = selected.getLastAction();
            if (lastAction instanceof EntityMoveAction) {
                defaultSpeed = ((EntityMoveAction)lastAction).getSpeed();
            }
        }
        jTickEdit.setValue(defaultSpeed);

        updateEnableState();
    }

    private DocumentListener entityNameEditListener = null;

    private void startListeningEntityPropertyEdits() {
        if (entityNameEditListener == null) {
            entityNameEditListener = new SimpleDocChangeListener() {
                @Override
                protected void onChange(DocumentEvent e) {
                    eventManager.triggerEvent(
                            ControlEvent.NAME_CHANGE, e);
                }
            };
        }

        jEntityNameEdit.getDocument().removeDocumentListener(entityNameEditListener);
        jEntityNameEdit.getDocument().addDocumentListener(entityNameEditListener);
    }

    private void stopListeningEntityPropertyEdits() {
        if (entityNameEditListener != null) {
            jEntityNameEdit.getDocument().removeDocumentListener(entityNameEditListener);
        }
    }

    private boolean isCombatStarted() {
        if (worldModel == null) {
            return false;
        }

        return worldModel.getCombatModel().getCombatState() != CombatState.JOIN_PHASE;
    }

    private class StartCombatUndoableEdit extends AbstractExaltedUndoableEdit {
        private static final long serialVersionUID = 5615202267504585113L;

        @Override
        public boolean canRedo() {
            return super.canRedo()
                    && worldModel != null
                    && !isCombatStarted();
        }

        @Override
        public boolean canUndo() {
            return super.canUndo()
                    && worldModel != null
                    && isCombatStarted();
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();

            worldModel.getCombatModel().revertToJoinPhase();
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();

            worldModel.getCombatModel().endJoinPhase();
        }

        @Override
        public String getPresentationName() {
            return START_COMBAT_EDIT_TEXT.toString();
        }
    }

    private class MoveEntityUndoableEdit extends AbstractEntityActionUndoableEdit {
        private static final long serialVersionUID = 7972626139460568301L;

        final int fromTick;
        final int toTick;

        public MoveEntityUndoableEdit(int fromTick, int toTick, CombatEntity entity, CombatEntityAction action) {
            super(worldModel.getPopulationModel(), entity, action);
            assert fromTick >= 0 && toTick >= 0;

            this.fromTick = fromTick;
            this.toTick = toTick;
        }

        @Override
        public boolean canRedo() {
            return super.canRedo()
                    && worldModel != null
                    && isCombatStarted()
                    && getTickOfEntity(getEntity()) == fromTick;
        }

        @Override
        public boolean canUndo() {
            return super.canUndo()
                    && worldModel != null
                    && isCombatStarted()
                    && getTickOfEntity(getEntity()) == toTick;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();

            CombatEntity entity = getEntity();

            entity.removeLastAction();
            worldModel.getCombatModel().getPositionModel().moveToTick(entity, fromTick);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();

            CombatEntity entity = getEntity();
            CombatEntityAction action = getAction();

            worldModel.getCombatModel().getPositionModel().moveToTick(entity, toTick);
            entity.addAction(action);
        }

        @Override
        public String getPresentationName() {
            return MOVE_ENTITY_EDIT_TEXT.format(getEntity().getShortName(),
                    fromTick, toTick, toTick - fromTick);
        }
    }

    private class LeaveCombatUndoableEdit extends AbstractEntityActionUndoableEdit {
        private static final long serialVersionUID = 7819958154176076546L;

        final int fromTick;

        public LeaveCombatUndoableEdit(int fromTick, CombatEntity entity, CombatEntityAction action) {
            super(worldModel.getPopulationModel(), entity, action);
            assert fromTick >= 0;
            this.fromTick = fromTick;
        }

        @Override
        public boolean canRedo() {
            return super.canRedo()
                    && isInCombat(getEntity())
                    && getTickOfEntity(getEntity()) == fromTick;
        }

        @Override
        public boolean canUndo() {
            return super.canUndo()
                    && !isInCombat(getEntity());
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();

            worldModel.getCombatModel().getPositionModel().removeEntity(getEntity());
            getEntity().addAction(getAction());
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();

            getEntity().removeLastAction();
            worldModel.getCombatModel().getPositionModel().moveToTick(getEntity(), fromTick);
        }

        @Override
        public String getPresentationName() {
            return LEAVE_COMBAT_EDIT_TEXT.format(getEntity().getShortName(),
                    fromTick);
        }
    }

    private enum ControlEvent implements ExaltedEvent {
        NAME_CHANGE
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jCurrentActionText = new javax.swing.JTextArea();
        jEntityNameEdit = new javax.swing.JTextField();
        jTickEdit = new javax.swing.JSpinner();
        jMoveToTickButton = new javax.swing.JButton();
        jRemoveButton = new javax.swing.JButton();

        jCurrentActionText.setColumns(20);
        jCurrentActionText.setLineWrap(true);
        jCurrentActionText.setRows(5);
        jCurrentActionText.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jCurrentActionText);

        jEntityNameEdit.setMinimumSize(new java.awt.Dimension(50, 20));

        jMoveToTickButton.setText("Move");
        jMoveToTickButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMoveToTickButtonActionPerformed(evt);
            }
        });

        jRemoveButton.setText("Leave Combat");
        jRemoveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRemoveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jEntityNameEdit, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTickEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jMoveToTickButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jRemoveButton))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jEntityNameEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTickEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jMoveToTickButton)
                    .addComponent(jRemoveButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private int getTickEditValue() {
        return (Integer)jTickEdit.getValue();
    }

    private int getTickOfEntity(CombatEntity entity) {
        return worldModel.getCombatModel().getPositionModel().getTickOfEntity(entity);
    }

    private void jMoveToTickButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMoveToTickButtonActionPerformed
        CombatEntity selectedEntity = getSelectedEntity();
        if (!isInCombat(selectedEntity)) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Trying to move an entity not in combat.");
            }
            return;
        }

        int dTick = getTickEditValue();
        int prevTick = getTickOfEntity(selectedEntity);

        UndoableEdit startCombatEdit = null;
        UndoableEdit moveEntityEdit = null;
        if (worldModel.getCombatModel().getCombatState() == CombatState.JOIN_PHASE) {
            if (!ExaltedDialogHelper.askYesNoQuestion(this,
                    CONFIRM_START_COMBAT_CAPTION.toString(),
                    CONFIRM_START_COMBAT_TEXT.toString(),
                    false)) {
                return;
            }

            worldModel.getCombatModel().endJoinPhase();

            startCombatEdit = new StartCombatUndoableEdit();
        }

        int tick = prevTick + dTick;
        if (tick >= 0) {
            int currentTick = worldModel.getCombatModel().getPositionModel().getCurrentTick();
            String actionText = jCurrentActionText.getText();

            CombatEntityAction action = new EntityMoveAction(
                    currentTick,
                    actionText,
                    prevTick,
                    dTick);

            worldModel.getCombatModel().getPositionModel().moveToTick(selectedEntity, tick);
            selectedEntity.addAction(action);

            moveEntityEdit = new MoveEntityUndoableEdit(
                    prevTick, tick, selectedEntity, action);
        }
        else {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Trying to move an entity before the first tick.");
            }
        }

        undoManager.addEdit(new SafeCompoundEdit(
                startCombatEdit, moveEntityEdit));

        worldModel.getPopulationModel().setSelection(null);
        jTickEdit.setValue(0);
    }//GEN-LAST:event_jMoveToTickButtonActionPerformed

    private void jRemoveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRemoveButtonActionPerformed
        CombatEntity selectedEntity = getSelectedEntity();
        if (selectedEntity == null) {
            return;
        }

        int prevTick = getTickOfEntity(selectedEntity);
        if (prevTick < 0 || !isCombatStarted()) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Trying to remove an entity not in combat (or still in join phase).");
            }
            return;
        }

        int currentTick = worldModel.getCombatModel().getPositionModel().getCurrentTick();
        String actionText = jCurrentActionText.getText();
        worldModel.getCombatModel().getPositionModel().removeEntity(selectedEntity);

        CombatEntityAction action = new EntityLeaveCombatAction(
                currentTick, actionText);
        selectedEntity.addAction(action);
        undoManager.addEdit(new LeaveCombatUndoableEdit(
                prevTick, selectedEntity, action));

        worldModel.getPopulationModel().setSelection(null);
    }//GEN-LAST:event_jRemoveButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea jCurrentActionText;
    private javax.swing.JTextField jEntityNameEdit;
    private javax.swing.JButton jMoveToTickButton;
    private javax.swing.JButton jRemoveButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSpinner jTickEdit;
    // End of variables declaration//GEN-END:variables
}
