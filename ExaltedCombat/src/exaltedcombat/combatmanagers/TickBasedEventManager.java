package exaltedcombat.combatmanagers;

import exaltedcombat.models.CombatPosEventListener;
import exaltedcombat.models.CombatPositionModel;
import java.util.*;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.EventHandlerContainer;
import org.jtrim.event.LifoEventHandlerContainer;
import org.jtrim.utils.ExceptionHelper;

/**
 * This class maintains the position of tick based combat entities on a zero
 * based timeline.
 * <P>
 * Entities on the timeline has a certain tick value. This tick value defines
 * on which tick will they act. It is possible that on a certain tick there are
 * multiple entities in this case these entities acts simultaneously.
 * <P>
 * The current time is defined by the entity with the lowest tick value. If
 * there are no entities defined, the time is the last time when there were
 * entities defined. In case the event manager never had an entity defined the
 * current time is defined to be zero (i.e.: the starting time is zero).
 * <P>
 * Note that instances of this class are not thread-safe and not transparent
 * to synchronization. Therefore it is not possible to use instances of this
 * class by multiple threads concurrently even if they are synchronized with
 * locks and similar blocking synchronization controls. In fact trying to hold
 * a lock while a method of this class is being invoked is prone to dead-lock.
 * In case instances of this class need to be used by multiple threads
 * concurrently, more advanced non-blocking technics must be used such as
 * provided by {@link org.jtrim.concurrent.TaskScheduler}. This limitation is
 * the cause of event listeners being invoked from methods of this class and
 * these event listeners must not be assumed to be synchronization transparent.
 * <B>Failing to heed the above warning is likely to cause dead-locks.</B>
 *
 * @param <EntityType> the type of an entities participating in the combat
 *
 * @author Kelemen Attila
 */
public final class TickBasedEventManager<EntityType>
implements
        CombatPositionModel<EntityType> {

    private final Map<EntityType, Integer> timeTable;
    private final SortedMap<Integer, List<EntityType>> ticks;

    private final EventHandlerContainer<CombatPosEventListener<EntityType>> listeners;

    private int currentTick;

    /**
     * Creates a new event manager with no entities on the timeline. The current
     * time of the newly created event manager is zero.
     */
    public TickBasedEventManager() {
        this.timeTable = new HashMap<>();
        this.ticks = new TreeMap<>();
        this.listeners = new LifoEventHandlerContainer<>();
        this.currentTick = 0;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addCombatPosListener(CombatPosEventListener<EntityType> listener) {
        listeners.registerListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeCombatPosListener(CombatPosEventListener<EntityType> listener) {
        listeners.removeListener(listener);
    }

    private void invalidateCurrentTick() {
        recalculateCurrentTick();
    }

    private int recalculateCurrentTick() {
        int result;

        if (ticks.isEmpty()) {
            return currentTick;
        }
        else {
            Integer firstEntity = ticks.firstKey();
            result = firstEntity != null ? firstEntity.intValue() : 0;
        }

        currentTick = result;
        return result;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Map<Integer, List<EntityType>> getEntities() {
        return new HashMap<>(ticks);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<EntityType> getEntities(int tick) {
        ExceptionHelper.checkArgumentInRange(tick, 0, Integer.MAX_VALUE, "tick");

        List<EntityType> result = ticks.get(tick);
        return result != null ? result : Collections.<EntityType>emptyList();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterable<List<EntityType>> getTicks(int startTick) {
        return new TickIterable(startTick);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Iterable<List<EntityType>> getTicks() {
        return getTicks(getCurrentTick());
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getCurrentTick() {
        return currentTick;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getTickOfEntity(EntityType entity) {
        Integer tick = timeTable.get(entity);
        return tick != null ? tick.intValue() : -1;
    }

    private void dispatchMoveEvent(
            final EntityType id,
            final int fromTick,
            final int toTick) {

        if (fromTick == toTick) {
            return;
        }

        if (fromTick >= 0) {
            listeners.onEvent(new EventDispatcher<CombatPosEventListener<EntityType>>() {
                @Override
                public void onEvent(CombatPosEventListener<EntityType> eventListener) {
                    eventListener.move(id, fromTick, toTick);
                }
            });
        }
        else {
            listeners.onEvent(new EventDispatcher<CombatPosEventListener<EntityType>>() {
                @Override
                public void onEvent(CombatPosEventListener<EntityType> eventListener) {
                    eventListener.enterCombat(id, toTick);
                }
            });
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int moveToTick(EntityType entity, int tick) {
        ExceptionHelper.checkArgumentInRange(tick, 0, Integer.MAX_VALUE, "tick");

        int prevTick = removeEntityWithoutEvent(entity);
        addEntity(entity, tick);
        dispatchMoveEvent(entity, prevTick, tick);

        return prevTick;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getNumberOfEntities() {
        return timeTable.size();
    }

    private void dispatchRemoveEvent(final Collection<? extends EntityType> entities) {
        listeners.onEvent(new EventDispatcher<CombatPosEventListener<EntityType>>() {
            @Override
            public void onEvent(CombatPosEventListener<EntityType> eventListener) {
                eventListener.leaveCombat(entities);
            }
        });
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeAllEntities() {
        List<EntityType> removedEntities = new ArrayList<>(getNumberOfEntities());
        for (Collection<EntityType> entities: ticks.values()) {
            removedEntities.addAll(entities);
        }

        timeTable.clear();
        ticks.clear();
        invalidateCurrentTick();

        dispatchRemoveEvent(removedEntities);
    }

    private int removeEntityWithoutEvent(EntityType id) {
        Integer tickKey = timeTable.remove(id);
        if (tickKey != null) {
            int tick = tickKey;

            List<EntityType> entities = ticks.get(tick);

            // If it was in the timeTable it must be in the "ticks" exactly
            // once.
            assert entities != null;
            assert !entities.isEmpty();

            if (entities.size() == 1) {
                assert Objects.equals(entities.get(0), id);
                ticks.remove(tick);
            }
            else {
                List<EntityType> newEntities = new ArrayList<>(entities.size() - 1);
                for (EntityType entity: entities) {
                    if (!Objects.equals(entity, id)) {
                        newEntities.add(entity);
                    }
                }
                ticks.put(tick, newEntities);
            }

            invalidateCurrentTick();
            return tick;
        }
        else {
            return -1;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int removeEntity(EntityType entity) {
        int prevTick = removeEntityWithoutEvent(entity);

        if (prevTick >= 0) {
            dispatchRemoveEvent(Collections.singleton(entity));
        }

        return prevTick;
    }

    private void addEntity(EntityType id, int tick) {
        Integer prevTick = timeTable.put(id, tick);
        assert prevTick == null;

        List<EntityType> entities = ticks.get(tick);
        if (entities == null) {
            ticks.put(tick, Collections.singletonList(id));
        }
        else {
            List<EntityType> newEntities = new ArrayList<>(entities.size() + 1);
            newEntities.addAll(entities);
            newEntities.add(id);
            ticks.put(tick, Collections.unmodifiableList(newEntities));
        }

        invalidateCurrentTick();
    }

    private class TickIterator implements Iterator<List<EntityType>> {
        private int currentTick;
        private boolean canRemove;

        public TickIterator(int startTick) {
            assert startTick >= 0;
            this.currentTick = startTick;
            this.canRemove = false;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        private boolean isOverflowed() {
            // If the currentTick was increased beyond Integer.MAX_VALUE
            // it will overflow to a negative value.
            // Note that no entities can be at ticks over Integer.MAX_VALUE.
            return currentTick < 0;
        }

        @Override
        public List<EntityType> next() {
            if (isOverflowed()) {
                canRemove = true;
                return Collections.<EntityType>emptyList();
            }

            List<EntityType> result = ticks.get(currentTick);
            currentTick++;
            canRemove = true;

            return result != null ? result : Collections.<EntityType>emptyList();
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException("next() was not called yet");
            }

            canRemove = false;
            if (!isOverflowed()) {
                ticks.remove(currentTick - 1);
            }
        }
    }

    private class TickIterable implements Iterable<List<EntityType>> {
        private final int startTick;

        public TickIterable(int startTick) {
            ExceptionHelper.checkArgumentInRange(startTick, 0, Integer.MAX_VALUE, "startTick");
            this.startTick = startTick;
        }

        @Override
        public Iterator<List<EntityType>> iterator() {
            return new TickIterator(startTick);
        }
    }
}
