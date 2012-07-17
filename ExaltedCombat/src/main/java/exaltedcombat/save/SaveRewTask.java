package exaltedcombat.save;

import java.io.IOException;
import java.nio.file.Path;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.swing.concurrent.BackgroundTask;
import org.jtrim.swing.concurrent.SwingReporter;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @see ExaltedSaveHelper#createSaveRewTask(java.nio.file.Path, exaltedcombat.save.SaveInfo, boolean, exaltedcombat.save.SaveDoneListener)
 * @see ExaltedSaveHelper#createSaveRewTask(java.lang.String, exaltedcombat.save.SaveInfo, boolean, exaltedcombat.save.SaveDoneListener)
 *
 * @author Kelemen Attila
 */
final class SaveRewTask implements BackgroundTask {
    private final String combatName;
    private final Path savePath;
    private final SaveInfo saveInfo;
    private final boolean mayOverwrite;
    private final SaveDoneListener doneListener;

    public SaveRewTask(
            String combatName,
            SaveInfo saveInfo,
            boolean mayOverwrite,
            SaveDoneListener doneListener) {
        this(combatName, null, saveInfo, mayOverwrite, doneListener);
        ExceptionHelper.checkNotNullArgument(combatName, "combatName");
    }

    public SaveRewTask(
            Path savePath,
            SaveInfo saveInfo,
            boolean mayOverwrite,
            SaveDoneListener doneListener) {
        this(null, savePath, saveInfo, mayOverwrite, doneListener);
        ExceptionHelper.checkNotNullArgument(savePath, "savePath");
    }

    private SaveRewTask(
            String combatName,
            Path savePath,
            SaveInfo saveInfo,
            boolean mayOverwrite,
            SaveDoneListener doneListener) {
        ExceptionHelper.checkNotNullArgument(saveInfo, "saveInfo");
        ExceptionHelper.checkNotNullArgument(doneListener, "doneListener");

        this.combatName = combatName;
        this.savePath = savePath;
        this.saveInfo = saveInfo;
        this.mayOverwrite = mayOverwrite;
        this.doneListener = doneListener;
    }

    private Path doSave() throws IOException {
        Path fileName = savePath != null
                ? savePath
                : ExaltedSaveHelper.SAVE_HOME.resolve(combatName + ExaltedSaveHelper.SAVEFILE_EXTENSION);
        ExaltedSaveHelper.doSave(fileName, saveInfo, mayOverwrite);
        return fileName;
    }

    @Override
    public void execute(CancellationToken cancelToken, SwingReporter reporter) {
        Throwable saveError = null;
        Path path = null;
        try {
            path = doSave();
        } catch (Throwable ex) {
            saveError = ex;
        }
        writeOutput(reporter, path, saveError);
    }

    public void writeOutput(SwingReporter reporter, final Path savePath, final Throwable saveError) {
        reporter.writeData(new Runnable() {
            @Override
            public void run() {
                if (saveError != null) {
                    doneListener.onFailedSave(saveError);
                }
                else {
                    doneListener.onSuccessfulSave(savePath);
                }
            }
        });
    }
}
