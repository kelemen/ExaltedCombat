/**
 * Contains classes and methods to allow ExaltedCombat to save its state to
 * a file.
 * <P>
 * Saving to file is done through the serialization mechanism of Java,
 * therefore it is extremely important to maintain backward compatible
 * serialization for instance actually saved. That is: <B>Unless there is a very
 * good reason to do otherwise, use a serialization proxy</B>.
 * <P>
 * Note that currently the only classes to be actually saved are in this package
 * except for the actions (stored in the {@code exaltedcombat.actions} package).
 */
package exaltedcombat.save;
