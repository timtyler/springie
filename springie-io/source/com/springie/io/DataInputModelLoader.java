// * The io module's World.ModelLoader: the bridge the engine asks for.
//
// * World.model_loader is a static the engine dereferences but cannot
// * implement (the io module sits downstream of it). SetUpCode used to be
// * the only installer, so anything that loaded a model without a full
// * GUI boot -- ModelManagerTest, the headless judges -- dereferenced null.
// * World now discovers this implementation via ServiceLoader instead.
// *
// * The io module also registers a test loader (TestModelLoader, in its
// * test services file) serving the cube for engine tests that cannot
// * boot the GUI; production discovery finds this class.

package com.springie.io;

import com.springie.elements.nodes.NodeManager;
import com.springie.io.in.DataInput;
import com.springie.world.World;

public class DataInputModelLoader implements World.ModelLoader {

  public DataInputModelLoader() {
    // ServiceLoader instantiation
  }

  @Override
  public void load(final NodeManager manager, final String path) {
    new DataInput(manager).loadFile(path);
  }

  @Override
  public void loadFromString(final NodeManager manager, final String data) {
    new DataInput(null).addFromString(data, manager);
  }
}
