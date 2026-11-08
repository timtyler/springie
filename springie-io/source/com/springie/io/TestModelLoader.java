// * World.ModelLoader for tests that cannot boot the GUI: registered in
// * this module's test META-INF/services, so World.modelLoader() discovers
// * it in any test JVM that has io's test output on its classpath (the
// * parent pom puts it on the engine tests' additional classpath, the
// * engine being unable to depend on io without closing a reactor cycle).
// * load() is a plain DataInput file load, like the production loader;
// * the point of this class is only which registration wins in a test
// * JVM (this one, via the test services file, ahead of the production
// * DataInputModelLoader) and that it exists at all where no GUI boot
// * installs one.

package com.springie.io;

import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import com.springie.world.World;

public final class TestModelLoader implements World.ModelLoader {


  public TestModelLoader() {
    // ServiceLoader instantiation
  }

  @Override
  public void load(final NodeManager manager, final String path) {
    // Real file load, exactly as the production loader does it -- the
    // earlier addFromString(CUBE_XML) shortcut fed raw SPR XML to the
    // tensegrity interpreter, which parses instruction text, not XML,
    // and silently produced an empty model.
    new DataInput(manager).loadFile(path);
  }

  @Override
  public void loadFromString(final NodeManager manager, final String data) {
    new DataInput(null).addFromString(data, manager);
  }
}
