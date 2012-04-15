package exaltedcombat.save;

import java.nio.file.Path;
import java.util.concurrent.Future;
import org.jtrim.access.task.RewTask;
import org.jtrim.access.task.RewTaskReporter;
import org.jtrim.access.task.TaskProgress;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @see ExaltedSaveHelper#createLoadRewTask(java.nio.file.Path, exaltedcombat.save.LoadDoneListener)
 *
 * @author Kelemen Attila
 */
final class LoadRewTask implements RewTask<Void, LoadRewTask.LoadResult> {
    private final LoadDoneListener doneTask;
    private final Future<SaveInfo> saveLoader;

    public LoadRewTask(Path savePath, LoadDoneListener doneTask) {
        ExceptionHelper.checkNotNullArgument(savePath, "savePath");
        ExceptionHelper.checkNotNullArgument(doneTask, "doneTask");

        this.doneTask = doneTask;
        this.saveLoader = ExaltedSaveHelper.getSaveInfoProvider(savePath);
    }

    @Override
    public Void readInput() {
        return null;
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public LoadResult evaluate(Void input, RewTaskReporter reporter) throws InterruptedException {
        Throwable saveError = null;
        SaveInfo saveInfo = null;
        try {
            saveInfo = saveLoader.get();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Throwable ex) {
            saveError = ex;
        }
        return new LoadResult(saveInfo, saveError);
    }

    @Override
    public void writeOutput(LoadResult output) {
        Throwable error = output.getLoadError();
        if (error != null) {
            doneTask.onFailedLoad(error);
        }
        else {
            doneTask.onSuccessfulLoad(output.getSaveInfo());
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
        saveLoader.cancel(true);
    }

    static final class LoadResult {
        private final SaveInfo saveInfo;
        private final Throwable loadError;

        public LoadResult(SaveInfo saveInfo, Throwable loadError) {
            this.saveInfo = saveInfo;
            this.loadError = loadError;
        }

        public Throwable getLoadError() {
            return loadError;
        }

        public SaveInfo getSaveInfo() {
            return saveInfo;
        }
    }
}
