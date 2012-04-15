package exaltedcombat.save;

import java.io.IOException;
import java.nio.file.Path;
import org.jtrim.access.task.RewTask;
import org.jtrim.access.task.RewTaskReporter;
import org.jtrim.access.task.TaskProgress;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @see ExaltedSaveHelper#createSaveRewTask(java.nio.file.Path, exaltedcombat.save.SaveInfo, boolean, exaltedcombat.save.SaveDoneListener)
 * @see ExaltedSaveHelper#createSaveRewTask(java.lang.String, exaltedcombat.save.SaveInfo, boolean, exaltedcombat.save.SaveDoneListener)
 *
 * @author Kelemen Attila
 */
final class SaveRewTask implements RewTask<Void, SaveRewTask.SaveResult> {
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

    @Override
    public Void readInput() {
        return null;
    }

    private Path doSave() throws IOException {
        Path fileName = savePath != null
                ? savePath
                : ExaltedSaveHelper.SAVE_HOME.resolve(combatName + ExaltedSaveHelper.SAVEFILE_EXTENSION);
        ExaltedSaveHelper.doSave(fileName, saveInfo, mayOverwrite);
        return fileName;
    }

    @Override
    public SaveResult evaluate(Void input, RewTaskReporter reporter) throws InterruptedException {
        Throwable saveError = null;
        Path path = null;
        try {
            path = doSave();
        } catch (Throwable ex) {
            saveError = ex;
        }
        return new SaveResult(path, saveError);
    }

    @Override
    public void writeOutput(SaveResult output) {
        Throwable saveError = output.getSaveError();
        if (saveError != null) {
            doneListener.onFailedSave(saveError);
        }
        else {
            doneListener.onSuccessfulSave(output.getSavePath());
        }
    }

    @Override
    public void writeProgress(TaskProgress<?> progress) {
    }

    @Override
    public void writeData(Object data) {
    }

    @Override
    public void cancel() {
    }

    static final class SaveResult {
        private final Path savePath;
        private final Throwable saveError;

        public SaveResult(Path savePath, Throwable saveError) {
            this.savePath = savePath;
            this.saveError = saveError;
        }

        public Throwable getSaveError() {
            return saveError;
        }

        public Path getSavePath() {
            return savePath;
        }
    }
}
