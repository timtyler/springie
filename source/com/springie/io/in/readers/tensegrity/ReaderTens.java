package com.springie.io.in.readers.tensegrity;

import java.io.Reader;
import java.util.Vector;

import com.springie.FrEnd;
import com.springie.composite.Composite;
import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceType;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.modification.resize.LinkResetter;
import com.springie.utilities.log.Log;
import com.springie.world.World;

public final class ReaderTens {
  static Reader in;

  static int last_token_number = 999;

  static int node_number;

  static final boolean debug_parser = false;

  static int MAX_INS = Instructions.INS_NAMES.length;

  private ReaderTens() {
    // ...
  }

  public static void interpretBuffer(NodeManager node_manager, char[] buf,
      int x, int y, int z, int scale_factor) {
    final int index = buf.length;
    Composite current_creature = null;

    int in_token = 0;
    boolean in_number = false;
    boolean in_instruction = false;
    String number = "";
    String token = "";
    int current_object = 0;

    NodeType current_node_type = null;
    LinkType current_link_type = null;
    Clazz current_clazz = null;
    FaceType current_face_type = null;

    Node current_node = new Node();
    Link current_link = new Link(null, null, current_link_type, current_clazz);
    int current_link_number = -1;
    Face current_polygon = new Face(new Vector());
    int current_face_number = -1;

    setUpUniverseProperties();

    int i = 0;

    do {
      char c = buf[i];

      // ignore commented lines...
      if (c == '#') {
        do {
          c = buf[++i];
        } while (c > 31);
      }

      if (!in_instruction) {
        if ((c == '-') || (Character.isDigit(c))) { // start number...
          in_number = true;
          in_instruction = true;
          number += c;
        } else {
          if (Character.isLetter(c)) { // (Character.isUpperCase(c)))
            // { // start instruction...
            in_token = 1;
            in_instruction = true;
            token += c;
          }
        }
      } else { // is in existing instruction...
        if (in_token != 0) {
          if ((c >= '0') && (c != ':')) {
            token += c;
          } else { // finished this token...
            int t = 0;
            boolean found = false;
            // if (false) {
            boolean done = false;

            if (current_object == Instructions.NA) {
              current_node.name = token;
              current_object = Instructions.N;
              done = true;
            } else if (current_object == Instructions.LK) {
              if (last_token_number == Instructions.V) {
                final char ch = token.charAt(0);
                if (ch > '9') {
                  final int temp = node_manager.getNodeNumberFromName(token);
                  // Log.log("Link[" + token + "] maps to " + temp);
                  final Node temp_node = (Node) node_manager.element.get(temp);
                  current_link.addNode(temp_node, node_number);
                  temp_node.list_of_links.add(current_link_number);
                  node_number++;
                  done = true;
                }
              }
            } else if (current_object == Instructions.P) {
              if (last_token_number == Instructions.V) {
                final char ch = token.charAt(0);
                if (ch > '9') {
                  Log.log("token:" + token);
                  final int temp = node_manager.getNodeNumberFromName(token);
                  final Node temp_node = (Node) node_manager.element
                      .get(temp);
                  current_polygon.nodes.addElement(temp_node);
                  temp_node.list_of_polygons.add(current_face_number);
                  node_number++;
                  done = true;
                }
              }
            }

            if (done) {
              last_token_number = -99;
              token = "";
            } else {
              do {
                if (token.equals(Instructions.INS_NAMES[t++])) {
                  found = true;
                  t--;
                }
              } while (!found && (t != MAX_INS));

              if (t == MAX_INS) {
                if (debug_parser) {
                  Log.log("Unknown token: " + token + " (ignored)");
                }
              } else {
                last_token_number = t;
                switch (last_token_number) {
                  case Instructions.N:
                    node_manager.addNewAgent();

                    final int n = node_manager.element.size();
                    current_node = (Node) node_manager.element.get(n - 1);

                    current_object = last_token_number;
                    current_node.type = current_node_type;
                    current_node.clazz = current_clazz;

                    if (current_creature != null) {
                      current_creature.add(current_node);
                    }

                    break;

                  case Instructions.NA:
                    current_object = last_token_number;
                    break;

                  case Instructions.LK:
                    current_link_number = node_manager.getLinkManager().element
                        .size();
                    current_object = last_token_number;
                    node_manager.getLinkManager().setLink(current_link_type,
                        current_clazz);
                    int sz = node_manager.getLinkManager().element.size();
                    current_link = (Link) node_manager.getLinkManager().element.get(sz - 1);
                    node_number = 0;

                    break;

                  case Instructions.P:
                    current_face_number = node_manager.getFaceManager().element
                        .size();
                    current_object = last_token_number;
                    node_manager.getFaceManager().setPolygon(current_face_type,
                        current_clazz);
                    int sz2 = node_manager.getFaceManager().element.size();
                    current_polygon = (Face) node_manager.getFaceManager().element.get(sz2);
                    node_number = 0;

                    break;

                  case Instructions.NG:
                    current_object = last_token_number;
                    current_node_type = node_manager.node_type_factory.getNew();
                    current_clazz = node_manager.clazz_factory
                        .getNew(0xFFFFFFF0);

                    break;

                  case Instructions.LG:
                    current_object = last_token_number;
                    current_link_type = node_manager.getLinkManager().link_type_factory
                        .getNew(200, 50);
                    current_clazz = node_manager.clazz_factory
                        .getNew(0xFFFFF0FF);

                    break;

                  case Instructions.PG:
                    current_object = last_token_number;
                    current_face_type = node_manager.getFaceManager().face_type_factory
                        .getNew();
                    // Log.log("ReaderTens: NEW PG");
                    current_clazz = node_manager.clazz_factory
                        .getNew(0xFFF0FFFF);

                    break;

                  case Instructions.CR:
                    current_creature = node_manager.creature_manager.add(); // creature_manager.creature[creature_manager.number_of_creatures
                    current_creature.private_world = node_manager;

                    current_object = last_token_number;
                    current_link.type = current_link_type;

                    break;

                  case Instructions.G:
                    current_object = last_token_number;
                    break;

                  case Instructions.GO:
                    // current_creature = manager.creature_manager.add(); //
                    // creature_manager.creature[creature_manager.number_of_creatures
                    // current_creature.private_world = manager;

                    // current_object = last_token_number;

                    break;

                  case Instructions.SEL_ALL_LNK:
                    node_manager.getLinkManager().selectAll();

                    break;

                  case Instructions.DES_ALL_LNK:
                    node_manager.getLinkManager().deselectAll();

                    break;

                  case Instructions.RST_LNK_LEN:
                    final LinkResetter lr = new LinkResetter(node_manager);
                    lr.reset();

                    break;

                  default:
                    break;
                // throw new RuntimeException("");
                }

                // current_object = last_token_number;

                if (debug_parser) {
                  Log.log("Token: " + token + " (" + t + ")");
                }
              }

              in_token = 0;
              in_instruction = false;
              token = "";
            }
          }
        }

        if (in_number) {
          if (c >= '0') {
            number += c;
          } else { // finished number...
            int temp;
            if ((number.length() >= 3) && (number.charAt(1) == 'x')) {
              number = number.substring(2);
              if (debug_parser) {
                Log.log("Hex number: 0x" + number + " ("
                    + (int) Long.parseLong(number, 16) + ")");
              }

              temp = (int) Long.parseLong(number, 16);
            } else {
              temp = (int) Long.parseLong(number);
            }

            switch (current_object) {
              case Instructions.G: // gravity...
                switch (last_token_number) {
                  case Instructions.GO:
                    World.gravity_active = temp == 1;
                    break;

                  case Instructions.GS:
                    World.gravity_strength = temp;

                    break;

                  case Instructions.CO:
                    ContextMananger.getNodeManager().electrostatic.charge_active = temp == 1;
                    break;

                  case Instructions.DIM:
                    FrEnd.three_d = temp == 3;
                    break;

                  // case Instructions.CS:
                  // ContextMananger.getNodeManager().electrostatic.charge_strength = temp;
                  // break;

                  default:
                    break;
                }
                break;

              case Instructions.NG: // nodes
                switch (last_token_number) {
                  case Instructions.R:
                    current_node_type.setSize((temp * scale_factor) >> 8);
                    break;

                  case Instructions.CH:
                    current_node_type.charge = temp;
                    break;

                  case Instructions.C:
                    current_clazz.colour = temp;

                    break;

                  case Instructions.H:
                    current_node_type.hidden = temp != 0;
                    break;

                  case Instructions.FX:
                    current_node_type.pinned = temp != 0;
                    break;

                  default:
                    break;
                }
                break;

              case Instructions.LG: // links
                switch (last_token_number) {
                  case Instructions.E:
                    current_link_type.elasticity = temp;

                    break;

                  case Instructions.DA:
                    current_link_type.damping = temp;

                    break;

                  case Instructions.L:
                    current_link_type.setLength((temp * scale_factor) >> 8);

                    break;

                  case Instructions.C:
                    current_clazz.colour = temp;

                    break;

                  case Instructions.R:
                    current_link_type.radius = (temp * scale_factor) >> 8;

                    break;

                  case Instructions.TE:
                    current_link_type.tension = temp != 0;

                    break;

                  case Instructions.CP:
                    current_link_type.compression = temp != 0;

                    break;

                  case Instructions.H:
                    current_link_type.hidden = temp != 0;
                    break;

                  case Instructions.D:
                    current_link_type.disabled = temp != 0;
                    break;

                  default:
                    break;
                }

                break;

              case Instructions.PG:
                switch (last_token_number) {
                  case Instructions.C:
                    current_clazz.colour = temp;

                    break;

                  case Instructions.H:
                    current_face_type.hidden = temp != 0;
                    break;

                  default:
                    break;
                }

                break;

              case Instructions.N:
                switch (last_token_number) {
                  case Instructions.X:
                    current_node.pos.x = ((temp * scale_factor) >> 8) + x;

                    break;

                  case Instructions.Y:
                    current_node.pos.y = ((temp * scale_factor) >> 8) + y;
                    // Log.log("Y:" + current_node.pos.y);

                    break;

                  case Instructions.Z:
                    current_node.pos.z = ((temp * scale_factor) >> 8) + z;
                    break;

                  case Instructions.DX:
                    current_node.velocity.x = (temp * scale_factor) >> 8;

                    break;

                  case Instructions.DY:
                    current_node.velocity.y = (temp * scale_factor) >> 8;
                    break;

                  case Instructions.DZ:
                    current_node.velocity.z = (temp * scale_factor) >> 8;
                    break;

                  default:
                    break;
                // throw new RuntimeException("");
                }

                break;

              case Instructions.LK:
                // no links yet...
                switch (last_token_number) {
                  case Instructions.V:
                    // if (node_number == 0) {
                    // TODO: handle expansion
                    final Node temp_node = (Node) node_manager.element
                        .get(temp);
                    current_link.addNode(temp_node, node_number);
                    temp_node.list_of_links.add(current_link_number);
                    node_number++;
                    last_token_number = -99;

                    break;

                  default:
                    break;
                }

                break;

              case Instructions.P:
                switch (last_token_number) {
                  case Instructions.V:
                    final Node temp_node1 = (Node) node_manager.element
                        .get(temp);
                    current_polygon.nodes.addElement(temp_node1);
                    temp_node1.list_of_polygons.add(current_face_number);
                    node_number++;
                    last_token_number = -99;

                    break;

                  default:
                    break;
                }

                break;

              case Instructions.CR:
                // creature...
                switch (last_token_number) {
                  // case 23:
                  // data...
                  // current_creature.genome.add(temp);

                  // break;
                  case Instructions.F:
                    // frequency...
                    current_creature.oscillator.setPhase((char) temp);

                    break;
                  default:
                    break;
                // throw new RuntimeException("");
                }

                break;

              default:
                Log.log("current_object:" + current_object);
                throw new RuntimeException("");
            }

            if (debug_parser) {
              Log.log("Number: " + number);
            }

            in_number = false;
            in_instruction = false;
            number = "";
          }
        }
      }
    } while (++i < index);

    if (node_manager == ContextMananger.getNodeManager()) {
      FrEnd.reflectValuesInGUIAfterPropertyEditing();
    }
  }

  //?
  private static void setUpUniverseProperties() {
    FrEnd.three_d = true;
    ContextMananger.getNodeManager().electrostatic.charge_active = true;
    World.gravity_active = false;
    World.gravity_strength = 10;
  }
}