package exaltedcombat.dialogs;

import exaltedcombat.save.ExaltedSaveHelper;
import exaltedcombat.save.SaveDoneListener;
import exaltedcombat.save.SaveInfo;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import javax.swing.JDialog;
import javax.swing.text.JTextComponent;
import org.jtrim.access.*;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.AutoDisplayState;
import org.jtrim.swing.concurrent.BackgroundTask;
import org.jtrim.swing.concurrent.BackgroundTaskExecutor;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

import static org.jtrim.access.AccessProperties.*;
import static org.jtrim.property.swing.AutoDisplayState.*;
import static org.jtrim.property.swing.SwingProperties.*;
import static org.jtrim.property.BoolProperties.*;

/**
 * Defines a dialog which allows the user to save a specified combat state.
 * This dialog class takes care of actually saving the combat state and once
 * this dialog has been closed {@link #isAccepted() isAccepted()} can be used
 * to check if the user has saved the combat state or not.
 * <P>
 * This dialog is not intended to be reused once the user has closed it. To
 * display a new dialog: Create a new instance of this dialog.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @see LoadCombatDialog
 * @see SaveInfo
 * @author Kelemen Attila
 */
public class SaveCombatDialog extends JDialog {
    private static final long serialVersionUID = 5809068676265384875L;

    private static final LocalizedString ERROR_WHILE_SAVING_CAPTION = StringContainer.getDefaultString("ERROR_WHILE_SAVING_CAPTION");

    private static final AccessRequest<String, HierarchicalRight> SAVE_REQUEST
            = AccessRequest.getWriteRequest(
                "FINALIZE-SAVE",
                HierarchicalRight.create("SAVE-RIGHT"));

    private final TaskExecutorService backgroundExecutor;
    private final BackgroundTaskExecutor<String, HierarchicalRight> bckgTaskExecutor;
    private final AccessManager<String, HierarchicalRight> accessManager;
    private final SaveInfo saveInfo;
    private boolean dialogClosed;
    private boolean accepted;
    private Path savePath;

    /**
     * Creates a new dialog which can be used to save the specified combat
     * state.
     *
     * @param parent the {@code Frame} from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other
     *   top-level windows when shown. If {@code true}, the modality type
     *   property is set to {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog
     *   is modeless.
     * @param saveInfo the combat state to be saved. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified combat state is
     *   {@code null}
     */
    public SaveCombatDialog(java.awt.Frame parent, boolean modal, SaveInfo saveInfo) {
        super(parent, modal);

        ExceptionHelper.checkNotNullArgument(saveInfo, "saveInfo");

        this.backgroundExecutor = new ThreadPoolTaskExecutor("SaveDlg Executor", 1);
        this.savePath = null;
        this.dialogClosed = false;
        this.accepted = false;
        this.saveInfo = saveInfo;

        initComponents();

        this.accessManager = new HierarchicalAccessManager<>(
                SwingTaskExecutor.getStrictExecutor(false));
        this.bckgTaskExecutor = new BackgroundTaskExecutor<>(accessManager, backgroundExecutor);

        AutoDisplayState.addSwingStateListener(
                and(not(textComponentEmpty(jCombatNameEdit)), trackRequestAvailable(accessManager, SAVE_REQUEST)),
                componentDisabler(jOkButton));

        getRootPane().setDefaultButton(jOkButton);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAndExit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                backgroundExecutor.shutdown();
            }
        });
    }

    private static PropertySource<Boolean> textComponentEmpty(JTextComponent component) {
        return equalsWithConst(PropertyFactory.trimmedString(documentText(component.getDocument())), "");
    }

    /**
     * Checks whether the user accepted to save the specified combat state.
     * The user can accept to save the specified combat state by clicking the
     * "Ok" button and cancel it by closing this dialog by any other means
     * (e.g.: clicking "Cancel").
     * <P>
     * If this method returns {@code true}, use the name of the save which was
     * chosen by the user can be retrieved by calling the
     * {@link #getCombatName() getCombatName()} method and the complete path to
     * the file storing the saved state can be retrieved by the
     * {@link #getSavePath() getSavePath()} method. If this method returns
     * {@code true} both of the previously mentioned methods will return a
     * non-null value.
     * <P>
     * This method will return {@code false} if the state could not be saved due
     * to some failures.
     *
     * @return {@code true} if the user accepted to save the specified combat
     *   state, {@code false} otherwise
     */
    public boolean isAccepted() {
        return accepted && savePath != null;
    }

    /**
     * Sets the default name for the state to be saved. The user can replace
     * this value with any valid name. Note that if the combat name contains
     * any character which is illegal to use in filenames saving the combat
     * state will fail.
     *
     * @param combatName the default name for the state to be saved. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified name is {@code null}
     */
    public void setCombatName(String combatName) {
        ExceptionHelper.checkNotNullArgument(combatName, "combatName");

        jCombatNameEdit.setText(combatName);
    }

    /**
     * Returns the name of the combat which was chosen by the user. This method
     * can only be used if {@link #isAccepted() isAccepted()} returns
     * {@code true}.
     *
     * @return the name of the combat which was chosen by the user. This method
     *   will never return {@code null} if {@link #isAccepted() isAccepted()}
     *   returns {@code true}.
     */
    public String getCombatName() {
        return jCombatNameEdit.getText().trim();
    }

    /**
     * Returns the name of the file used to store the specified combat state.
     * This method can only be used if {@link #isAccepted() isAccepted()}
     * returns {@code true}.
     *
     * @return  the name of the file used to store the specified combat state.
     *   This method will never return {@code null} if
     *   {@link #isAccepted() isAccepted()} returns {@code true}.
     */
    public Path getSavePath() {
        return savePath;
    }

    private void cancelAndExit() {
        final AccessResult<String> cancelTask = accessManager.getScheduledAccess(SAVE_REQUEST);
        cancelTask.releaseAndCancelBlockingTokens();

        TaskExecutor closeExecutor = cancelTask.getAccessToken().createExecutor(
                SwingTaskExecutor.getStrictExecutor(true));

        closeExecutor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                returnDialog(null);
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                cancelTask.release();
            }
        });
    }

    private void returnDialog(Path path) {
        if (!dialogClosed) {
            dialogClosed = true;
            savePath = path;
            accepted = path != null;
            dispose();
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
        jOkButton = new javax.swing.JButton();
        jCancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jCombatNameEdit = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Save Exalted Combat");

        jOkButton.setText("Save");
        jOkButton.setPreferredSize(new java.awt.Dimension(75, 23));
        jOkButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jOkButtonActionPerformed(evt);
            }
        });
        jPanel1.add(jOkButton);

        jCancelButton.setText("Cancel");
        jCancelButton.setPreferredSize(new java.awt.Dimension(75, 23));
        jCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelButtonActionPerformed(evt);
            }
        });
        jPanel1.add(jCancelButton);

        jLabel1.setText("Combat name:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addComponent(jCombatNameEdit, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCombatNameEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jOkButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jOkButtonActionPerformed
        String combatName = getCombatName();
        if (combatName.isEmpty()) {
            return;
        }

        BackgroundTask saveTask = ExaltedSaveHelper.createSaveRewTask(
                combatName,
                saveInfo,
                false,
                new SaveDoneListener() {

            @Override
            public void onSuccessfulSave(Path path) {
                returnDialog(path);
            }

            @Override
            public void onFailedSave(Throwable saveError) {
                ExaltedDialogHelper.displayError(SaveCombatDialog.this,
                        ERROR_WHILE_SAVING_CAPTION.toString(), saveError);
            }
        });
        bckgTaskExecutor.tryExecute(SAVE_REQUEST, saveTask);
    }//GEN-LAST:event_jOkButtonActionPerformed

    private void jCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCancelButtonActionPerformed
        cancelAndExit();
    }//GEN-LAST:event_jCancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jCancelButton;
    private javax.swing.JTextField jCombatNameEdit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton jOkButton;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
