package exaltedcombat.dialogs;

import exaltedcombat.components.*;
import exaltedcombat.events.*;
import exaltedcombat.models.impl.*;

import java.awt.Color;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jtrim.concurrent.*;
import org.jtrim.swing.concurrent.*;
import org.jtrim.utils.*;

import resources.icons.*;
import resources.strings.*;

/**
 * Defines a frame managing combat entities not currently in combat. This frame
 * is intended to be used to store and manage entities not actively used so they
 * don't clutter other dialogs.
 * <P>
 * This frame allows the stored combat entities to be moved to another storage
 * which is usually the main frame where active entities are organized.
 * <P>
 * This frame can be redisplayed after it has been {@link #dispose() closed}
 * and it will still display the entities this frame currently stores.
 * <P>
 * Notice that this frame implements the {@link EntityStorage} interface
 * and methods of this can be used even if this frame is not visible or
 * displayable. Note however that methods of this interface can still only be
 * called from the AWT event dispatching thread.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class EntityStorageFrame extends JFrame implements EntityStorage {
    private static final long serialVersionUID = -3547031532510426759L;

    private static final Logger LOGGER = Logger.getLogger(EntityStorageFrame.class.getName());

    private static final LocalizedString FRAME_TITLE = StringContainer.getDefaultString("OTHER_ENTITIES_FRAME_TITLE");
    private static final LocalizedString CONFIRM_REMOVE_SINGLE_ENTITY_TEXT = StringContainer.getDefaultString("CONFIRM_REMOVE_SINGLE_ENTITY_TEXT");
    private static final LocalizedString CONFIRM_REMOVE_ENTITIES_TEXT = StringContainer.getDefaultString("CONFIRM_REMOVE_ENTITIES_TEXT");
    private static final LocalizedString CONFIRM_REMOVE_ENTITY_CAPTION = StringContainer.getDefaultString("CONFIRM_REMOVE_ENTITY_CAPTION");
    private static final LocalizedString ENTITY_LIST_CAPTION = StringContainer.getDefaultString("ENTITY_LIST_CAPTION");
    private static final LocalizedString ENTITY_DESCRIPTION_CAPTION = StringContainer.getDefaultString("ENTITY_DESCRIPTION_CAPTION");
    private static final LocalizedString HIDE_OTHER_ENTITIES_BUTTON_CAPTION = StringContainer.getDefaultString("HIDE_OTHER_ENTITIES_BUTTON_CAPTION");

    private static final LocalizedString BUTTON_CAPTION_YES = StringContainer.getDefaultString("BUTTON_CAPTION_YES");
    private static final LocalizedString BUTTON_CAPTION_NO = StringContainer.getDefaultString("BUTTON_CAPTION_NO");

    private static Collator STR_CMP = StringContainer.getDefault().getStringCollator();

    private final Set<CombatEntity> storedEntities;
    private final EntityStorage otherStore;

    private final UpdateTaskExecutor entityListUpdateExecutor;

    /**
     * Creates a new frame with a main storage to where entities can be moved
     * from this frame.
     *
     * @param otherStore the storage to where combat entities can be moved
     *   by the user. This argument cannot be {@code null}
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public EntityStorageFrame(EntityStorage otherStore) {
        ExceptionHelper.checkNotNullArgument(otherStore, "otherStore");

        initComponents();

        setIconImage(IconStorage.getSecondaryIcon());
        this.otherStore = otherStore;
        this.entityListUpdateExecutor = new SwingUpdateTaskExecutor(true);
        this.storedEntities = new HashSet<>(1000);

        jEntityList.setCellRenderer(new ColoredListCellRenderer<>(Color.BLACK, 2));
        jEntityList.setBackground(Color.WHITE); // must be explicitly set, the default does not work

        jDescritionText.getDocument().addDocumentListener(new SimpleDocChangeListener() {
            @Override
            protected void onChange(DocumentEvent e) {
                onChangeDescriptionText();
            }
        });

        jEntityList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                onChangeSelection();
            }
        });

        onChangeSelection();
        setComponentCaptions();
    }

    private void setComponentCaptions() {
        setTitle(FRAME_TITLE.toString());
        jEntityListCaption.setText(ENTITY_LIST_CAPTION.toString());
        jDescriptionCaption.setText(ENTITY_DESCRIPTION_CAPTION.toString());
        jHideButtonCaption.setText(HIDE_OTHER_ENTITIES_BUTTON_CAPTION.toString());
    }

    private void onChangeSelection() {
        StoredEntityElement selected = jEntityList.getSelectedValue();
        String selectedDescr = selected != null
                ? selected.getEntity().getDescription()
                : "";

        if (!Objects.equals(jDescritionText.getText(), selectedDescr)) {
            jDescritionText.setText(selectedDescr);
        }

        jDescritionText.setEnabled(selected != null);
    }

    private void onChangeDescriptionText() {
        StoredEntityElement selectedElement = jEntityList.getSelectedValue();
        if (selectedElement == null) {
            return;
        }

        String text = jDescritionText.getText();
        CombatEntity selected = selectedElement.getEntity();
        if (!Objects.equals(selected.getDescription(), text)) {
            selected.setDescription(text);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Collection<CombatEntity> getStoredEntities() {
        List<CombatEntity> result = new ArrayList<>(storedEntities.size());
        for (CombatEntity entity: storedEntities) {
            result.add(new CombatEntity(entity));
        }
        return result;
    }

    /**
     * {@inheritDoc }
     * <P>
     * The specified entity will immediately displayed if this frame is
     * visible.
     */
    @Override
    public void storeEntity(CombatEntity entity) {
        storeEntities(Collections.singleton(entity));
    }

    /**
     * {@inheritDoc }
     * <P>
     * The specified entities will immediately displayed if this frame is
     * visible.
     */
    @Override
    public void storeEntities(Collection<? extends CombatEntity> entities) {
        ExceptionHelper.checkNotNullElements(entities, "entities");

        for (CombatEntity entity: entities) {
            storedEntities.add(new CombatEntity(entity));
        }

        if (!entities.isEmpty()) {
            updateEntityList();
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * The modifications will immediately visible if this frame is visible.
     */
    @Override
    public void clearEntities() {
        if (!storedEntities.isEmpty()) {
            storedEntities.clear();
            updateEntityList();
        }
    }

    private void updateEntityList() {
        entityListUpdateExecutor.execute(new Runnable(){
            @Override
            public void run() {
                List<StoredEntityElement> elements = new ArrayList<>(storedEntities.size());
                for (CombatEntity entity: storedEntities) {
                    elements.add(new StoredEntityElement(entity));
                }

                Collections.sort(elements, EntityElementComparator.INSTANCE);

                StoredEntityElement selectedElement = jEntityList.getSelectedValue();
                CombatEntity selected = selectedElement != null
                        ? selectedElement.getEntity()
                        : null;

                jEntityList.setModel(new ImmutableListModel<>(elements));

                if (selected != null) {
                    int index = 0;
                    for (StoredEntityElement entity: elements) {
                        if (entity.getEntity() == selected) {
                            jEntityList.setSelectedIndex(index);
                            break;
                        }
                        index++;
                    }
                }
            }
        });
    }

    private boolean askYesNoQuestion(String caption, String question, boolean defaultAnswer) {
        String yesOption = BUTTON_CAPTION_YES.toString();
        String noOption = BUTTON_CAPTION_NO.toString();
        return JOptionPane.showOptionDialog(this,
                question, caption,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{yesOption, noOption},
                defaultAnswer ? yesOption : noOption) == 0;
    }

    private void removeEntity(CombatEntity entity) {
        storedEntities.remove(entity);
        updateEntityList();
    }

    private void removeSelected() {
        List<StoredEntityElement> selectedElements = jEntityList.getSelectedValuesList();
        if (selectedElements.isEmpty()) {
            return;
        }

        String question;
        if (selectedElements.size() == 1) {
            question = CONFIRM_REMOVE_SINGLE_ENTITY_TEXT.format(
                    selectedElements.get(0).toString());
        }
        else {
            question = CONFIRM_REMOVE_ENTITIES_TEXT.toString();
        }

        if (!askYesNoQuestion(CONFIRM_REMOVE_ENTITY_CAPTION.toString(), question, false)) {
            return;
        }

        for (StoredEntityElement element: selectedElements) {
            removeEntity(element.getEntity());
        }
    }

    private void storeSelected() {
        if (storedEntities == null) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Entity store was not set.");
            }
            return;
        }

        List<StoredEntityElement> selectedElements = jEntityList.getSelectedValuesList();
        if (selectedElements.isEmpty()) {
            return;
        }

        List<CombatEntity> toStore = new ArrayList<>(selectedElements.size());
        for (StoredEntityElement element: selectedElements) {
            toStore.add(element.getEntity());
        }

        otherStore.storeEntities(toStore);
        for (CombatEntity entity: toStore) {
            removeEntity(entity);
        }
    }

    private enum EntityElementComparator
    implements
            Comparator<StoredEntityElement> {
        INSTANCE;

        @Override
        public int compare(StoredEntityElement o1, StoredEntityElement o2) {
            String name1 = o1.getEntity().getShortName();
            String name2 = o2.getEntity().getShortName();

            return STR_CMP.compare(name1, name2);
        }
    }

    private static class StoredEntityElement implements ColoredListCell {
        private final CombatEntity entity;

        public StoredEntityElement(CombatEntity entity) {
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
            return entity.getShortName();
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
        jHideButtonCaption = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jEntityListCaption = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jEntityList = new javax.swing.JList<StoredEntityElement>();
        jPanel3 = new javax.swing.JPanel();
        jDescriptionCaption = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jDescritionText = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);

        jHideButtonCaption.setText("Hide");
        jHideButtonCaption.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jHideButtonCaptionActionPerformed(evt);
            }
        });
        jPanel1.add(jHideButtonCaption);

        jSplitPane1.setDividerLocation(200);

        jEntityListCaption.setText("Entities:");

        jEntityList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jEntityListKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(jEntityList);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jEntityListCaption)
                .addContainerGap(160, Short.MAX_VALUE))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jEntityListCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE))
        );

        jSplitPane1.setLeftComponent(jPanel2);

        jDescriptionCaption.setText("Description:");

        jDescritionText.setColumns(20);
        jDescritionText.setLineWrap(true);
        jDescritionText.setRows(5);
        jDescritionText.setWrapStyleWord(true);
        jScrollPane2.setViewportView(jDescritionText);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jDescriptionCaption)
                .addContainerGap(279, Short.MAX_VALUE))
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jDescriptionCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 271, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel3);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 293, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jHideButtonCaptionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jHideButtonCaptionActionPerformed
        dispose();
    }//GEN-LAST:event_jHideButtonCaptionActionPerformed

    private void jEntityListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jEntityListKeyPressed
        switch (evt.getKeyCode()) {
            case KeyEvent.VK_DELETE:
                removeSelected();
                break;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                storeSelected();
                break;
        }
    }//GEN-LAST:event_jEntityListKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jDescriptionCaption;
    private javax.swing.JTextArea jDescritionText;
    private javax.swing.JList<StoredEntityElement> jEntityList;
    private javax.swing.JLabel jEntityListCaption;
    private javax.swing.JButton jHideButtonCaption;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    // End of variables declaration//GEN-END:variables
}
