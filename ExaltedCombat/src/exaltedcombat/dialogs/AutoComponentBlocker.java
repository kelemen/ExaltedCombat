package exaltedcombat.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import javax.swing.*;

import org.jtrim.access.*;
import org.jtrim.swing.access.*;
import org.jtrim.swing.concurrent.*;
import org.jtrim.utils.*;

/**
 * An {@link AccessStateListener AccessStateListener} for
 * {@link AccessManager AccessManager}s which disables access to a specific
 * Swing component if it is unavailable. The component is required to be
 * a {@link JLayer JLayer} or top level window having a
 * {@link JRootPane root pane}.
 * <P>
 * <B>Note that once an instance of this class is created it must initialized
 * with the {@code AccessManager} using it.</B> This must be done exactly once
 * by calling
 * {@link #setAccessManager(org.jtrim.access.AccessManager) setAccessManager(AccessManager)}.
 * <P>
 * The component is blocked using its glass pane, therefore the glass pane
 * of the blocked component will be replaced while the component is blocked
 * (and will be restored once it is not blocked). Therefore this class is not
 * recommended to be used with components using their glass pane.
 * <P>
 * While the component is in blocked state the component will be darkened
 * giving it a disabled look and a message will be displayed on the middle
 * of the specified component. The message is defined by the result of the
 * {@link Object#toString() toString()} method of the
 * {@link SwingRight#getSubRight() subright} of the blocking
 * {@link SwingRight SwingRight} (an empty string if it is {@code null}).
 * Note that the component will not have a disabled look immediately to avoid
 * flickering when the component is blocked for only a short period of time.
 * Regardless this the component will still be blocked immediately, only it
 * will not have a visual indication of this blocking state for a very short
 * period of time.
 * <P>
 * Methods of this class can only be invoked on the AWT event dispatching
 * thread and are not required to be transparent to any synchronization.
 * <P>
 * Note that the implementation of this class will be somewhat redesigned and
 * this class will be moved to the jtrim library.
 *
 * @see SwingAccessManager
 * @author Kelemen Attila
 */
public final class AutoComponentBlocker implements AccessStateListener<SwingRight> {
    private static final int BLOCK_PATIENCE_MS = 200;

    private static final Logger LOGGER = Logger.getLogger(AutoComponentBlocker.class.getName());

    private final Collection<SwingRight> rootRights;
    private final ComponentBlocker blocker;
    private final AtomicReference<AccessManager<?, SwingRight>> accessManagerRef;

    /**
     * Creates a new {@code AutoComponentBlocker} blocking a window. The passed
     * component must inherit from (directly or indirectly)
     * {@link java.awt.Component Component}.
     * <P>
     * Don't forget to call
     * {@link #setAccessManager(org.jtrim.access.AccessManager) setAccessManager(AccessManager)}
     * on the newly created instance.
     *
     * @param window the window to be blocked. This argument cannot be
     *   {@code null} and must subclass {@link java.awt.Component Component}.
     *
     * @throws ClassCastException if the passed argument is not an instance of
     *   {@link java.awt.Component Component}
     * @throws NullPointerException if the passed argument is {@code null}
     */
    public AutoComponentBlocker(RootPaneContainer window) {
        this(new WindowWrapper(window));
    }

    /**
     * Creates a new {@code AutoComponentBlocker} blocking a specific component.
     * The passed {@link JLayer JLayer} must contain the component to be
     * blocked.
     * <P>
     * Don't forget to call
     * {@link #setAccessManager(org.jtrim.access.AccessManager) setAccessManager(AccessManager)}
     * on the newly created instance.
     *
     * @param component the component to be blocked. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException if the passed argument is {@code null}
     */
    public AutoComponentBlocker(JLayer<?> component) {
        this(new JLayerWrapper(component));
    }

    private AutoComponentBlocker(GlassPaneContainer container) {
        this.blocker = new ComponentBlocker(container);
        this.rootRights = Collections.singleton(new SwingRight(container.getComponent()));
        this.accessManagerRef = new AtomicReference<>(null);
    }

    /**
     * Initializes this component blocker with its owner
     * {@link AccessManager AccessManager}. This method must be called exactly
     * once for every instance of {@code AutoComponentBlocker}.
     *
     * @param accessManager the owner {@code AccessManager}. This argument
     *   cannot be {@code null}.
     *
     * @throws IllegalStateException thrown if this method was already called
     * @throws NullPointerException thrown if the passed argument is
     *   {@code null}
     */
    public void setAccessManager(AccessManager<?, SwingRight> accessManager) {
        ExceptionHelper.checkNotNullArgument(accessManager, "accessManager");

        if (!accessManagerRef.compareAndSet(null, accessManager)) {
            throw new IllegalStateException("AccessManager was already set");
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void onEnterState(SwingRight right, AccessState state) {
        if (!Objects.equals(right.getComponent(), blocker.getBlockedComponent())) {
            return;
        }

        // FIXME: This implementation is flawed:
        //   The message defined by the last right will remain until the
        //   component is available. This silently assumes that the last aquired
        //   right will be the last to keep the component from becoming
        //   available.
        //
        //   Note that regardless the above mentioned flaw, the component will
        //   still remain blocked if the component is unavailable.
        switch (state) {
            case UNAVAILABLE:
            {
                Object subRight = right.getSubRight();
                blocker.block(subRight != null ? subRight.toString() : "");
                break;
            }
            case AVAILABLE:
            {
                AccessManager<?, SwingRight> accessManager = accessManagerRef.get();
                if (accessManager != null) {
                    if (accessManager.getBlockingTokens(
                            Collections.<SwingRight>emptySet(), rootRights).isEmpty()) {
                        blocker.unblock();
                    }
                }
                else {
                    LOGGER.warning("AccessManager was not set.");
                }
                break;
            }
        }
    }

    private static void registerConsumers(Component component) {
        component.addMouseListener(new MouseAdapter() {});
        component.addMouseMotionListener(new MouseMotionAdapter() {});
        component.addMouseWheelListener(new MouseAdapter() {});
        component.addKeyListener(new KeyAdapter() {});
    }

    private static JPanel createInvisibleBlockingPanel() {
        JPanel result = new JPanel();
        registerConsumers(result);

        result.setOpaque(false);
        result.setBackground(ExaltedDialogHelper.TRANSPARENT_COLOR);
        return result;
    }

    private static BlockingMessagePanel createBlockingPanel(Component blockedComponent) {
        BlockingMessagePanel result = new BlockingMessagePanel();
        registerConsumers(result);

        Color bckg = blockedComponent.getBackground();
        // Just in case
        if (bckg == null) {
            bckg = Color.LIGHT_GRAY;
        }

        result.setOpaque(false);
        result.setPanelColor(bckg.darker(), 128);
        return result;
    }

    private interface GlassPaneContainer {
        public Component getGlassPane();
        public void setGlassPane(Component glassPane);
        public Component getComponent();
    }

    private static class JLayerWrapper implements GlassPaneContainer {
        private final JLayer<?> component;

        public JLayerWrapper(JLayer<?> component) {
            ExceptionHelper.checkNotNullArgument(component, "component");
            this.component = component;
        }

        @Override
        public void setGlassPane(Component glassPane) {
            component.setGlassPane((JPanel)glassPane);
            component.revalidate();
        }

        @Override
        public Component getGlassPane() {
            return component.getGlassPane();
        }

        @Override
        public Component getComponent() {
            return component;
        }
    }

    private static class WindowWrapper implements GlassPaneContainer {
        private final RootPaneContainer asContainer;
        private final Component asComponent;

        public WindowWrapper(RootPaneContainer window) {
            ExceptionHelper.checkNotNullArgument(window, "window");
            this.asContainer = window;
            this.asComponent = (Component)window;
        }

        @Override
        public void setGlassPane(Component glassPane) {
            asContainer.setGlassPane(glassPane);
            asComponent.revalidate();
        }

        @Override
        public Component getGlassPane() {
            return asContainer.getGlassPane();
        }

        @Override
        public Component getComponent() {
            return asComponent;
        }
    }

    private enum BlockState {
        UNBLOCKED, WAITING_TO_BLOCK, BLOCKED
    }

    private static class ComponentBlocker {
        private final Executor swingExecutor;

        private final GlassPaneContainer container;

        // accessed from the EDT
        private BlockState blockState;
        private boolean isOldVisible;
        private Component oldGlassPane;
        private BlockingMessagePanel blockingPanel;
        private javax.swing.Timer currentStartTimer;

        public ComponentBlocker(GlassPaneContainer container) {
            assert container != null;

            this.swingExecutor = SwingTaskExecutor.getStrictExecutor(false);
            this.container = container;
            this.isOldVisible = false;
            this.oldGlassPane = null;
            this.blockingPanel = null;
            this.blockState = BlockState.UNBLOCKED;
            this.currentStartTimer = null;
        }

        public Component getBlockedComponent() {
            return container.getComponent();
        }

        private void installGlassPane(Component glassPane) {
            assert SwingUtilities.isEventDispatchThread();
            assert glassPane != null;

            container.setGlassPane(glassPane);
            glassPane.setVisible(true);
            glassPane.requestFocusInWindow();
        }

        private void startBlock() {
            assert SwingUtilities.isEventDispatchThread();
            assert blockingPanel != null;

            blockState = BlockState.WAITING_TO_BLOCK;
            oldGlassPane = container.getGlassPane();
            isOldVisible = oldGlassPane != null
                    ? oldGlassPane.isVisible()
                    : false;

            installGlassPane(createInvisibleBlockingPanel());

            javax.swing.Timer startTimer = new javax.swing.Timer(BLOCK_PATIENCE_MS, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentStartTimer != e.getSource()) {
                        return;
                    }

                    currentStartTimer = null;
                    if (blockState == BlockState.WAITING_TO_BLOCK) {
                        blockState = BlockState.BLOCKED;
                        installGlassPane(blockingPanel);
                    }
                }
            });

            currentStartTimer = startTimer;
            startTimer.setRepeats(false);
            startTimer.start();
        }

        public void block(final String message) {
            swingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    BlockingMessagePanel currentPanel = blockingPanel;
                    if (blockState == BlockState.UNBLOCKED) {
                        assert currentPanel == null;
                        currentPanel = createBlockingPanel(container.getComponent());
                        blockingPanel = currentPanel;
                        currentPanel.setMessage(message);
                        startBlock();
                    }
                    else {
                        currentPanel.setMessage(message);
                    }
                }
            });
        }

        public void unblock() {
            swingExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    BlockState currentState = blockState;
                    blockState = BlockState.UNBLOCKED;
                    BlockingMessagePanel currentPanel = blockingPanel;
                    if (currentPanel != null) {
                        blockingPanel = null;
                        if (currentState != BlockState.UNBLOCKED) {
                            container.setGlassPane(oldGlassPane);
                            if (oldGlassPane != null) {
                                oldGlassPane.setVisible(isOldVisible);
                            }
                        }
                        // Don't retain an unnecessary reference
                        oldGlassPane = null;
                    }

                    if (currentStartTimer != null) {
                        currentStartTimer.stop();
                        currentStartTimer = null;
                    }
                }
            });
        }
    }
}
