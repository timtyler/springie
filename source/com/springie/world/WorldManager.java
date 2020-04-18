package com.springie.world;

import com.springie.composite.Composite;
import com.springie.elements.links.Link;
import com.springie.elements.nodes.Node;
import com.springie.utilities.log.Log;

public final class WorldManager {
  static World[] private_world;
  static World[] new_private_world;

  static int max_number_of_private_worlds = 16;
  static int number_of_private_worlds;

  static World pw;
  static World pw_from;
  static World temp_private_world;

  static Node pw_node;
  static Node temp_node;
  static Node temp_node2;

  static Link temp_link;
  static Link temp_link2;

  static Composite temp_creature;

  private WorldManager() {
    //...
  }

  public static void initial() {
    private_world = new World[max_number_of_private_worlds];
    number_of_private_worlds = 0;
  }

  public static void reset() {
    for (int temp = 0; temp < number_of_private_worlds; temp++) {
      private_world[temp].initial();
    }

    number_of_private_worlds = 0;
  }

  static void makeMoreWorlds() {
    max_number_of_private_worlds++;

    new_private_world = new World[max_number_of_private_worlds];

    for (int temp = 0; temp < (max_number_of_private_worlds - 1); temp++) {
      new_private_world[temp] = private_world[temp];
    }

    private_world = new_private_world;
  }

  public static void privateWorldUnbufferedUpdate() {
    for (int temp = 0; temp < number_of_private_worlds; temp++) {
      private_world[temp].privateWorldUnbufferedUpdate();
    }
  }

  static World getWorldAssociatedWith(Node e) {
    for (int temp = 0; temp < number_of_private_worlds; temp++) {
      temp_private_world = private_world[temp];
      // if (temp_private_world.contains(e)) {
      if (temp_private_world.associated_node == e) {
        return temp_private_world;
      }
    }

    Log.log("Couldn't find the World associated with this node :-(");

    return null;
  }

  // EXPERIMENT FURTHER WITH THIS - USE A SWAP...
  public static void copyEverythingOut(Node e, World pw_to) {
    pw_from = getWorldAssociatedWith(e);
    if (pw_from == null) {
      return;
    }

    final int number_of_nodes_from = pw_from.element.size();

    if (number_of_nodes_from > 0) {
      for (int temp = 0; temp < number_of_nodes_from; temp++) {

        temp_node = (Node) pw_from.element.elementAt(temp);
        pw_from.element.setElementAt(pw_to.element.lastElement(), temp);
        pw_to.element.add(temp, temp_node);
      }
    }

    final int n_o_l_from = pw_from.getLinkManager().element.size();

    if (n_o_l_from > 0) {
      for (int temp = 0; temp < n_o_l_from; temp++) {
        final Link l = (Link) pw_from.getLinkManager().element.elementAt(temp);
        pw_from.getLinkManager().element.setElementAt(pw_to.getLinkManager().element.elementAt(pw_to.getLinkManager().element.size()), temp);
        pw_to.getLinkManager().element.addElement(l);
      }
    }

    moveCreaturesBetweenWorlds(pw_from, pw_to); // needs care...

    killSpecifiedWorld(pw_from);
  }

  static void moveCreaturesBetweenWorlds(World _pw_from, World _pw_to) {
    for (int temp = _pw_from.creature_manager.number_of_creatures; --temp >= 0;) {
      temp_creature = _pw_to.creature_manager.add();

      temp_creature.private_world = _pw_from;
      _pw_from.creature_manager.creature[temp].private_world = _pw_to;

      _pw_to.creature_manager.creature[_pw_to.creature_manager.number_of_creatures - 1] = _pw_from.creature_manager.creature[temp];
      _pw_from.creature_manager.creature[temp] = temp_creature;
    }
  }

  static void killNumberedWorld(int n) {
    temp_private_world = private_world[n];
    private_world[n] = private_world[number_of_private_worlds - 1];
    private_world[--number_of_private_worlds] = temp_private_world;
  }

  static int getNumberOfWorld(World pw) {
    for (int temp = number_of_private_worlds; --temp >= 0;) {
      if (private_world[temp] == pw) {
        return temp;
      }
    }

    Log.log("World not found!");
    
    return -1;
  }

  static void killSpecifiedWorld(World pw) {
    final int temp = getNumberOfWorld(pw);
    killNumberedWorld(temp);
  }
}