Java-Gravity-Simulator
======================

A gravity simulator inspired by (this)[http://nowykurier.com/toys/gravity/gravity.html]

Controls:

- Left click to place a particle.
- Right click near a particle to select it.
- Middle click and drag to move the viewport.
- Hold shift while placing a particle to make that particle static.
- There is a menu in the left right corner that show the currently selected mouse wheel behaviour.

Keyboard shortcuts:

- S: invert selected particle's static flag.
- V: change selected particle's velocity, click or press v again to finalize.
- M: move selected particle, click or press g again to finalize.
- F: freeze time.
- C: clear all particles.
- 1-9: select mouse wheel menu 1-9.
- 0: select mouse wheel menu 10.
- +: speed up time.
- -: slow down time. **WARNING** _this seems to mess everything up if it goes below .25x speed_
- Escape: cancel velocity/position editing.
- Backspace: delete selected particle.

If you want to export this as a runnable jar, (this)[http://thecodinguniverse.com/lwjgl-exporting-as-jar/] works;

This doesn't include the .classpath file, because it is platform specific, so here is how to set yours up.

Eclipse:
1. Right click the project in the Package Explorer
2. Click on Properties
3. Select Java Build path
4. Click Add JARS...
5. Go to <Project name> -> lib -> jars -> lwjgl.jar
6. Click Add JARS... again
7. Go to <Project name> -> lib -> slick.jar

Now we are going to add the natives
8. Open up the lwjgl.jar entry
9. Select Native Library Location
10. Click Edit...
11. Click Workspace...
12. Select <Project name> -> lib -> native -> <Your OS>
13. Click OK until you get out of the Properties window

You are done!

Others:
  no idea
