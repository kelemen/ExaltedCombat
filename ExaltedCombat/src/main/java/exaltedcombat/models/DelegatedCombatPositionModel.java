package exaltedcombat.models;

import java.util.List;
import java.util.Map;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Forwards all its methods call to a wrapped {@link CombatPositionModel}.
 * This class has two intended purposes:
 * <ul>
 *  <li>
 *   In case a simple modification is required to a {@code CombatPositionModel}
 *   implementation, instead of subclassing it: Subclass this class instead,
 *   wrap an instance of the used {@code CombatPositionModel} and modify it
 *   by overriding only the needed methods. Adding new functionality
 *   to a {@code CombatPositionModel} is more clearer and less prone to unwanted
 *   side-effects this way.
 *  </li>
 *  <li>
 *   If you need to share a {@code CombatPositionModel} which implements other
 *   interfaces or provides other public methods, the returned value can be
 *   wrapped by this class, so clients can only access methods derived from the
 *   {@code CombatPositionModel}.
 *  </li>
 * </ul>
 * <P>
 * Note that the {@link #wrapped wrapped model} is a protected field and can be
 * accessed from subclasses directly.
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @param <EntityType> the type of the combat entities in this model
 *
 * @author Kelemen Attila
 */
public class DelegatedCombatPositionModel<EntityType>
implements
        CombatPositionModel<EntityType> {

    /**
     * The wrapped model specified at construction time. This class will forward
     * all the method calls of the {@code CombatPositionModel} interface to this
     * model by default.
     */
    protected final CombatPositionModel<EntityType> wrapped;

    /**
     * Creates a model delegating its calls to the specified model.
     *
     * @param wrapped the model to which calls will be delegated to by default.
     *   This argument cannot be {@code null} and can be accessed from the
     *   {@link #wrapped wrapped} field from subclasses.
     */
    public DelegatedCombatPositionModel(CombatPositionModel<EntityType> wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

        this.wrapped = wrapped;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef addCombatPosListener(CombatPosEventListener<EntityType> listener) {
        return wrapped.addCombatPosListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getCurrentTick() {
        return wrapped.getCurrentTick();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Map<Integer, List<EntityType>> getEntities() {
        return wrapped.getEntities();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<EntityType> getEntities(int tick) {
        return wrapped.getEntities(tick);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getNumberOfEntities() {
        return wrapped.getNumberOfEntities();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getTickOfEntity(EntityType entity) {
        return wrapped.getTickOfEntity(entity);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterable<List<EntityType>> getTicks(int startTick) {
        return wrapped.getTicks(startTick);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterable<List<EntityType>> getTicks() {
        return wrapped.getTicks();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int moveToTick(EntityType entity, int tick) {
        return wrapped.moveToTick(entity, tick);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeAllEntities() {
        wrapped.removeAllEntities();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int removeEntity(EntityType entity) {
        return wrapped.removeEntity(entity);
    }
}
