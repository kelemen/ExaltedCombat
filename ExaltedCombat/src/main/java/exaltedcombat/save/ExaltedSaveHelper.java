package exaltedcombat.save;

import exaltedcombat.models.CombatModel;
import exaltedcombat.models.CombatPositionModel;
import exaltedcombat.models.CombatState;
import exaltedcombat.models.impl.CombatEntity;
import exaltedcombat.models.impl.CombatEntityWorldModel;
import exaltedcombat.utils.CancelableDeserializer;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim.access.task.RewTask;
import org.jtrim.utils.ExceptionHelper;

/**
 * Contains static helper methods and fields to save and load a state of
 * ExaltedCombat. The provided methods are safe to call from any thread and may
 * be called concurrently unless otherwise noted. However, methods are not
 * required to transparent to their synchronization.
 * <P>
 * This class cannot be instantiated.
 *
 * @author Kelemen Attila
 */
public final class ExaltedSaveHelper {
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * The file extension of the save files of ExaltedCombat. This string also
     * contains the extension separator '.' character.
     */
    public static final String SAVEFILE_EXTENSION = ".ec";

    /**
     * The directory where the save files of ExaltedCombat are being stored.
     * This field is never {@code null}.
     */
    public static final Path SAVE_HOME = Paths.get(System.getProperty("user.home"), "ExaltedCombat");

    private static final Path LAST_USED_SOURCE = SAVE_HOME.resolve("lastused");

    /**
     * Creates a serializable object storing the state of ExaltedCombat.
     * The returned object is intended to be backward compatible with regards to
     * serialization, so it can be stored in a file to be retrieved later.
     * <P>
     * Note that this method needs to access the world model of ExaltedCombat,
     * therefore this method should only be called from the AWT event
     * dispatching thread.
     *
     * @param worldModel the model containing the population and the combat.
     *   This argument cannot be {@code null}.
     * @param hiddenEntities the entities not currently part of the population
     *   but needed to be saved anyway. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements but can be an empty
     *   {@code Collection}.
     * @return a serializable object storing the state of ExaltedCombat. This
     *   method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null} or {@code hiddenEntities} contains {@code null} elements
     */
    public static SaveInfo createSaveInfo(CombatEntityWorldModel worldModel,
            Collection<? extends CombatEntity> hiddenEntities) {

        // Note that every objects defined to be non-null will be dereferenced
        // sooner or later and since this method has no side effect there is
        // no reason to check for null values early.

        CombatModel<CombatEntity> combatModel = worldModel.getCombatModel();

        List<SavedEntityInfo> savedHiddens = new ArrayList<>(hiddenEntities.size());
        for (CombatEntity entity: hiddenEntities) {
            savedHiddens.add(new SavedEntityInfo(entity));
        }

        List<SavedActiveEntityInfo> savedReadies = new LinkedList<>();
        List<SavedCombatEntity> savedFighters = new LinkedList<>();

        CombatPositionModel<CombatEntity> positionModel = combatModel.getPositionModel();
        for (CombatEntity entity: worldModel.getPopulationModel().getEntities()) {
            SavedActiveEntityInfo baseInfo = new SavedActiveEntityInfo(entity);

            int tick = positionModel.getTickOfEntity(entity);
            if (tick >= 0) {
                savedFighters.add(new SavedCombatEntity(
                        baseInfo,
                        tick,
                        combatModel.getPreJoinRoll(entity)));
            }
            else {
                savedReadies.add(baseInfo);
            }
        }

        SaveInfo result = new SaveInfo(combatModel.getCombatState() != CombatState.JOIN_PHASE);
        result.addHiddenEntities(savedHiddens);
        result.addReadyEntities(savedReadies);
        result.addCombatEntities(savedFighters);
        return result;
    }

    /**
     * Saves the specified state of ExaltedCombat into the specified file.
     * This methods blocks and waits until the file was actually saved.
     * <P>
     * Saved states can later be retrieved by a task returned by the
     * {@link #createLoadRewTask(Path, LoadDoneListener)} method.
     *
     * @param path the path to the file where the state of ExaltedCombat must
     *   be saved to. This argument cannot be {@code null}.
     * @param saveInfo the state of ExaltedCombat to be saved. This argument
     *   cannot be {@code null}.
     * @param allowOverwrite if {@code true} and the specified file exists, its
     *   content will be overwritten. If {@code false} trying to overwrite an
     *   already existing file will cause an {@code IOException} to be thrown.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IOException thrown if there was an error while trying to read the
     *   specified file (e.g.: it does not exists)
     */
    public static void doSave(Path path, SaveInfo saveInfo, boolean allowOverwrite) throws IOException {
        ExceptionHelper.checkNotNullArgument(saveInfo, "saveInfo");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream fileOutput = allowOverwrite
                ? Files.newOutputStream(path, CREATE, TRUNCATE_EXISTING)
                : Files.newOutputStream(path, CREATE_NEW)) {

            // This stream does not need to be closed because only
            // the file stream holds unmanaged resource.
            ObjectOutputStream output = new ObjectOutputStream(fileOutput);
            output.writeObject(saveInfo);
            output.flush();
        }
    }

    /**
     * Creates a {@link RewTask REW} (Read, Evaluate, Write) task which when
     * executed will save a specified state of ExaltedCombat given a name of a
     * combat. The file storing the state will be saved to the
     * {@link #SAVE_HOME} directory. The completion of the task can be detected
     * by a user specified listener.
     * <P>
     * The task will notify the listener in the context of its
     * {@link org.jtrim.access.AccessToken write token}.
     * <P>
     * Saved states can later be retrieved by a task returned by the
     * {@link #createLoadRewTask(Path, LoadDoneListener)} method.
     *
     * @param combatName the name of the combat to be saved. This argument will
     *   be used as a part of the file storing the state to be saved. Therefore
     *   it must contain characters valid to use in filenames. This argument
     *   cannot be {@code null}.
     * @param saveInfo the state of ExaltedCombat to be saved. This argument
     *   cannot be {@code null}.
     * @param mayOverwrite specify {@code true} if the task may overwrite
     *   existing file when saving the state. If {@code false} is specified for
     *   this argument and the file intended to be used by the returned task
     *   to store the state of ExaltedCombat exists, the task will fail.
     * @param doneListener the listener to be notified when the returned task
     *   had been executed and has terminated. This argument cannot be
     *   {@code null}
     * @return the {@link RewTask REW} (Read, Evaluate, Write) task which when
     *   executed will save a specified state of ExaltedCombat. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if {@code combatName}
     *   or {@code saveInfo} or {@code doneListener} is {@code null}
     *
     * @see IdempotentSaveListener
     */
    public static RewTask<?, ?> createSaveRewTask(String combatName,
            SaveInfo saveInfo,
            boolean mayOverwrite,
            SaveDoneListener doneListener) {
        return new SaveRewTask(combatName, saveInfo, mayOverwrite, doneListener);
    }

    /**
     * Creates a {@link RewTask REW} (Read, Evaluate, Write) task which when
     * executed will save a specified state of ExaltedCombat to a given file.
     * The completion of the task can be detected by a user specified listener.
     * <P>
     * The task will notify the listener in the context of its
     * {@link org.jtrim.access.AccessToken write token}.
     * <P>
     * Saved states can later be retrieved by a task returned by the
     * {@link #createLoadRewTask(Path, LoadDoneListener)} method.
     *
     * @param savePath the file where the specified saved state is to be saved.
     *   This argument cannot be {@code null}.
     * @param saveInfo the state of ExaltedCombat to be saved. This argument
     *   cannot be {@code null}.
     * @param mayOverwrite specify {@code true} if the task may overwrite
     *   the file pointed to by {@code savePath} when saving the state. If
     *   {@code false} is specified for this argument and the file pointed to
     *   by {@code savePath} exists, the task will fail.
     * @param doneListener the listener to be notified when the returned task
     *   had been executed and has terminated. This argument cannot be
     *   {@code null}
     * @return the {@link RewTask REW} (Read, Evaluate, Write) task which when
     *   executed will save a specified state of ExaltedCombat. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if {@code savePath}
     *   or {@code saveInfo} or {@code doneListener} is {@code null}
     *
     * @see IdempotentSaveListener
     */
    public static RewTask<?, ?> createSaveRewTask(Path savePath,
            SaveInfo saveInfo,
            boolean mayOverwrite,
            SaveDoneListener doneListener) {
        return new SaveRewTask(savePath, saveInfo, mayOverwrite, doneListener);
    }

    /**
     * Creates a {@link RewTask REW} (Read, Evaluate, Write) task which when
     * executed will load a saved state of ExaltedCombat from a given file.
     * The saved state will be forwarded to the user specified listener.
     * <P>
     * The task will notify the listener in the context of its
     * {@link org.jtrim.access.AccessToken write token}.
     *
     * @param savePath the file from which the state of ExaltedCombat is to be
     *   retrieved. This argument cannot be {@code null}.
     * @param taskOnSuccess the listener to be notified if the saved state was
     *   successfully loaded. This argument cannot be {@code null}.
     *
     * @return the {@link RewTask REW} (Read, Evaluate, Write) task which when
     *   executed will load a saved state of ExaltedCombat. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if {@code savePath} or
     *   {@code taskOnSuccess} is {@code null}.
     */
    public static RewTask<?, ?> createLoadRewTask(
            Path savePath,
            LoadDoneListener taskOnSuccess) {
        return new LoadRewTask(savePath, taskOnSuccess);
    }

    /**
     * Reads the first non-empty line from a file.
     *
     * @param file the file from which the line is to be read. This argument
     *   cannot be {@code null}.
     * @return the first non-empty line in the given file or {@code null} if
     *   there is no such line in the specified file
     *
     * @throws IOException thrown if there was an error while reading the
     *   specified file (e.g.: it does not exist)
     */
    private static String readFirstLine(Path file) throws IOException {
        try (InputStream input = Files.newInputStream(file)) {
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(input, DEFAULT_CHARSET));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    return line;
                }
                line = reader.readLine();
            }
        }

        return null;
    }

    private static void writeSingleLine(Path file, String line) throws IOException {
        try (OutputStream output = Files.newOutputStream(file, CREATE, TRUNCATE_EXISTING)) {
            new OutputStreamWriter(output, DEFAULT_CHARSET).append(line).flush();
        }
    }

    static Future<SaveInfo> getSaveInfoProvider(Path file) {
        final Future<Object> result = new CancelableDeserializer(file);

        return new Future<SaveInfo>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return result.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return result.isCancelled();
            }

            @Override
            public boolean isDone() {
                return result.isDone();
            }

            private SaveInfo cast(Object saveInfo) throws ExecutionException {
                if (saveInfo instanceof SaveInfo) {
                    return (SaveInfo)saveInfo;
                }
                else {
                    throw new ExecutionException(new IOException("Invalid save file."));
                }
            }

            @Override
            public SaveInfo get() throws InterruptedException, ExecutionException {
                return cast(result.get());
            }

            @Override
            public SaveInfo get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return cast(result.get(timeout, unit));
            }
        };
    }

    /**
     * Stores path to a filename which can be later retrieved by a
     * {@link #getLastUsedPath() getLastUsedPath()} method call.
     * This method is intended to be used to store the last loaded
     * save file, so when restarting ExaltedCombat, it can be automatically
     * reloaded on startup.
     * <P>
     * This method currently stores the filename information in a file in the
     * directory specified by {@link #SAVE_HOME}.
     *
     * @param file the file path to the last used save file. This argument can
     *   be {@code null} which is interpreted as no save file was used
     *   previously.
     *
     * @throws IOException thrown if there was an error while trying to save
     *   the specified file path
     */
    public static void storeLastUsedPath(Path file) throws IOException {
        writeSingleLine(LAST_USED_SOURCE, file != null ? file.toString() : "");
    }

    /**
     * Retrieves a file path previously saved by a
     * {@link #storeLastUsedPath(Path) storeLastUsedPath(Path)} method call.
     * <P>
     * The returned path is intended to be used to automatically load the saved
     * state of ExaltedCombat on startup.
     *
     * @return the path to the last used save file of ExaltedCombat or
     *   {@code null} if there was no such file
     *
     * @throws IOException thrown if there was an error while trying to read
     *   the last used save filename. Note that normally this method does not
     *   throw an exception if {@link #storeLastUsedPath(Path) storeLastUsedPath(Path)}
     *   was not called before instead in this case it will return {@code null}.
     */
    public static Path getLastUsedPath() throws IOException {
        if (!Files.exists(LAST_USED_SOURCE)) {
            return null;
        }

        String line = readFirstLine(LAST_USED_SOURCE);
        return line != null ? Paths.get(line) : null;
    }

    private ExaltedSaveHelper() {
        throw new AssertionError();
    }
}
