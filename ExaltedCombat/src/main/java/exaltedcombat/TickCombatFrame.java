package exaltedcombat;

import exaltedcombat.dialogs.*;
import exaltedcombat.events.EntitySelectChangeArgs;
import exaltedcombat.events.ExaltedEvent;
import exaltedcombat.events.RecursionStopperEventTracker;
import exaltedcombat.events.WorldEvent;
import exaltedcombat.models.CombatPosEventListener;
import exaltedcombat.models.CombatPositionModel;
import exaltedcombat.models.CombatState;
import exaltedcombat.models.CombatStateChangeListener;
import exaltedcombat.models.impl.*;
import exaltedcombat.panels.*;
import exaltedcombat.save.*;
import java.awt.Component;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.jtrim.access.*;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.event.EventTracker;
import org.jtrim.event.LinkedEventTracker;
import org.jtrim.event.TrackedEvent;
import org.jtrim.event.TrackedEventListener;
import org.jtrim.swing.access.ComponentDecorator;
import org.jtrim.swing.access.DecoratorPanelFactory;
import org.jtrim.swing.access.DelayedDecorator;
import org.jtrim.swing.concurrent.BackgroundTask;
import org.jtrim.swing.concurrent.BackgroundTaskExecutor;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.swing.concurrent.async.BackgroundDataProvider;
import resources.icons.IconStorage;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * The main frame of ExaltedCombat. The parts of this frame and therefore most
 * of the code can be found in the panels of the {@code exaltedcombat.panels}
 * package. Currently the code creates the model of the
 * {@link CombatEntityWorldModel world model}, the
 * {@link EventTracker event tracker}, the {@link UndoManager undo manager} and
 * connects the frame using them.
 * <P>
 * Unless otherwise noted the methods of this class must be called on the AWT
 * event dispatch thread.
 * <P>
 * Note once this frame was disposed of it will be unusable forever and must not
 * be made visible again.
 * <P>
 * This class also contains a {@link #main(java.lang.String[]) main} method, so
 * it can be an entrypoint of an application.
 * <P>
 * <B>The layout of the gui must only be edited using the gui builder of
 * NetBeans. This in practice means that the {@code initComponents()} must
 * not be directly edited.</B>
 * <P>
 * TODO: This class should be refactored to use a single panel instead
 * which will allow us to reuse it in environment where a {@link JFrame JFrame}
 * is not a good solution.
 *
 * @author Kelemen Attila
 */
public class TickCombatFrame extends JFrame {
    private static final long serialVersionUID = -6142616359136916398L;

    private static final Logger LOGGER = Logger.getLogger(TickCombatFrame.class.getName());

    private static final LocalizedString FRAME_TITLE = StringContainer.getDefaultString("MAIN_FRAME_TITLE");
    private static final LocalizedString MAIN_FRAME_TITLE_INIT_COMBAT = StringContainer.getDefaultString("MAIN_FRAME_TITLE_INIT_COMBAT");
    private static final LocalizedString MAIN_FRAME_TITLE_STARTED_COMBAT = StringContainer.getDefaultString("MAIN_FRAME_TITLE_STARTED_COMBAT");

    private static final LocalizedString CONFIRM_UNSAVED_EXIT_CAPTION = StringContainer.getDefaultString("CONFIRM_UNSAVED_EXIT_CAPTION");
    private static final LocalizedString CONFIRM_UNSAVED_EXIT_TEXT = StringContainer.getDefaultString("CONFIRM_UNSAVED_EXIT_TEXT");
    private static final LocalizedString DISABLED_UNDO_TEXT = StringContainer.getDefaultString("DISABLED_UNDO_TEXT");
    private static final LocalizedString DISABLED_REDO_TEXT = StringContainer.getDefaultString("DISABLED_REDO_TEXT");
    private static final LocalizedString FILE_MENU_CAPTION = StringContainer.getDefaultString("FILE_MENU_CAPTION");
    private static final LocalizedString NEW_COMBAT_BUTTON_CAPTION = StringContainer.getDefaultString("NEW_COMBAT_BUTTON_CAPTION");
    private static final LocalizedString LOAD_COMBAT_BUTTON_CAPTION = StringContainer.getDefaultString("LOAD_COMBAT_BUTTON_CAPTION");
    private static final LocalizedString SAVE_AS_BUTTON_CAPTION = StringContainer.getDefaultString("SAVE_AS_BUTTON_CAPTION");

    private static final LocalizedString LOADING_IN_PROGRESS_CAPTION = StringContainer.getDefaultString("LOADING_IN_PROGRESS_CAPTION");
    private static final LocalizedString SAVING_IN_PROGRESS_CAPTION = StringContainer.getDefaultString("SAVING_IN_PROGRESS_CAPTION");
    private static final LocalizedString BACKGROUND_TASK_IN_PROGRESS_CAPTION = StringContainer.getDefaultString("BACKGROUND_TASK_IN_PROGRESS_CAPTION");

    private static final LocalizedString ERROR_WHILE_SAVING_CAPTION = StringContainer.getDefaultString("ERROR_WHILE_SAVING_CAPTION");
    private static final LocalizedString ERROR_WHILE_LOADING_CAPTION = StringContainer.getDefaultString("ERROR_WHILE_LOADING_CAPTION");

    private static final TaskID SAVETASK_ID = new TaskID("SAVE", SAVING_IN_PROGRESS_CAPTION.toString());
    private static final TaskID LOADTASK_ID = new TaskID("LOAD", LOADING_IN_PROGRESS_CAPTION.toString());

    private static final HierarchicalRight BACKROUND_TASK_RIGHT
            = HierarchicalRight.create(RootRight.BACKGROUND_TASK);

    private static final AccessRequest<TaskID, HierarchicalRight> SAVE_REQUEST =
            AccessRequest.getWriteRequest(SAVETASK_ID,
            BACKROUND_TASK_RIGHT.createSubRight(BackgrounTaskRight.SAVING));

    private static final AccessRequest<TaskID, HierarchicalRight> LOAD_REQUEST =
            AccessRequest.getWriteRequest(LOADTASK_ID,
            BACKROUND_TASK_RIGHT.createSubRight(BackgrounTaskRight.LOADING));

    private final EventTracker eventTracker;
    private final CombatEntityWorldModel worldModel;
    private final GeneralCombatModel combatModel; // equals to worldModel.getCombatModel()

    private final EntityStorageFrame storeFrame;

    // can be null
    // do not modify directly, call the appropriate method call
    // If this field is not null, the current state of the combat will be saved
    // to this path. Else the user need to speecify the filename.
    private Path currentCombatPath;

    // do not modify directly, call the appropriate method call
    private boolean definitionsModified;

    private final UndoManager undoManager;

    private final TaskExecutorService backgroundExecutor;
    private final BackgroundTaskExecutor<TaskID, HierarchicalRight> bckgTaskExecutor;
    private final BackgroundDataProvider<TaskID, HierarchicalRight> bckgDataProvider;
    private final AccessManager<TaskID, HierarchicalRight> accessManager;

    /**
     * The future controlling the outstanding combat loading process.
     *
     */
    private final CancellationSource dlgCancelSource;

    /**
     * Creates a new frame containing an empty population and noone in combat.
     * One of the {@code loadCombat} methods can be used to load a state of the
     * world.
     */
    public TickCombatFrame() {
        this.dlgCancelSource = Cancellation.createCancellationSource();
        this.backgroundExecutor = new ThreadPoolTaskExecutor("ExaltedCombat Executor", 1);

        this.accessManager = new HierarchicalAccessManager<>(
                SwingTaskExecutor.getStrictExecutor(false));

        this.bckgTaskExecutor = new BackgroundTaskExecutor<>(accessManager, backgroundExecutor);
        this.bckgDataProvider = new BackgroundDataProvider<>(accessManager);

        AccessAvailabilityNotifier<HierarchicalRight> rightHandler = AccessAvailabilityNotifier.attach(accessManager);
        DecoratorPanelFactory blockingPanelFactory = new DecoratorPanelFactory() {
            @Override
            public JPanel createPanel(Component decorated) {
                BlockingMessagePanel result = new BlockingMessagePanel();
                result.addMouseListener(new MouseAdapter() {});
                result.addMouseMotionListener(new MouseMotionAdapter() {});
                result.addMouseWheelListener(new MouseAdapter() {});
                result.addKeyListener(new KeyAdapter() {});

                Collection<AccessToken<TaskID>> blockingTokens
                        = accessManager.getBlockingTokens(
                            Collections.<HierarchicalRight>emptySet(),
                            Collections.<HierarchicalRight>singleton(BACKROUND_TASK_RIGHT));
                if (!blockingTokens.isEmpty()) {
                    // Only display the message of the first blocking task.
                    result.setMessage(blockingTokens.iterator().next().getAccessID().toString());
                }
                else {
                    result.setMessage(BACKGROUND_TASK_IN_PROGRESS_CAPTION.toString());
                }
                return result;
            }
        };
        ComponentDecorator decorator = new ComponentDecorator(
                this,
                new DelayedDecorator(blockingPanelFactory, 200, TimeUnit.MILLISECONDS));
        rightHandler.addGroupListener(
                null,
                Collections.singleton(BACKROUND_TASK_RIGHT),
                decorator);

        this.undoManager = new UndoManager();
        this.definitionsModified = false;
        this.currentCombatPath = null;
        this.eventTracker = new RecursionStopperEventTracker(new LinkedEventTracker());

        initComponents();

        storeFrame = new EntityStorageFrame(new EntityStorage() {
            @Override
            public Collection<CombatEntity> getStoredEntities() {
                Collection<CombatEntity> stored = worldModel.getPopulationModel().getEntities();

                List<CombatEntity> result = new ArrayList<>(stored.size());
                for (CombatEntity entity: stored) {
                    result.add(new CombatEntity(entity));
                }
                return result;
            }

            @Override
            public void clearEntities() {
                worldModel.getPopulationModel().clear();
            }

            @Override
            public void storeEntity(CombatEntity entity) {
                storeEntities(Collections.singleton(entity));
            }

            @Override
            public void storeEntities(Collection<? extends CombatEntity> toStore) {
                List<CombatEntity> newEntities = new ArrayList<>(toStore.size());
                for (CombatEntity entityDef: toStore) {
                    newEntities.add(new CombatEntity(entityDef));
                }
                worldModel.getPopulationModel().addEntities(newEntities);
            }
        });

        ActionDisplayPanel actionDisplayPanel = new ActionDisplayPanel();
        CombatPositionPanel posPanel = new CombatPositionPanel();
        EntityDescriptionPanel entityDescriptionPanel = new EntityDescriptionPanel();
        EntityOrganizerPanel entityOrganizerPanel = new EntityOrganizerPanel();
        InCombatActionPanel inCombatActionPanel = new InCombatActionPanel();

        this.combatModel = new GeneralCombatModel(posPanel.getCombatPositionModel());
        this.worldModel = new GeneralCombatEntityWorldModel(
                new CombatEntities(100),
                combatModel,
                storeFrame);

        registerWorldEvents();

        actionDisplayPanel.setEventTracker(eventTracker);
        actionDisplayPanel.setWorldModel(worldModel);

        posPanel.setEventTracker(eventTracker);
        posPanel.setPopulationModel(worldModel.getPopulationModel());

        entityDescriptionPanel.setEventTracker(eventTracker);
        entityDescriptionPanel.setWorldModel(worldModel);

        entityOrganizerPanel.setEventTracker(eventTracker);
        entityOrganizerPanel.setWorldModel(worldModel);
        entityOrganizerPanel.setStoreFrame(storeFrame);
        entityOrganizerPanel.setUndoManager(undoManager);

        inCombatActionPanel.setEventTracker(eventTracker);
        inCombatActionPanel.setWorldModel(worldModel);
        inCombatActionPanel.setUndoManager(undoManager);

        jMainSplitPane.setRightComponent(posPanel);
        jTopSplitPane.setLeftComponent(entityOrganizerPanel);
        jTopSplitPane.setRightComponent(entityDescriptionPanel);
        jMiddleSplitPane.setLeftComponent(inCombatActionPanel);
        jMiddleSplitPane.setRightComponent(actionDisplayPanel);

        setComponentProperties();

        updateTitle();
        setIconImage(IconStorage.getMainIcon());

        defineEventHandlers();
    }

    /**
     * This method is expected to be called only from the constructor and only
     * once.
     */
    private void setComponentProperties() {
        setComponentCaptions();
    }

    private void setComponentCaptions() {
        updateRedoUndoButtons();

        jFileMenu.setText(FILE_MENU_CAPTION.toString());
        jNewCombatButton.setText(NEW_COMBAT_BUTTON_CAPTION.toString());
        jLoadCombatButton.setText(LOAD_COMBAT_BUTTON_CAPTION.toString());
        jSaveAsButton.setText(SAVE_AS_BUTTON_CAPTION.toString());
    }

    /**
     * This method is expected to be called only from the constructor and only
     * once.
     */
    private void defineEventHandlers() {
        jEditMenu.getPopupMenu().addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                updateRedoUndoButtons();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    dlgCancelSource.getController().cancel();

                    saveCurrentCombat(new SaveDoneListener() {
                        @Override
                        public void onFailedSave(Throwable saveError) {
                            if (ExaltedDialogHelper.askYesNoQuestion(TickCombatFrame.this,
                                    CONFIRM_UNSAVED_EXIT_CAPTION.toString(),
                                    CONFIRM_UNSAVED_EXIT_TEXT.toString(),
                                    false)) {
                                dispose();
                            }
                        }

                        @Override
                        public void onSuccessfulSave(Path path) {
                            dispose();
                        }
                    });
                } catch (Throwable ex) {
                    dispose();
                    LOGGER.log(Level.SEVERE, "Unexpected exception while closing the main frame.", ex);
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    try {
                        ExaltedSaveHelper.storeLastUsedPath(currentCombatPath);
                    } catch (Throwable ex) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.log(Level.SEVERE,
                                    "Failed to save the last used combat name.",
                                    ex);
                        }
                    }
                } finally {
                    storeFrame.dispose();
                }
            }
        });

        ExaltedEvent.Helper.register(eventTracker, WorldEvent.ENTITY_NAME_CHANGE,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        ExaltedEvent.Helper.register(eventTracker, WorldEvent.ENTITY_COLOR_CHANGE,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_DESCRIPTION_CHANGE,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_PREV_ACTION_CHANGE,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_LIST_CHANGE,
                new TrackedEventListener<Void>() {
            @Override
            public void onEvent(TrackedEvent<Void> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_MOVE_IN_COMBAT,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_MOVE_IN_COMBAT,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_ENTER_COMBAT,
                new TrackedEventListener<CombatEntity>() {
            @Override
            public void onEvent(TrackedEvent<CombatEntity> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITIES_LEAVE_COMBAT,
                new TrackedEventListener<Collection<?>>() {
            @Override
            public void onEvent(TrackedEvent<Collection<?>> trackedEvent) {
                setModified();
            }
        });

        registerListener(WorldEvent.ENTITY_COMBAT_PHASE_CHANGE,
                new TrackedEventListener<Void>() {
            @Override
            public void onEvent(TrackedEvent<Void> trackedEvent) {
                updateTitle();
            }
        });

        registerListener(FrameEvent.PATH_CHANGED,
                new TrackedEventListener<Void>() {
            @Override
            public void onEvent(TrackedEvent<Void> trackedEvent) {
                updateTitle();
            }
        });
    }

    private void setModified() {
        definitionsModified = true;
    }

    private void setAsNotModified() {
        definitionsModified = false;
    }

    private boolean isModified() {
        return definitionsModified;
    }

    private <ArgType> void registerListener(ExaltedEvent<ArgType> eventKind,
                TrackedEventListener<ArgType> eventListener) {
        ExaltedEvent.Helper.register(eventTracker, eventKind, eventListener);
    }

    private <ArgType> void triggerEvent(
            ExaltedEvent<ArgType> eventKind,
            ArgType eventArgument) {
        ExaltedEvent.Helper.triggerEvent(eventTracker, eventKind, eventArgument);
    }

    private void setCombatPath(Path combatPath) {
        this.currentCombatPath = combatPath;
        triggerEvent(FrameEvent.PATH_CHANGED, null);
    }

    private void updateTitle() {
        Path path = currentCombatPath;
        boolean inCombat = combatModel.getCombatState() != CombatState.JOIN_PHASE;

        String appCaption = FRAME_TITLE.toString();
        StringBuilder mainTitle = new StringBuilder(appCaption.length() + 64);
        mainTitle.append(appCaption);

        if (path != null) {
            String fileName = path.getFileName().toString();
            int extIndex = fileName.lastIndexOf('.');
            String combatName = extIndex >= 0
                    ? fileName.substring(0, extIndex)
                    : fileName;

            mainTitle.append(" [");
            mainTitle.append(combatName);
            mainTitle.append("]");
        }

        LocalizedString titleFormatter;
        if (inCombat) {
            titleFormatter = MAIN_FRAME_TITLE_STARTED_COMBAT;
        }
        else {
            titleFormatter = MAIN_FRAME_TITLE_INIT_COMBAT;
        }

        setTitle(titleFormatter.format(mainTitle.toString()));
    }

    private void updateRedoUndoButtons() {
        if (undoManager.canUndo()) {
            String text = undoManager.getUndoPresentationName();
            jUndoButton.setText(text);
            jUndoButton.setEnabled(true);
        }
        else {
            jUndoButton.setText(DISABLED_UNDO_TEXT.toString());
            jUndoButton.setEnabled(false);
        }

        if (undoManager.canRedo()) {
            String text = undoManager.getRedoPresentationName();
            jRedoButton.setText(text);
            jRedoButton.setEnabled(true);
        }
        else {
            jRedoButton.setText(DISABLED_REDO_TEXT.toString());
            jRedoButton.setEnabled(false);
        }
    }

    private boolean saveCurrentCombat(SaveDoneListener listener) {
        final SaveDoneListener idempotentListener = new IdempotentSaveListener(listener);

        CombatPositionModel<CombatEntity> posModel = worldModel.getCombatModel().getPositionModel();
        if (!isModified() && posModel.getNumberOfEntities() == 0) {
            idempotentListener.onSuccessfulSave(currentCombatPath);
            return true;
        }

        SaveInfo toSave = ExaltedSaveHelper.createSaveInfo(
                worldModel,
                storeFrame.getStoredEntities());

        if (currentCombatPath != null) {
            BackgroundTask saveTask = ExaltedSaveHelper.createSaveRewTask(
                    currentCombatPath, toSave, true, idempotentListener);
            return bckgTaskExecutor.tryExecute(SAVE_REQUEST, saveTask) == null;
        }

        SaveCombatDialog saveDlg = new SaveCombatDialog(this, true, toSave);
        saveDlg.setLocationRelativeTo(this);
        saveDlg.setVisible(true);
        if (saveDlg.isAccepted()) {
            Path newSavePath = saveDlg.getSavePath();
            setCombatPath(newSavePath);
            idempotentListener.onSuccessfulSave(newSavePath);
        }
        else {
            setCombatPath(null);
            idempotentListener.onFailedSave(null);
        }
        return true;
    }

    /**
     * Sets up the world model and therefore the frame to display the specified
     * world state.
     * <P>
     * This method will overwrite the content of the whole frame without saving
     * the data it contained.
     *
     * @param saveInfo the new world state to use. This argument cannot be
     *   {@code null}.
     * @param sourcePath the path to the file from which the file was loaded.
     *   The combat will be saved to this path automatically without asking the
     *   user if she/he tries to unload it. This argument can be {@code null},
     *   in which case the user will be asked to enter a filename for the save.
     *
     * @throws NullPointerException thrown if {@code saveInfo} is {@code null}
     */
    public void loadCombat(SaveInfo saveInfo, Path sourcePath) {
        combatModel.resetCombat();
        worldModel.getPopulationModel().clear();

        storeFrame.clearEntities();
        List<CombatEntity> hiddenEntities = new LinkedList<>();
        for (SavedEntityInfo entity: saveInfo.getHiddenEntities()) {
            hiddenEntities.add(entity.toCombatEntity());
        }
        storeFrame.storeEntities(hiddenEntities);

        CombatPositionModel<CombatEntity> posModel = combatModel.getPositionModel();
        List<CombatEntity> newEntities = new LinkedList<>();

        for (SavedActiveEntityInfo entity: saveInfo.getReadyEntities()) {
            newEntities.add(entity.toCombatEntity());
        }

        for (SavedCombatEntity entity: saveInfo.getCombatEntities()) {
            CombatEntity newEntity = entity.getActiveInfo().toCombatEntity();
            newEntities.add(newEntity);

            posModel.moveToTick(newEntity, entity.getTick());
            combatModel.setPreStartJoinRoll(newEntity, entity.getPreStartRoll());
        }

        worldModel.getPopulationModel().addEntities(newEntities);

        if (saveInfo.isCombatWasStarted()) {
            combatModel.endJoinPhase();
        }

        setCombatPath(sourcePath);
        setAsNotModified();
    }

    /**
     * Starts loading the world state from the given file and once it was loaded
     * this frame will be set up as if it were done by a call to
     * {@link #loadCombat(SaveInfo, Path) loadCombat(SaveInfo, Path)}.
     * <P>
     * The frame will be disabled while loading and the user cannot do anything
     * but close this frame while loading the file (this implicitly cancels
     * loading the file). Note that the frame will be disabled during the whole
     * duration of the file loading but visual indicator (darkening the frame
     * and displaying "Loading ...") will only appear after a few milliseconds
     * to avoid flickering in the case loading is fast (which usually is).
     * <P>
     * Note that this method will eventually clear all the content of this frame
     * without saving it.
     * <P>
     * The file is expected to contain a single serialized
     * {@link SaveInfo SaveInfo} instance. In any case the file cannot be
     * loaded, the exception will be displayed to user.
     *
     * @param file the file containing the world state. This argumen cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if {@code file} is {@code null}
     */
    public void loadCombat(final Path file) {
        AsyncDataLink<SaveInfo> loadLink = ExaltedSaveHelper.createLoadRewTask(file, backgroundExecutor);
        loadLink = bckgDataProvider.createLink(SAVE_REQUEST, loadLink);
        loadLink.getData(dlgCancelSource.getToken(), new AsyncDataListener<SaveInfo>() {
            private SaveInfo saveInfo = null;

            @Override
            public void onDataArrive(SaveInfo data) {
                saveInfo = data;
            }

            @Override
            public void onDoneReceive(AsyncReport report) {
                if (report.isCanceled()) {
                    return;
                }

                if (report.isSuccess()) {
                    loadCombat(saveInfo, file);
                }
                else {
                    ExaltedDialogHelper.displayError(
                            TickCombatFrame.this,
                            ERROR_WHILE_LOADING_CAPTION.toString(),
                            report.getException());
                }
            }
        });
    }

    /**
     * Opens the dialog to load the combat. This method will not save the
     * current world state.
     */
    private void loadCombat() {
        LoadCombatDialog loadDlg = new LoadCombatDialog(this, true);
        loadDlg.setLocationRelativeTo(this);
        loadDlg.setVisible(true);

        if (loadDlg.isAccepted()) {
            setCombatPath(null);
            setAsNotModified();

            Path choosenPath = loadDlg.getChoosenCombat();
            loadCombat(choosenPath);
        }
    }

    private void registerWorldEvents() {
        combatModel.addCombatStateChangeListener(new CombatStateChangeListener() {
            @Override
            public void onChangeCombatState(CombatState state) {
                ExaltedEvent.Helper.triggerEvent(eventTracker,
                        WorldEvent.ENTITY_COMBAT_PHASE_CHANGE, null);
            }
        });

        CombatPositionModel<CombatEntity> positionModel = combatModel.getPositionModel();
        positionModel.addCombatPosListener(new CombatPosEventListener<CombatEntity>(){
            @Override
            public void enterCombat(CombatEntity entity, int tick) {
                triggerEvent(WorldEvent.ENTITY_ENTER_COMBAT, entity);
            }

            @Override
            public void leaveCombat(Collection<? extends CombatEntity> entities) {
                triggerEvent(WorldEvent.ENTITIES_LEAVE_COMBAT, entities);
            }

            @Override
            public void move(CombatEntity entity, int srcTick, int destTick) {
                triggerEvent(WorldEvent.ENTITY_MOVE_IN_COMBAT, entity);
            }
        });

        CombatEntities populationModel = worldModel.getPopulationModel();
        populationModel.addUpdateListener(new CombatEntity.UpdateListener() {
            @Override
            public void onChangedShortName(CombatEntity entity) {
                triggerEvent(WorldEvent.ENTITY_NAME_CHANGE, entity);
            }

            @Override
            public void onChangedColor(CombatEntity entity) {
                triggerEvent(WorldEvent.ENTITY_COLOR_CHANGE, entity);
            }

            @Override
            public void onChangedDescription(CombatEntity entity) {
                triggerEvent(WorldEvent.ENTITY_DESCRIPTION_CHANGE, entity);
            }

            @Override
            public void onChangedPreviousAction(CombatEntity entity) {
                triggerEvent(WorldEvent.ENTITY_PREV_ACTION_CHANGE, entity);
            }
        });
        populationModel.addChangeListener(new CombatEntities.ChangeListener() {
            @Override
            public void onChangedSelection(CombatEntity prevSelected, CombatEntity newSelected) {
                triggerEvent(WorldEvent.ENTITY_SELECT_CHANGE,
                        new EntitySelectChangeArgs(prevSelected, newSelected));
            }

            @Override
            public void onChangedEntities() {
                triggerEvent(WorldEvent.ENTITY_LIST_CHANGE, null);
            }
        });
    }

    /**
     * Resets the state of the combat. The entities of the population will still
     * remain but will lose any history they had (i.e.: no previous actions will
     * remain).
     */
    private void resetEntities() {
        Collection<CombatEntity> entities = worldModel.getPopulationModel().getEntities();
        List<CombatEntity> newEntities = new ArrayList<>(entities.size());
        for (CombatEntity entity: entities) {
            newEntities.add(new CombatEntity(entity));
        }

        worldModel.getPopulationModel().clear();
        worldModel.getPopulationModel().addEntities(newEntities);
    }

    private static class FrameEvent {
        public static final ExaltedEvent<Void> PATH_CHANGED
                = ExaltedEvent.Helper.createExaltedEvent(Void.class);
    }

    /**
     * The ID to be used by the AccessManager.
     */
    private static class TaskID {
        private final String name;
        private final String inProgressText;

        public TaskID(String name, String inProgressText) {
            assert name != null;
            assert inProgressText != null;

            this.name = name;
            this.inProgressText = inProgressText;
        }

        public String getInProgressText() {
            return inProgressText;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return inProgressText;
        }
    }

    private enum RootRight {
        BACKGROUND_TASK
    }

    private enum BackgrounTaskRight {
        SAVING, LOADING
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMainSplitPane = new javax.swing.JSplitPane();
        jControlPanel = new javax.swing.JPanel();
        jControlSplitPane = new javax.swing.JSplitPane();
        jTopSplitPane = new javax.swing.JSplitPane();
        jMiddleSplitPane = new javax.swing.JSplitPane();
        jMainMenu = new javax.swing.JMenuBar();
        jFileMenu = new javax.swing.JMenu();
        jNewCombatButton = new javax.swing.JMenuItem();
        jLoadCombatButton = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jSaveAsButton = new javax.swing.JMenuItem();
        jEditMenu = new javax.swing.JMenu();
        jUndoButton = new javax.swing.JMenuItem();
        jRedoButton = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);

        jMainSplitPane.setDividerLocation(400);
        jMainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jMainSplitPane.setResizeWeight(1.0);

        jControlSplitPane.setDividerLocation(250);
        jControlSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        jTopSplitPane.setDividerLocation(400);
        jTopSplitPane.setMinimumSize(new java.awt.Dimension(200, 150));
        jControlSplitPane.setLeftComponent(jTopSplitPane);

        jMiddleSplitPane.setDividerLocation(350);
        jControlSplitPane.setRightComponent(jMiddleSplitPane);

        javax.swing.GroupLayout jControlPanelLayout = new javax.swing.GroupLayout(jControlPanel);
        jControlPanel.setLayout(jControlPanelLayout);
        jControlPanelLayout.setHorizontalGroup(
            jControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jControlSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 687, Short.MAX_VALUE)
        );
        jControlPanelLayout.setVerticalGroup(
            jControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jControlSplitPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)
        );

        jMainSplitPane.setLeftComponent(jControlPanel);

        jFileMenu.setText("File");

        jNewCombatButton.setText("New Combat");
        jNewCombatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jNewCombatButtonActionPerformed(evt);
            }
        });
        jFileMenu.add(jNewCombatButton);

        jLoadCombatButton.setText("Load Combat");
        jLoadCombatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jLoadCombatButtonActionPerformed(evt);
            }
        });
        jFileMenu.add(jLoadCombatButton);
        jFileMenu.add(jSeparator1);

        jSaveAsButton.setText("Save as");
        jSaveAsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jSaveAsButtonActionPerformed(evt);
            }
        });
        jFileMenu.add(jSaveAsButton);

        jMainMenu.add(jFileMenu);

        jEditMenu.setText("Edit");

        jUndoButton.setText("Undo");
        jUndoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jUndoButtonActionPerformed(evt);
            }
        });
        jEditMenu.add(jUndoButton);

        jRedoButton.setText("Redo");
        jRedoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRedoButtonActionPerformed(evt);
            }
        });
        jEditMenu.add(jRedoButton);

        jMainMenu.add(jEditMenu);

        setJMenuBar(jMainMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jMainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 689, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jMainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jNewCombatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jNewCombatButtonActionPerformed
        saveCurrentCombat(new SaveDoneListener() {
            @Override
            public void onFailedSave(Throwable saveError) {
                ExaltedDialogHelper.displayError(TickCombatFrame.this,
                        ERROR_WHILE_SAVING_CAPTION.toString(), saveError);
            }

            @Override
            public void onSuccessfulSave(Path path) {
                setCombatPath(null);
                worldModel.getCombatModel().resetCombat();
                resetEntities();
                setAsNotModified();
            }
        });
    }//GEN-LAST:event_jNewCombatButtonActionPerformed

    private void jLoadCombatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jLoadCombatButtonActionPerformed
        saveCurrentCombat(new SaveDoneListener() {
            @Override
            public void onFailedSave(Throwable saveError) {
                ExaltedDialogHelper.displayError(TickCombatFrame.this,
                        ERROR_WHILE_SAVING_CAPTION.toString(), saveError);
            }

            @Override
            public void onSuccessfulSave(Path path) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        loadCombat();
                    }
                });
            }
        });
    }//GEN-LAST:event_jLoadCombatButtonActionPerformed

    private void jSaveAsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jSaveAsButtonActionPerformed
        final Path prevPath = currentCombatPath;
        setCombatPath(null);

        SaveDoneListener listener = new SaveDoneListener() {
            @Override
            public void onFailedSave(Throwable saveError) {
                setCombatPath(prevPath);
                if (saveError != null) {
                    ExaltedDialogHelper.displayError(TickCombatFrame.this,
                            ERROR_WHILE_SAVING_CAPTION.toString(), saveError);
                }
            }

            @Override
            public void onSuccessfulSave(Path path) {
            }
        };

        if (!saveCurrentCombat(listener)) {
            setCombatPath(prevPath);
        }
    }//GEN-LAST:event_jSaveAsButtonActionPerformed

    private void jUndoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jUndoButtonActionPerformed
        if (undoManager.canUndo()) {
            try {
                undoManager.undo();
            } catch (CannotUndoException ex) {
                undoManager.discardAllEdits();

                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Undo operation has failed.", ex);
                }
            }
        }
    }//GEN-LAST:event_jUndoButtonActionPerformed

    private void jRedoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRedoButtonActionPerformed
        if (undoManager.canRedo()) {
            try {
                undoManager.redo();
            } catch (CannotUndoException ex) {
                undoManager.discardAllEdits();

                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Redo operation has failed.", ex);
                }
            }
        }
    }//GEN-LAST:event_jRedoButtonActionPerformed

    /**
     * Opens a new instance of this {@code TickCombatFrame}. This method will
     * try to load the last used save file and load it if possible.
     * The last used save file is determined by the
     * {@link ExaltedSaveHelper#getLastUsedPath()} method call.
     * <P>
     * Note that this method will assume that once the opened frame is closed
     * the application should terminate and act accordingly: Will notify the
     * user if the user in case this application fails to terminate within a
     * reasonable time.
     * <P>
     * Also note that this method will set the Nimbus Look and Feel but this may
     * change in the future.
     *
     * @param args currently this argument is ignored
     *
     * @throws Throwable throws every uncaught exception to be caught by the
     *   JVM
     */
    public static void main(String args[]) throws Throwable {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException
                | InstantiationException
                | IllegalAccessException
                | UnsupportedLookAndFeelException ex) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to set Nimbus L&F", ex);
        }
        //</editor-fold>

        Path lastUsedPath = null;
        try {
            lastUsedPath = ExaltedSaveHelper.getLastUsedPath();
        } catch (Throwable ex) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE, "Failed to get the last used save file.", ex);
            }
        }

        final Path fileToLoad = lastUsedPath;

        /* Create and display the form */
        java.awt.EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                final TickCombatFrame frame = new TickCombatFrame();
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        // Though after the window was closed, the applciation
                        // should terminate, in case of some unexpected error
                        // Java will not terminate the EDT, so notify the
                        // user about it after 10 seconds (so the app can be
                        // forcefully closed by System.exit).
                        TooLongTerminateFrame.waitForTerminate(
                                10, 10, TimeUnit.SECONDS);
                    }
                });

                if (fileToLoad != null) {
                    frame.loadCombat(fileToLoad);
                }

                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jControlPanel;
    private javax.swing.JSplitPane jControlSplitPane;
    private javax.swing.JMenu jEditMenu;
    private javax.swing.JMenu jFileMenu;
    private javax.swing.JMenuItem jLoadCombatButton;
    private javax.swing.JMenuBar jMainMenu;
    private javax.swing.JSplitPane jMainSplitPane;
    private javax.swing.JSplitPane jMiddleSplitPane;
    private javax.swing.JMenuItem jNewCombatButton;
    private javax.swing.JMenuItem jRedoButton;
    private javax.swing.JMenuItem jSaveAsButton;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSplitPane jTopSplitPane;
    private javax.swing.JMenuItem jUndoButton;
    // End of variables declaration//GEN-END:variables
}
