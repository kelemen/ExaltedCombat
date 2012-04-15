package exaltedcombat.save;

import exaltedcombat.models.impl.CombatEntity;
import java.awt.Color;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a serializable class storing the basic properties of an entity.
 * The history of actions of an entity is not stored but every other property
 * of a {@link CombatEntity CombatEntity} instance is stored.
 * <P>
 * Note that this class is completely immutable (assuming that the
 * {@code Color} instance is immutable) so instances of this class are safe to
 * be shared across multiple threads concurrently.
 * <P>
 * The serialization of this class is intended to be kept backward compatible,
 * so the serialized form of this class is appropriate for long term storage.
 *
 * @see CombatEntity
 * @author Kelemen Attila
 */
public final class SavedEntityInfo implements Serializable {
    private static final long serialVersionUID = -8217680551547844350L;

    private final String shortName;
    private final Color color;
    private final String description;

    /**
     * Creates serializable instance storing the properties of an entity except
     * for its history of actions.
     *
     * @param entity the entity whose properties are to be copied. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public SavedEntityInfo(CombatEntity entity) {
        this(entity.getShortName(), entity.getColor(), entity.getDescription());
    }

    /**
     * Creates serializable instance storing the
     * {@link CombatEntity#getShortName() short name}, the
     * {@link CombatEntity#getColor() color} and the
     * {@link CombatEntity#getDescription() description} of an entity.
     *
     * @param shortName the short name of the entity which can be retrieved by a
     *   {@link #getShortName() getShortName()} call. This argument cannot be
     *   {@code null}.
     * @param color the color of the entity which can be retrieved by a
     *   {@link #getColor() getColor()} call. This argument cannot be
     *   {@code null}.
     * @param description the description of the entity which can be retrieved
     *   by a {@link #getDescription() getDescription()} call. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public SavedEntityInfo(String shortName, Color color, String description) {
        ExceptionHelper.checkNotNullArgument(shortName, "shortName");
        ExceptionHelper.checkNotNullArgument(color, "color");
        ExceptionHelper.checkNotNullArgument(description, "description");

        this.shortName = shortName;
        this.color = color;
        this.description = description;
    }

    /**
     * Creates a new unique combat entity with an empty history of actions.
     * Every call to this method returns a new different entity with the
     * same properties.
     * <P>
     * The returned instance is not used by this {@link SavedEntityInfo}
     * instance and can be used by the client as it wishes.
     *
     * @return a new unique combat entity with an empty history of actions.
     *   This method never returns {@code null}.
     */
    public CombatEntity toCombatEntity() {
        return new CombatEntity(shortName, color, description);
    }

    /**
     * Returns the saved {@link CombatEntity#getColor() color} of an entity.
     *
     * @return the saved color of an entity. This method never returns
     *   {@code null}.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Returns the saved {@link CombatEntity#getDescription() description} of an
     * entity.
     *
     * @return the saved description of an entity. This method never returns
     *   {@code null}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the saved {@link CombatEntity#getShortName() short name} of an
     * entity.
     *
     * @return the saved short name of an entity. This method never returns
     *   {@code null}.
     */
    public String getShortName() {
        return shortName;
    }

    private Object writeReplace() {
        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = -4238814486483415943L;

        private final String shortName;
        private final Color color;
        private final String description;

        public Format1(SavedEntityInfo info) {
            this.shortName = info.shortName;
            this.color = info.color;
            this.description = info.description;
        }

        private Object readResolve() throws InvalidObjectException {
            return new SavedEntityInfo(shortName, color, description);
        }
    }
}
