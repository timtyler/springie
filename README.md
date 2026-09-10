# Springie

This is a tensegrity simulator. For details, see:

http://springie.com/

This distribution contains Springie's source code.

This file is intended to explain what's where in the archive.

The index.html file runs the jar file.

The main Java source file can be found at: com/springie/FrEnd.java

Switches at the start of this control how the whole program behaves.

To build, use:
mvn clean compile

To test, use:
mvn clean test

To package, use:
mvn clean package

To run, use:
java -jar target/springie-1.02-beta-5-SNAPSHOT.jar

# ToDo
 * Add "temperature" control
 * Add "countdown-timer" concept
   * Add "disease" concept
   * Add "immunity" concept
 * Add "bounce elasticity" controls
 * Add "friction" controls?
 * Adapt to make 2D simulations more accessible
 * Speed up if no selection
 
Enjoy,
--
__________
 |im |yler  http://timtyler.org/
 