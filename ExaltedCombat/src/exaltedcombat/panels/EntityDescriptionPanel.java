package exaltedcombat.panels;

import exaltedcombat.events.*;
import exaltedcombat.models.impl.CombatEntity;
import exaltedcombat.models.impl.CombatEntityWorldModel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Defines a Swing panel displaying the description of the selected entity.
 * This implementation needs a {@link CombatEntityWorldModel world model} to
 * check which entity is currently selected and an
 * {@link EventManager event manager} to detect changes made to the model.
 * Therefore every instance of this class needs to be initialized after creating
 * by calling the following methods:
 * <ul>
 *  <li>{@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}</li>
 *  <li>{@link #setEventManager(exaltedcombat.events.EventManager) setEventManager(EventManager)}</li>
 * </ul>
 * Forgetting to call these methods and displaying this panel may cause
 * unchecked exceptions to be thrown. The methods can be called in any order.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class EntityDescriptionPanel extends JPanel {
    private static final long serialVersionUID = -2891247441222840879L;

    private static final LocalizedString ENTITY_DESCRIPTION_CAPTION = StringContainer.getDefaultString("ENTITY_DESCRIPTION_CAPTION");

    private CombatEntityWorldModel worldModel;
    private LocalEventManager<ExaltedEvent> eventManager;

    /**
     * Creates a new panel without displaying any description. After creating
     * this instance don't forget to initialize it by calling these methods:
     * <ul>
     *  <li>{@link #setWorldModel(CombatEntityWorldModel) setWorldModel(CombatEntityWorldModel)}</li>
     *  <li>{@link #setEventManager(exaltedcombat.events.EventManager) setEventManager(EventManager)}</li>
     * </ul>
     */
    public EntityDescriptionPanel() {
        this.worldModel = null;
        this.eventManager = null;

        initComponents();
        setComponentProperties();
    }

    private void setComponentProperties() {
        jDescriptionCaption.setText(ENTITY_DESCRIPTION_CAPTION.toString());

        onChangeSelection(null, null);
    }

    /**
     * Sets the world model to use by this panel. This panel will not register
     * event listeners with the given model and will use the model only to
     * retrieve information. Instead it will use events received from the
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

        eventManager.registerListener(ControlEvent.DESCRIPTION_CHANGE, new GeneralEventListener<ExaltedEvent>() {
            @Override
            public void onEvent(EventCauses<ExaltedEvent> causes, Object eventArg) {
                CombatEntity selected = getSelectedEntity();
                if (selected == null) {
                    return;
                }

                selected.setDescription(jDescriptionText.getText());
            }
        });
    }

    private DocumentListener entityDescrEditListener = null;

    private void startListeningEntityPropertyEdits() {
        if (entityDescrEditListener == null) {
            entityDescrEditListener = new SimpleDocChangeListener() {
                @Override
                protected void onChange(DocumentEvent e) {
                    eventManager.triggerEvent(
                            ControlEvent.DESCRIPTION_CHANGE, e);
                }
            };
        }

        // This remove is here, so we will surely not register the listener
        // multiple times.
        jDescriptionText.getDocument().removeDocumentListener(entityDescrEditListener);
        jDescriptionText.getDocument().addDocumentListener(entityDescrEditListener);
    }

    private void stopListeningEntityPropertyEdits() {
        if (entityDescrEditListener != null) {
            jDescriptionText.getDocument().removeDocumentListener(entityDescrEditListener);
        }
    }

    private void updateDescrText(EventCauses<ExaltedEvent> causes, String text) {
        if (causes == null || !causes.isIndirectCause(ControlEvent.DESCRIPTION_CHANGE)) {
            jDescriptionText.setText(text);
        }
    }

    private void updateEnableState() {
        jDescriptionText.setEnabled(getSelectedEntity() != null);
    }

    private void onChangeSelection(EventCauses<ExaltedEvent> causes, CombatEntity selected) {
        updateDescrText(causes, selected != null ? selected.getDescription() : "");
        updateEnableState();
    }

    private CombatEntity getSelectedEntity() {
        return worldModel != null
                ? worldModel.getPopulationModel().getSelection()
                : null;
    }

    private enum ControlEvent implements ExaltedEvent {
        DESCRIPTION_CHANGE
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDescriptionCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jDescriptionText = new javax.swing.JTextArea();

        jDescriptionCaption.setText("Description:");

        jDescriptionText.setColumns(20);
        jDescriptionText.setLineWrap(true);
        jDescriptionText.setRows(5);
        jDescriptionText.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jDescriptionText);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jDescriptionCaption)
                .addContainerGap(302, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jDescriptionCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jDescriptionCaption;
    private javax.swing.JTextArea jDescriptionText;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
