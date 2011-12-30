Developing ExaltedCombat
========================

Used JDK
--------
ExaltedCombat needs **JDK 7** to compile and **JRE 7** to run.

Supported IDEs
--------------
Currently there are three possible IDEs being directly supported:

 - Eclipse: Test files are not currently supported because I don't know how to 
   specify them to Eclipse. Not that there are many tests.

 - IntelliJ IDEA

 - **NetBeans**: Should be prefered over other IDEs because many Swing panels
   were created using the GUI builder of NetBeans (formerly known as Matisse).
   Other than this other IDEs are fine to use.

GUI builder
-----------
For most of the GUIs (where it is convenient), the GUI builder of NetBeans is
being used. Therefore the blocks autogenerated by this GUI builder must not be
edited manually. These autogenerated blocks can be recognised by the comments
the GUI builder surrounds them surrounds them.

Dependencies
------------
Dependencies of ExaltedCombat can be found in the *ExaltedCombat/lib* 
directory.

Currently the only dependecy of ExaltedCombat is the *JTrim* library. This
library is a work in progress and not currently released. However for
convenience and for being able to generate the javadoc, the source can also
be found in the above mentioned *lib* folder.

How to start?
-------------
The first place to look for information about the code is the api 
documentation of the `exaltedcombat` package found in 
*ExaltedCombat/src/exaltedcombat/package-info.java*. Also it is possible
to generate javadoc for the ExaltedCombat project and view it in convenient
html format.
