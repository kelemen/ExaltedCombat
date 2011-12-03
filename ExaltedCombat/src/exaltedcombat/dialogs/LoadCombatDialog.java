package exaltedcombat.dialogs;

import exaltedcombat.save.*;

import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import javax.swing.*;

import org.jtrim.swing.concurrent.*;

import resources.strings.*;

/**
 * Defines a dialog which allows the user to choose a previously saved combat
 * to be loaded. This dialog does not actually loads this file but users of
 * this class can retrieve the reference to the filename of the saved state
 * which the user choose.
 * <P>
 * Once the user has choosen a specific file to load and closes the dialog:
 * Check {@link #isAccepted() isAccepted()} and if it returns {@code true},
 * load the file returned by {@link #getChoosenCombat() getChoosenCombat()}.
 * <P>
 * This dialog is not intended to be reused once the user has closed it. To
 * display a new dialog: Create a new instance of this dialog.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @see SaveCombatDialog
 * @author Kelemen Attila
 */
public class LoadCombatDialog extends JDialog {
    private static final long serialVersionUID = -4448670043251992205L;

    private static final LocalizedString EXCEPTION_MESSAGE_TEXT = StringContainer.getDefaultString("EXCEPTION_MESSAGE_TEXT");
    private static final LocalizedString ERROR_WHILE_DETECTING_LOAD_FILES_CAPTION = StringContainer.getDefaultString("ERROR_WHILE_DETECTING_LOAD_FILES_CAPTION");

    private static Collator STR_CMP = StringContainer.getDefault().getStringCollator();

    private static final Executor SWING_EXECUTOR = SwingTaskExecutor.getSimpleExecutor(true);
    private final ExecutorService taskExecutor;

    private Path choosenCombat;
    private boolean accepted;

    /**
     * Creates a new dialog which can be used to select a previously saved
     * combat state. The available files will only be retrieved when this
     * dialog is displayed.
     *
     * @param parent the {@code Frame} from which the dialog is displayed
     * @param modal specifies whether dialog blocks user input to other
     *   top-level windows when shown. If {@code true}, the modality type
     *   property is set to {@code DEFAULT_MODALITY_TYPE}, otherwise the dialog
     *   is modeless.
     */
    public LoadCombatDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        this.choosenCombat = null;
        this.accepted = false;
        this.taskExecutor = Executors.newSingleThreadExecutor();

        jCombatCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                checkEnable();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                taskExecutor.execute(new CollectSaveFilesTask());
            }

            @Override
            public void windowClosed(WindowEvent e) {
                taskExecutor.shutdownNow();
            }
        });
    }

    private void checkEnable() {
        jLoadButton.setEnabled(jCombatCombo.getSelectedItem() != null);
    }

    /**
     * Returns the filename of the previously saved combat state choosen by the
     * user. The return value of this method is only defined if
     * {@link #isAccepted() isAccepted()} returns {@code true}.
     *
     * @return the filename of the previously saved combat state choosen by the
     *   user. This method never returns {@code null} if {@code isAccepted()}
     *   returns {@code true}.
     */
    public Path getChoosenCombat() {
        return choosenCombat;
    }

    /**
     * Checks whether the user accepted to load a specific saved state. The user
     * can accept to load a specific saved state by clicking the "Ok" button and
     * cancel it by closing this dialog by any other means
     * (e.g.: clicking "Cancel").
     * <P>
     * If this method returns {@code true}, use the path returned by
     * {@link #getChoosenCombat() getChoosenCombat()} to determine the name of
     * the file to be loaded.
     *
     * @return {@code true} if the user accepted to load a specific saved
     *   state, {@code false} otherwise
     */
    public boolean isAccepted() {
        return accepted && choosenCombat != null;
    }

    private void showError(final String caption, final String errorText) {
        SWING_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                // Don't annoy the user with an error message if she/he has
                // already closed it.
                if (isVisible()) {
                    JOptionPane.showMessageDialog(LoadCombatDialog.this,
                            errorText, caption, JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private enum CombatFileItemComparator implements Comparator<CombatFileItem> {
        INSTANCE;

        @Override
        public int compare(CombatFileItem o1, CombatFileItem o2) {
            return STR_CMP.compare(o1.getName(), o2.getName());
        }
    }

    private static class CombatFileItem {
        private final String name;
        private final Path filePath;

        public CombatFileItem(Path filePath) {
            String fileName = filePath.getFileName().toString();
            String extension = ExaltedSaveHelper.SAVEFILE_EXTENSION;
            if (fileName.endsWith(extension)) {
                fileName = fileName.substring(0, fileName.length() - extension.length());
            }

            this.name = fileName;
            this.filePath = filePath;
        }

        public String getName() {
            return name;
        }

        public Path getFilePath() {
            return filePath;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class CollectSaveFilesTask implements Runnable {
        private List<CombatFileItem> collectFiles() throws IOException, InterruptedException {
            Path savePath = ExaltedSaveHelper.SAVE_HOME;
            String searchPattern = "*" + ExaltedSaveHelper.SAVEFILE_EXTENSION;

            List<CombatFileItem> result = new LinkedList<>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(savePath, searchPattern)) {
                for (Path file: files) {
                    // Clears the interrupted status as well
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    result.add(new CombatFileItem(file));
                }
            }
            return result;
        }

        @Override
        public void run() {
            List<CombatFileItem> combatFiles;
            try {
                combatFiles = collectFiles();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable ex) {
                showError(
                        ERROR_WHILE_DETECTING_LOAD_FILES_CAPTION.toString(),
                        EXCEPTION_MESSAGE_TEXT.format(ex.toString()));
                return;
            }

            final CombatFileItem[] comboEntries = combatFiles.toArray(new CombatFileItem[combatFiles.size()]);
            Arrays.sort(comboEntries, CombatFileItemComparator.INSTANCE);

            SWING_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    jCombatCombo.setModel(
                            new DefaultComboBoxModel<>(comboEntries));

                    if (jCombatCombo.getItemCount() > 0) {
                        jCombatCombo.setSelectedIndex(0);
                    }
                    checkEnable();
                }
            });
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
        jLoadButton = new javax.swing.JButton();
        jCancelButton = new javax.swing.JButton();
        jCombatCombo = new javax.swing.JComboBox<CombatFileItem>();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLoadButton.setText("Load");
        jLoadButton.setEnabled(false);
        jLoadButton.setPreferredSize(new java.awt.Dimension(75, 23));
        jLoadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadButtonActionPerformed(evt);
            }
        });
        jPanel1.add(jLoadButton);

        jCancelButton.setText("Cancel");
        jCancelButton.setPreferredSize(new java.awt.Dimension(75, 23));
        jCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCancelButtonActionPerformed(evt);
            }
        });
        jPanel1.add(jCancelButton);

        jLabel1.setText("Saved combats:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(jLabel1)
                    .addComponent(jCombatCombo, 0, 380, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCombatCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jLoadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadButtonActionPerformed
        accepted = true;
        CombatFileItem selected = (CombatFileItem)jCombatCombo.getSelectedItem();
        // The user should not be able to click the load button if there is
        // nothing selected but it does not hurt to check for it anyway.
        choosenCombat = selected != null ? selected.getFilePath() : null;
        dispose();
    }//GEN-LAST:event_jLoadButtonActionPerformed

    private void jCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_jCancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jCancelButton;
    private javax.swing.JComboBox<CombatFileItem> jCombatCombo;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JButton jLoadButton;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables
}
