package exaltedcombat.dialogs;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.*;

import org.jtrim.swing.concurrent.*;
import org.jtrim.utils.*;

import resources.icons.*;
import resources.strings.*;

/**
 * This frame can be used to detect that the JVM has failed to shutdown and
 * allow the user to forcefully terminate it.
 * <P>
 * When writing a Java application some call {@code System.exit(0)} directly
 * when the main frame is closed. This is generally discouraged for more
 * reasons: Security managers (like in an Applet) may not allow to invoke
 * {@code System.exit(int)}, therefore limiting the reusability of the code
 * using it. Also there can be outstanding unfinished tasks running in the
 * background, forcefully terminating them may cause dataloss. Therefore it
 * is a good practice to rely on the JVM to shutdown itself. The JVM shutdowns
 * itself automatically when there are only daemon threads alive. The caveat in
 * this is that sometimes due to programming errors a non-daemon thread could
 * remain alive (e.g.: an executor was not shutted down, an unhandled exception
 * in an AWT event handler may keep the AWT event dispatching thread running).
 * This error would cause the application to fail to terminate. This is a rather
 * serious issue since the avarage user will have no means to terminate the
 * application which could have several annoying side effects. This class was
 * designed to circumvent this problem by allowing the user to forcefully
 * terminate the application if it fails to exit within a reasonable time.
 * <P>
 * This class cannot be instantiated directly; instead it is intended to use
 * the following way: Once you expect the JVM to terminate call:
 * {@link #waitForTerminate(long, long, java.util.concurrent.TimeUnit)
 * TooLongTerminateFrame.waitForTerminate(long, long, TimeUnit)}. If the
 * application fails to terminate within a given time: a window will appear
 * allowing the user to either wait more or forcefully shutdown the JVM. Of
 * course a forceful shutdown may not be possible in case the installed security
 * manager forbids it.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class TooLongTerminateFrame extends javax.swing.JFrame {
    private static final long serialVersionUID = -7351262773561080207L;

    private static final LocalizedString FAILED_TO_TERMINATE_TITLE = StringContainer.getDefaultString("FAILED_TO_TERMINATE_TITLE");
    private static final LocalizedString FAILED_TO_TERMINATE_CAPTION = StringContainer.getDefaultString("FAILED_TO_TERMINATE_CAPTION");
    private static final LocalizedString FORCE_TERMINATE_BUTTON_CAPTION = StringContainer.getDefaultString("FORCE_TERMINATE_BUTTON_CAPTION");
    private static final LocalizedString WAIT_MORE_TO_TERMINATE_BUTTON_CAPTION = StringContainer.getDefaultString("WAIT_MORE_TO_TERMINATE_BUTTON_CAPTION");
    private static final LocalizedString CONFIRM_FORCEFUL_TERMINATE_CAPTION = StringContainer.getDefaultString("CONFIRM_FORCEFUL_TERMINATE_CAPTION");
    private static final LocalizedString CONFIRM_FORCEFUL_TERMINATE_TEXT = StringContainer.getDefaultString("CONFIRM_FORCEFUL_TERMINATE_TEXT");

    private long waitTimeNanos;

    private TooLongTerminateFrame() {
        initComponents();

        waitTimeNanos = TimeUnit.MINUTES.toNanos(1);

        setTitle(FAILED_TO_TERMINATE_TITLE.toString());
        setIconImage(IconStorage.getMainIcon());
        jCaption.setText(FAILED_TO_TERMINATE_CAPTION.toString());
        jForceTerminateButton.setText(FORCE_TERMINATE_BUTTON_CAPTION.toString());
        jWaitMoreButton.setText(WAIT_MORE_TO_TERMINATE_BUTTON_CAPTION.toString());

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                waitForTerminate(waitTimeNanos, waitTimeNanos, TimeUnit.NANOSECONDS);
            }
        });
    }

    private void setWaitTime(long waitTime, TimeUnit timeUnit) {
        this.waitTimeNanos = timeUnit.toNanos(waitTime);
    }

    /**
     * Displays a dialog if the JVM does not terminate within the specified
     * time. The dialog will allow the user to forcefully terminate the JVM (if
     * the security manager allows it) or wait some more time.
     * <P>
     * This method is safe to call from any thread (not just the AWT event
     * dispatching thread) and from any context.
     *
     * @param firstWaitTime the time to wait before the window notifying the
     *   user will appear. This argument must be a non-negative value.
     * @param nextWaitTime the time to wait when the user chooses to wait more
     *   time when dialog appeared. This argument must be a non-negative value.
     * @param timeUnit the time unit of the arguments (e.g.: milliseconds,
     *   seconds). This argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if any of the specified time
     *   argument is negative
     * @throws NullPointerException thrown if the specified time unit is
     *   {@code null}
     */
    public static void waitForTerminate(
            final long firstWaitTime,
            final long nextWaitTime,
            final TimeUnit timeUnit) {
        ExceptionHelper.checkArgumentInRange(firstWaitTime, 0, Long.MAX_VALUE, "firstWaitTime");
        ExceptionHelper.checkArgumentInRange(nextWaitTime, 0, Long.MAX_VALUE, "nextWaitTime");
        ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");

        Thread waitThread = new Thread("Wait to terminate thread") {
            @Override
            public void run() {
                try {
                    timeUnit.sleep(firstWaitTime);
                    SwingTaskExecutor.getSimpleExecutor(true).execute(new Runnable() {
                        @Override
                        public void run() {
                            TooLongTerminateFrame frame = new TooLongTerminateFrame();
                            frame.setLocationRelativeTo(null);
                            frame.setWaitTime(nextWaitTime, timeUnit);
                            frame.setVisible(true);
                        }
                    });
                } catch (InterruptedException ex) {
                    // The thread will terminate
                }
            }
        };

        waitThread.setDaemon(true);
        waitThread.start();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCaption = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jForceTerminateButton = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jWaitMoreButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jCaption.setText("Still not terminated ...");

        jForceTerminateButton.setText("Force terminate");
        jForceTerminateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jForceTerminateButtonActionPerformed(evt);
            }
        });
        jPanel1.add(jForceTerminateButton);

        jWaitMoreButton.setText("Wait more");
        jWaitMoreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jWaitMoreButtonActionPerformed(evt);
            }
        });
        jPanel2.add(jWaitMoreButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCaption)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 373, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jForceTerminateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jForceTerminateButtonActionPerformed
        if (ExaltedDialogHelper.askYesNoQuestion(this,
                CONFIRM_FORCEFUL_TERMINATE_CAPTION.toString(),
                CONFIRM_FORCEFUL_TERMINATE_TEXT.toString(),
                false)) {
            System.exit(1);
        }
    }//GEN-LAST:event_jForceTerminateButtonActionPerformed

    private void jWaitMoreButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jWaitMoreButtonActionPerformed
        dispose();
    }//GEN-LAST:event_jWaitMoreButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jCaption;
    private javax.swing.JButton jForceTerminateButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton jWaitMoreButton;
    // End of variables declaration//GEN-END:variables
}
