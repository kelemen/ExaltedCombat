package exaltedcombat.dialogs;

import exaltedcombat.events.*;
import exaltedcombat.save.*;

import java.awt.event.*;
import java.nio.file.*;
import java.util.concurrent.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jtrim.access.*;
import org.jtrim.access.task.*;
import org.jtrim.concurrent.*;
import org.jtrim.swing.access.*;
import org.jtrim.utils.*;

import resources.strings.*;

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

    private final AccessRequest<String, SwingRight> saveRequest;
    private final ExecutorService backgroundExecutor;
    private final RewTaskExecutor rewExecutor;
    private final AccessManager<String, SwingRight> accessManager;
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

        this.backgroundExecutor = ExecutorsEx.newMultiThreadedExecutor(1, false, "SaveDlg Executor");
        this.rewExecutor = new GenericRewTaskExecutor(backgroundExecutor);
        this.accessManager = new SwingAccessManager<>(AutoComponentDisabler.INSTANCE);
        this.savePath = null;
        this.dialogClosed = false;
        this.accepted = false;
        this.saveInfo = saveInfo;

        initComponents();

        this.saveRequest = AccessRequest.getWriteRequest(
                "FINALIZE-SAVE",
                new SwingRight(jOkButton));

        getRootPane().setDefaultButton(jOkButton);

        jCombatNameEdit.getDocument().addDocumentListener(new SimpleDocChangeListener() {
            @Override
            protected void onChange(DocumentEvent e) {
                checkEnableState();
            }
        });

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

    private void checkEnableState() {
        jOkButton.setEnabled(!getCombatName().isEmpty());
    }

    /**
     * Checks whether the user accepted to save the specified combat state.
     * The user can accept to save the specified combat state by clicking the
     * "Ok" button and cancel it by closing this dialog by any other means
     * (e.g.: clicking "Cancel").
     * <P>
     * If this method returns {@code true}, use the name of the save which was
     * choosen by the user can be retrieved by calling the
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
        checkEnableState();
    }

    /**
     * Returns the name of the combat which was choosen by the user. This method
     * can only be used if {@link #isAccepted() isAccepted()} returns
     * {@code true}.
     *
     * @return the name of the combat which was choosen by the user. This method
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
        AccessResult<String> cancelTask = accessManager.getScheduledAccess(saveRequest);
        cancelTask.shutdownBlockingTokensNow();

        cancelTask.getAccessToken().executeAndShutdown(new Runnable() {
            @Override
            public void run() {
                returnDialog(null);
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
        jOkButton.setEnabled(false);
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
        String requestID = saveRequest.getRequestID();
        AccessToken<String> readToken = AccessTokens.createSyncToken(requestID);
        AccessResult<String> writeAccess = accessManager.tryGetAccess(saveRequest);
        if (!writeAccess.isAvailable()) {
            return;
        }

        RewTask<?, ?> rewTask = ExaltedSaveHelper.createSaveRewTask(
                getCombatName(),
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

        rewExecutor.executeNowAndRelease(
                rewTask,
                readToken,
                writeAccess.getAccessToken());
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
