package exaltedcombat.events;

import java.util.concurrent.*;

import javax.swing.event.*;

import org.jtrim.swing.concurrent.*;

/**
 * A simplified more convenient abstract implementation of
 * {@code DocumentListener}. In many cases an event handler of a document
 * want only to be notified about the fact that the document was changed and not
 * exactly how it was changed (it is usually the case with
 * {@link javax.swing.JTextField} components). In this case implementing this
 * abstract class is more convenient than implementing the
 * {@code DocumentListener} interface directly.
 * <P>
 * To use this interface only the
 * {@link #onChange(javax.swing.event.DocumentEvent) onChange(DocumentEvent)}
 * method need to be implemented.
 * <P>
 * This class will always invoke the {@code onChange} method on the AWT event
 * dispatching thread.
 *
 * @author Kelemen Attila
 */
public abstract class SimpleDocChangeListener implements DocumentListener {
    private static final Executor SWING_EXECUTOR = SwingTaskExecutor.getSimpleExecutor(false);

    /**
     * {@inheritDoc }
     * <P>
     * This method cannot be overridden and will forward calls to it to the
     * {@link #onChange(javax.swing.event.DocumentEvent) onChange(DocumentEvent)}
     * method. Therefore you need to override the {@code onChange} method
     * instead of this method.
     */
    @Override
    public final void changedUpdate(DocumentEvent e) {
        dispatchOnChange(e);
    }

    /**
     * {@inheritDoc }
     * <P>
     * This method cannot be overridden and will forward calls to it to the
     * {@link #onChange(javax.swing.event.DocumentEvent) onChange(DocumentEvent)}
     * method. Therefore you need to override the {@code onChange} method
     * instead of this method.
     */
    @Override
    public final void insertUpdate(DocumentEvent e) {
        dispatchOnChange(e);
    }

    /**
     * {@inheritDoc }
     * <P>
     * This method cannot be overridden and will forward calls to it to the
     * {@link #onChange(javax.swing.event.DocumentEvent) onChange(DocumentEvent)}
     * method. Therefore you need to override the {@code onChange} method
     * instead of this method.
     */
    @Override
    public final void removeUpdate(DocumentEvent e) {
        dispatchOnChange(e);
    }

    private void dispatchOnChange(final DocumentEvent e) {
        SWING_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                onChange(e);
            }
        });
    }

    /**
     * Invoked when the document has changed. This method must be overridden
     * to receive events of changes in the listened document.
     * <P>
     * Note that if required
     * {@link DocumentEvent#getType() DocumentEvent.getType()} can be used to
     * check what kind of modification was done on the underlying document.
     * <P>
     * This method will always be called from the AWT event dispatching thread.
     *
     * @param e the document event describing the change
     */
    protected abstract void onChange(DocumentEvent e);
}
