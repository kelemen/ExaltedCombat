package exaltedcombat.panels;

import exaltedcombat.actions.CombatEntityAction;
import exaltedcombat.components.ImmutableListModel;
import exaltedcombat.events.EntitySelectChangeArgs;
import exaltedcombat.events.ExaltedEvent;
import exaltedcombat.events.WorldEvent;
import exaltedcombat.models.impl.CombatEntity;
import exaltedcombat.models.impl.CombatEntityWorldModel;
import exaltedcombat.utils.ExaltedConsts;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jtrim.event.EventTracker;
import org.jtrim.event.LocalEventTracker;
import org.jtrim.event.TrackedEvent;
import org.jtrim.event.TrackedEventListener;
import org.jtrim.utils.ExceptionHelper;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Defines a Swing panel displaying the action history of the selected entity.
 * This implementation needs a {@link CombatEntityWorldModel world model} to
 * check which entity is currently selected and an
 * {@link EventTracker event tracker} to detect changes made to the model.
 * Therefore every instance of this class needs to be initialized after creating
 * by calling the following methods:
 * <ul>
 *  <li>{@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}</li>
 *  <li>{@link #setEventTracker(EventTracker) EventTracker(EventTracker)}</li>
 * </ul>
 * Forgetting to call these methods and displaying this panel may cause
 * unchecked exceptions to be thrown. The methods can be called in any order.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class ActionDisplayPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 2711472300696931407L;

    private static final LocalizedString ENTITY_ACTION_LIST_ELEMENT_CAPTION = StringContainer.getDefaultString("ENTITY_ACTION_LIST_ELEMENT_CAPTION");
    private static final LocalizedString PREV_ACTION_LIST_CAPTION = StringContainer.getDefaultString("PREV_ACTION_LIST_CAPTION");
    private static final LocalizedString SELECTED_ACTION_CAPTION = StringContainer.getDefaultString("SELECTED_ACTION_CAPTION");

    private LocalEventTracker eventTracker;
    private CombatEntity selectedEntity;

    /**
     * Creates a new panel without displaying any actions. After creating this
     * instance don't forget to initialize it by calling these methods:
     * <ul>
     *  <li>{@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}</li>
     *  <li>{@link #setEventTracker(EventTracker) setEventTracker(EventTracker)}</li>
     * </ul>
     */
    public ActionDisplayPanel() {
        this.eventTracker = null;
        this.selectedEntity = null;

        initComponents();
        setComponentProperties();
    }

    /**
     * Sets the world model to use by this panel. This panel will not register
     * event listeners with the given model and will use the model only to
     * retrieve information. Instead it will use events received from the
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

        selectedEntity = worldModel.getPopulationModel().getSelection();
        updateList(selectedEntity);
    }

    private void setComponentProperties() {
        updateList(null);
        jPrevActionsCaption.setText(PREV_ACTION_LIST_CAPTION.toString());
        jSelectedActionCaption.setText(SELECTED_ACTION_CAPTION.toString());

        jPreviousActionsList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                EntityActionElement selected = jPreviousActionsList.getSelectedValue();
                String text;
                if (selected != null) {
                    text = selected.getAction().getPresentationText();
                }
                else {
                    text = "";
                }

                jSelectedActionDescription.setText(text);
            }
        });
    }

    private void updateList(CombatEntity selected) {
        selectedEntity = selected;
        if (selected == null) {
            jPreviousActionsList.setModel(new ImmutableListModel<EntityActionElement>());
            jSelectedActionDescription.setText("");
        }
        else {
            List<CombatEntityAction> prevActions = selected.getPreviousActions();
            List<EntityActionElement> listElements = new ArrayList<>(prevActions.size());
            for (CombatEntityAction action: prevActions) {
                listElements.add(new EntityActionElement(action));
            }

            jPreviousActionsList.getSelectionModel().setValueIsAdjusting(true);
            try {
                jPreviousActionsList.setModel(new ImmutableListModel<>(listElements));
                if (!listElements.isEmpty()) {
                    int selectedIndex = listElements.size() - 1;
                    jPreviousActionsList.setSelectedIndex(selectedIndex);
                    jPreviousActionsList.ensureIndexIsVisible(selectedIndex);
                }
            } finally {
                jPreviousActionsList.getSelectionModel().setValueIsAdjusting(false);
            }
        }
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

    private void registerEventTracker() {
        registerListener(WorldEvent.ENTITY_SELECT_CHANGE, new TrackedEventListener<EntitySelectChangeArgs>() {
            @Override
            public void onEvent(TrackedEvent<EntitySelectChangeArgs> trackedEvent) {
                updateList(trackedEvent.getEventArg().getNewSelection());
            }
        });

        registerListener(WorldEvent.ENTITY_PREV_ACTION_CHANGE, new TrackedEventListener<CombatEntity>() {

            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                if (trackedEvent.getEventArg() == selectedEntity) {
                    updateList(selectedEntity);
                }
            }
        });
    }

    private static class EntityActionElement {
        private final CombatEntityAction action;
        private final String listCaption;

        public EntityActionElement(CombatEntityAction action) {
            assert action != null;

            this.action = action;

            this.listCaption = ENTITY_ACTION_LIST_ELEMENT_CAPTION.format(
                    action.getActionTick() + ExaltedConsts.TICK_OFFSET,
                    action.getPresentationCaption());
        }

        public CombatEntityAction getAction() {
            return action;
        }

        @Override
        public String toString() {
            return listCaption;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPrevActionsCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPreviousActionsList = new javax.swing.JList<EntityActionElement>();
        jPanel2 = new javax.swing.JPanel();
        jSelectedActionCaption = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jSelectedActionDescription = new javax.swing.JTextArea();

        setLayout(new java.awt.GridLayout(1, 2, 5, 0));

        jPrevActionsCaption.setText("Previous actions:");

        jPreviousActionsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jPreviousActionsList);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPrevActionsCaption)
                .addContainerGap(107, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPrevActionsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE))
        );

        add(jPanel1);

        jSelectedActionCaption.setText("Selected action:");

        jSelectedActionDescription.setColumns(20);
        jSelectedActionDescription.setEditable(false);
        jSelectedActionDescription.setLineWrap(true);
        jSelectedActionDescription.setRows(5);
        jSelectedActionDescription.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jSelectedActionDescription);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jSelectedActionCaption)
                .addContainerGap(112, Short.MAX_VALUE))
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jSelectedActionCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE))
        );

        add(jPanel2);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel jPrevActionsCaption;
    private javax.swing.JList<EntityActionElement> jPreviousActionsList;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel jSelectedActionCaption;
    private javax.swing.JTextArea jSelectedActionDescription;
    // End of variables declaration//GEN-END:variables
}
