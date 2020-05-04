// This code has been placed into the public domain by its author

package com.springie.elements.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.electrostatics.ElectrostaticRepulsion;
import com.springie.elements.links.LinkManager;
import com.springie.geometry.Point3D;
import com.springie.geometry.Vector3D;
import com.springie.gui.gestures.PerformSelection;
import com.springie.messages.ArgumentList;
import com.springie.messages.NewMessage;
import com.springie.modification.post.PostModification;
import com.springie.modification.translation.CentreOnScreen;
import com.springie.modification.velocity.DampOverallVelocities;
import com.springie.presets.PresetObjects;
import com.springie.render.CachedNode;
import com.springie.render.Coords;
import com.springie.utilities.log.Log;
import com.springie.utilities.math.SquareRoot;
import com.springie.world.World;
import com.tifsoft.Forget;

/**
 * The class that manages the particular collection of entities that is
 * universal - and is not confined to a particular egg.
 */
public class NodeManager extends World {

	static int max_poss_dyn_size = 500;

	static int min_poss_dyn_size = 10;

	static Node min_x_node;

	static Node max_x_node;

	static Node min_y_node;

	static Node max_y_node;

	static int general_size = 256;

	public Range depth_range;

	int[] node_depth_index = new int[1];

	public ElectrostaticRepulsion electrostatic = new ElectrostaticRepulsion();

	public static int origin_shift_x;

	public static int origin_shift_y;

	public static int eye_distance = 7000;

	public static int mask_left = 0xFF0000;

	public static int mask_right = 0x00FFFF;

	public boolean nodes_have_been_deleted;

	public boolean is_tensegrity;

	final static int virus_color = 0xFFFF00FF;

	public NodeManager() {
		super();
	}

	public final void initial() {
		initialSetUp();

		if (!FrEnd.module) {
			addCreature();
		}
	}

	public final void initialWithPreset(ArgumentList preset_type) {
		initialSetUp();

		addCreatureFromPresetObject(preset_type);
	}

	public void initialSetUp() {
		set(null);
		initial_reset();
		resetNodeGrid(); // first of all...

		NodeManager.max_poss_dyn_size = Integer.MAX_VALUE;
		NodeManager.min_poss_dyn_size = 10;
	}

	public final void add() {
		addCreature(); // 64 + World.rnd.nextInt(128));
	}

	private void addCreature() {
		final String desc = FrEnd.choose_initial.choice.getSelectedItem();

		final String location = (String) FrEnd.choose_initial.hashtable.get(desc);

		addCreatureFromLocation(location);
	}

	void addCreatureFromPresetObject(ArgumentList preset_type) {
		String d = null;
		d = PresetObjects.getCreatureDescription(preset_type);
		FrEnd.data_input.addFromString(d, ContextMananger.getNodeManager());
	}

	public void addCreatureFromLocation(String location) {
		String d = null;
		if (FrEnd.archive != null) {
			if (!"".equals(FrEnd.archive)) {
				FrEnd.last_file_path = FrEnd.archive;
			}
		}

		try {
			d = PresetObjects.getCreatureDescription(location);
		} catch (IOException e) {
			reportProblem(location);
			e.printStackTrace();
		} catch (RuntimeException e) {
			reportProblem(location);
			e.printStackTrace();
		} catch (SAXException e) {
			reportProblem(location);
			e.printStackTrace();
		}

		FrEnd.data_input.addFromString(d, ContextMananger.getNodeManager());
	}

	private void reportProblem(String location) {
		Log.log("Problem getting information from location:" + location);
	}

	// called on resize...
	public final void resetNodeGrid() {
		final int gcd = (NodeManager.general_size + 4) << Coords.shift;

		int temp = 8;

		while (gcd > (1 << temp)) {
			temp++;
		}

		temp = 14;

		// this.node_grid = new GridOfBinsForCachedNodes(temp);
		this.node_grid.reset(temp);

		ensureEntitiesAreBinnedAgain();
	}

	final void ensureEntitiesAreBinnedAgain() {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			n.current_bin.x = -1;
			n.current_bin.y = -1;
			n.current_bin.z = -1;
		}
	}

	public void nodeAndLinkUpdate() {
		Node.temp_private_world = this;
		if (FrEnd.frame_frequency == 0) {
			oneFrameWorthOfUpdate();
		} else {
			updateAgentsAfterMoreThanOneFrameHasPassed();
		}
	}

	private void oneFrameWorthOfUpdate() {
		final int number_of_nodes = this.element.size();
		if (number_of_nodes > 0) {
			travelTheAgents();
		}

		if (needToMoveNodes()) {
			updateLinks(this.getLinkManager());

			applyAcceleration();
			if (FrEnd.check_collisions) {
				collisionCheck();
			}
		}
	}

	private void updateAgentsAfterMoreThanOneFrameHasPassed() {
		for (int temp = 0; temp <= FrEnd.frame_frequency; temp++) {
			oneFrameWorthOfUpdate();
		}
	}

	boolean needToMoveNodes() {
		if (FrEnd.paused) {
			return false;
		}

		if (FrEnd.forces_disabled_during_gesture) {
			return false;
		}

		return true;
	}

	private void updateLinks(LinkManager link_manager) {
		Forget.about(link_manager);
		if (needToMoveNodes()) {
			final int n_o_l = this.getLinkManager().element.size();
			if (n_o_l > 0) {
				if (!FrEnd.paused) {
					if (!FrEnd.links_disabled) {
						wrappedLinkExerciser(link_manager);
						ContextMananger.getNodeManager().electrostatic.repel();
						ContextMananger.getNodeManager().applyViscousDrag();
					}
				}
			}
		}
	}

	// just set up polygons...
	public void nodeAndLinkRenderDummy() {
		sortIndex();
		// Log.log("nodeAndLinkRenderNormal");
		final int number_of_nodes = this.element.size();
		this.renderer.renderer_node.ensureCapacity(number_of_nodes);
		final int mask = 0xFFFFFFFF;

		for (int counter = number_of_nodes; --counter >= 0;) {
			final int num = this.node_depth_index[counter];
			final Node node = (Node) this.element.get(num);
			final CachedNode cached_node = this.renderer.renderer_node.array[num];
			// if (FrEnd.render_nodes) {
			// cached_node.renderNodes(node, mask);
			// }

			// if (FrEnd.render_links) {
			cached_node.renderDummyLinks(this.renderer.renderer_link, getLinkManager(), node, mask);
			// }

			// if (FrEnd.render_polygons) {
			cached_node.renderDummyPolygons(this.renderer.renderer_face, getFaceManager(), node, mask);
			// }
		}
	}

	public void nodeAndLinkRenderNormal() {
		// Log.log("nodeAndLinkRenderNormal");
		final int number_of_nodes = this.element.size();
		this.renderer.renderer_node.ensureCapacity(number_of_nodes);
		final int mask = 0xFFFFFFFF;

		for (int counter = number_of_nodes; --counter >= 0;) {
			final int num = this.node_depth_index[counter];
			final Node node = (Node) this.element.get(num);
			final CachedNode cached_node = this.renderer.renderer_node.array[num];
			if (FrEnd.render_nodes) {
				cached_node.renderNodes(node, mask);
			}

			if (FrEnd.render_links) {
				cached_node.renderLinks(this.renderer.renderer_link, getLinkManager(), node, mask);
			}

			if (FrEnd.render_faces) {
				cached_node.renderPolygons(this.renderer.renderer_face, this.getFaceManager(), node, mask);
			}
		}
	}

	public void nodeAndLinkRenderAnaglyph() {
		int anaglyph_distance = NodeManager.eye_distance;
		final int number_of_nodes = this.element.size();

		this.renderer.renderer_node.ensureCapacity(number_of_nodes);
		this.renderer2.renderer_node.ensureCapacity(number_of_nodes);

		for (int counter = number_of_nodes; --counter >= 0;) {
			final int num = this.node_depth_index[counter];
			final Node n = (Node) this.element.get(num);
			final CachedNode cn1 = this.renderer.renderer_node.array[num];
			final CachedNode cn2 = this.renderer2.renderer_node.array[num];
			if (FrEnd.render_nodes) {
				Coords.shift_constant_x -= anaglyph_distance;
				cn1.renderNodes(n, mask_right);
				Coords.shift_constant_x += anaglyph_distance << 1;
				cn2.renderNodes(n, mask_left);
				Coords.shift_constant_x -= anaglyph_distance;
			}

			if (FrEnd.render_links) {
				Coords.shift_constant_x -= anaglyph_distance;
				cn1.renderLinks(this.renderer.renderer_link, getLinkManager(), n, mask_right);
				Coords.shift_constant_x += anaglyph_distance << 1;
				cn2.renderLinks(this.renderer2.renderer_link, getLinkManager(), n, mask_left);
				Coords.shift_constant_x -= anaglyph_distance;
			}

			if (FrEnd.render_faces) {
				Coords.shift_constant_x -= anaglyph_distance;
				cn1.renderPolygons(this.renderer.renderer_face, getFaceManager(), n, mask_right);
				Coords.shift_constant_x += anaglyph_distance << 1;
				cn1.renderPolygons(this.renderer2.renderer_face, getFaceManager(), n, mask_left);
				Coords.shift_constant_x -= anaglyph_distance;
			}
		}
	}

	private void setUpNewNodeDepthIndex() {
		// Log.log("EVERY TIME?"); // NO: fine...
		final int number_of_nodes = this.element.size();
		this.node_depth_index = new int[number_of_nodes];
		for (int temp = number_of_nodes; --temp >= 0;) {
			this.node_depth_index[temp] = temp;
		}
	}

	// to do - keep track of first and last flip...
	public void sortIndex() {
		final int number_of_nodes = this.element.size();
		if (number_of_nodes != this.node_depth_index.length) {
			setUpNewNodeDepthIndex();
		}

		if (FrEnd.redraw_deepest_first) {
			performTheNodeSort();
		}
	}

	private void performTheNodeSort() {
		// perform a dimwitted bubble sort... TODO improve sort...
		// Log.log("sortIndex");
		final int number_of_nodes = this.element.size();

		for (int i = number_of_nodes - 1; --i >= 0;) {
			boolean flipped = false;
			for (int j = 0; j <= i; j++) {
				final int k = j + 1;
				final int j1 = this.node_depth_index[j];
				final int k1 = this.node_depth_index[k];
				final Node a = (Node) this.element.get(j1);
				final Node b = (Node) this.element.get(k1);
				if (a.pos.z > b.pos.z) {
					int temp = this.node_depth_index[j];
					this.node_depth_index[j] = this.node_depth_index[k];
					this.node_depth_index[k] = temp;

					flipped = true;
				}
			}

			if (!flipped) {
				return;
			}
		}
	}

	final void agentExpansion() {
		final int number = this.node_type_factory.array.size();
		if (FrEnd.node_growth) {
			for (int temp = 0; temp < number; temp++) {
				final NodeType type = (NodeType) this.node_type_factory.array.elementAt(temp);
				if (type.radius < NodeManager.max_poss_dyn_size) {
					type.setSize(type.radius + 6);
				}
			}
		}

		if (FrEnd.continuously_centre) {
			CentreOnScreen.moveTowardsCentre(this);
			DampOverallVelocities.damp(this);
		}
	}

	public void collisionCheckNbyN() {
		final int number_of_nodes = this.element.size();
		for (int counter = number_of_nodes; --counter >= 0;) {
			temp_agent = (Node) this.element.get(counter);

			for (int counter2 = counter + 1; counter2 < number_of_nodes; counter2++) {
				temp2_agent = (Node) this.element.get(counter2);
				collideTheseEntities();
			}
		}
	}

	final void collisionCheck() {
		agentExpansion();

		collisionCheckNbyN();
	}

	public final void findNewBins() {
		final int number_of_nodes = this.element.size();
		for (int counter = number_of_nodes; --counter >= 0;) {
			temp_agent = (Node) this.element.get(counter);

			int new_bin_x = temp_agent.pos.x >> this.node_grid.log2binsize;
			int new_bin_y = temp_agent.pos.y >> this.node_grid.log2binsize;
			int new_bin_z = temp_agent.pos.z >> this.node_grid.log2binsize;

			if (new_bin_x < 0) {
				new_bin_x = 0;
			}

			if (new_bin_y < 0) {
				new_bin_y = 0;
			}

			if (new_bin_z < 0) {
				new_bin_z = 0;
			}

			if ((temp_agent.current_bin.x != new_bin_x) || (temp_agent.current_bin.y != new_bin_y)
					|| (temp_agent.current_bin.z != new_bin_z)) {
				this.node_grid.addToList(new_bin_x, new_bin_y, new_bin_z, temp_agent);
				this.node_grid.removeFromList(temp_agent.current_bin.x, temp_agent.current_bin.y,
						temp_agent.current_bin.z, temp_agent);

				temp_agent.current_bin.x = new_bin_x;
				temp_agent.current_bin.y = new_bin_y;
				temp_agent.current_bin.z = new_bin_z;
			}
		}
	}

	boolean sameSign(int a, int b) {
		return ((a ^ b) & 0x80000000) == 0;
	}

	final void applyAcceleration() {
		if (FrEnd.forces_disabled_during_gesture) {
			return;
		}

		ContextMananger.getNodeManager().creature_manager.update();

		// apply acceleration...
		final int number_of_nodes = this.element.size();
		if (number_of_nodes > 0) {
			for (int temp = 0; temp < number_of_nodes; temp++) {
				temp_agent = (Node) this.element.get(temp);
				applyGravity();
				applyTemperature();
				decrementCounter();
			}
		}
	}

	private void decrementCounter() {
		int counter = temp_agent.type.counter;
		if (counter > 0) {
			if (temp_agent.clazz.colour == virus_color) {
				if (temp_agent.type.counter == 1) {
					sendDeleteNodeMessage(temp_agent);
				}
			} else {
				if (counter == PerformSelection.INFECTION_START) {
					temp_agent.clazz = new Clazz(0xFFFF4040);
				}
				if (counter == PerformSelection.IMMUNITY_START) {
					temp_agent.clazz = new Clazz(0xFFFFFF40);
				}
				if (counter == 1) {
					temp_agent.clazz = new Clazz(0xFF40FF40);
				}
				if (!FrEnd.pandemic_paradigm_direct_contact) {
					if (counter == PerformSelection.VIRUS_RELEASE1) {
						releaseVirus(temp_agent);
					}
				}
			}
			temp_agent.type.counter--;
		}
	}

	private void sendDeleteNodeMessage(Node node) {
		FrEnd.new_message_manager.add(new NewMessage(null) {
			
			@Override
			public Object execute() {
				node.killWithNoExplosion();
				return null;
			}
		});
	}

	private void releaseVirus(Node agent) {
		int radius = 0x400;
		int delta = radius + agent.type.radius;
		createViralNode(agent, radius, delta, 0);
		createViralNode(agent, radius, -delta, 0);
		createViralNode(agent, radius, 0, delta);
		createViralNode(agent, radius, 0, -delta);
	}

	private void createViralNode(Node agent, int radius, int delta_x, int delta_y) {
		Node node = new Node(agent, this.node_type_factory, this.clazz_factory);
		node.clazz = this.clazz_factory.getNew(virus_color);
		node.type.radius = radius;
		node.type.counter = 20;
		int velocityExtra = 0xFFF;
		node.velocity.x += rnd.nextInt(velocityExtra);
		node.velocity.x -= rnd.nextInt(velocityExtra);
		node.velocity.y += rnd.nextInt(velocityExtra);
		node.velocity.y -= rnd.nextInt(velocityExtra);
		node.pos.x += delta_x;
		node.pos.y += delta_y;
		
		FrEnd.new_message_manager.add(new NewMessage(null) {
			
			@Override
			public Object execute() {
				element.add(node);
				FrEnd.postCleanup();
				return null;
			}
		});
	}

	private void applyGravity() {
		if (gravity_active) {
			temp_agent.velocity.y += World.gravity_strength;
		}
	}

	private void applyTemperature() {
		int temperature = World.global_temperature;
		if (temperature > 0) {
			temp_agent.velocity.x += rnd.nextInt(temperature << 1) - temperature;
			temp_agent.velocity.y += rnd.nextInt(temperature << 1) - temperature;
			temp_agent.velocity.z += rnd.nextInt(temperature << 1) - temperature;
		}
	}

	// z ??
	public final byte arcTangent(int dx, int dy) {
		final int temp_x0 = World.rnd.nextInt();

		if ((dx < 0) && (dy < 0)) {
			if (dx < dy) {
				return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31));
			}
			return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x10);
		}
		if ((dx < 0) && (dy >= 0)) {
			if (-dx < dy) {
				return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x60);
			}
			return (byte) (((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x70) & Node.TRIG_TAB_SIZEMO);
		}
		if ((dx >= 0) && (dy < 0)) {
			if (dx < -dy) {
				return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x20);
			}
			return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x30);
		}
		if (dx > dy) {
			return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x40);
		}
		return (byte) ((temp_x0 & 0xF) + (temp_x0 >>> 31) + 0x50);
	}

	// accepts temp_agent and temp2_agent
	public final void collideTheseEntities() {
		int tadx;
		int tady;
		int tadz;

		int temp_x;
		int temp_y;
		int temp_z;

		int temp_x0;
		int temp_y0;
		int temp_z0;

		int temp_x1;
		int temp_y1;
		int temp_z1;

		final int temp_detection_distance2 = temp_agent.type.radius + temp2_agent.type.radius;
		final int temp_detection_distance = temp_detection_distance2 >> Coords.shift;
		final Point3D pos = temp_agent.pos;
		final Point3D pos2 = temp2_agent.pos;

		temp_x0 = pos.x - pos2.x;
		temp_x1 = (temp_x0 < 0) ? -temp_x0 : temp_x0; // hmmm...
		if (temp_x1 < temp_detection_distance2) {

			temp_y0 = pos.y - pos2.y;
			temp_y1 = (temp_y0 < 0) ? -temp_y0 : temp_y0;
			if (temp_y1 < temp_detection_distance2) {

				temp_z0 = pos.z - pos2.z;
				temp_z1 = (temp_z0 < 0) ? -temp_z0 : temp_z0;
				if (temp_z1 < temp_detection_distance2) {
					// probable collision...
					// outer diamond check...
					if (((temp_detection_distance2 + (temp_detection_distance2 >> 1)) - temp_x1) > temp_y1) {
						// make smaller to prevent overflow...
						tadx = temp_x1 >> Coords.shift;
						tady = temp_y1 >> Coords.shift;
						tadz = temp_z1 >> Coords.shift;

						int temp_radius = (tadx * tadx) + (tady * tady) + (tadz * tadz); // now
						// check using circle...
						// temp_detection_distance = temp_detection_distance >> shift;
						final int temp_detection_distance_squared = temp_detection_distance * temp_detection_distance;
						if (temp_radius < temp_detection_distance_squared) {
							// collision
							// sqrt - really? :-(
							temp_radius = SquareRoot.fastSqrt(1 + temp_radius);

							// find unit vector along collision path...
							// unit vector * 65536
							temp_x = temp_x0 / temp_radius;
							temp_y = temp_y0 / temp_radius;
							temp_z = temp_z0 / temp_radius;

							// compute the relative velocities...
							tadx = temp_agent.velocity.x - temp2_agent.velocity.x;
							tady = temp_agent.velocity.y - temp2_agent.velocity.y;
							tadz = temp_agent.velocity.z - temp2_agent.velocity.z;

							// dot product...
							int magnitude = -(((tadx * temp_x) + (tady * temp_y) + (tadz * temp_z)) >> Coords.shift);
							// magnitude = (magnitude < 0) ? -magnitude : magnitude; // ???
							if (magnitude > 0) {
								int temp_count = ((temp_detection_distance - temp_radius) << 8) // !
										+ World.minimum_magnitude;

								if (temp_count > World.maximum_magnitude) {
									temp_count = World.maximum_magnitude;
								}

								if (magnitude < temp_count) {
									magnitude = temp_count;
								}

								// /very/ crude hack - since impulse calculations
								// don't seem to be running yet...
								temp_x = temp_x * magnitude;
								temp_y = temp_y * magnitude;
								temp_z = temp_z * magnitude;

								final int temp_ma = temp_agent.type.log_mass - temp2_agent.type.log_mass;

								if (temp_ma < 0) {
									tadx = temp_x >> -temp_ma;
									tady = temp_y >> -temp_ma;
									tadz = temp_z >> -temp_ma;

									temp_agent.velocity.x += tadx >> 9;
									temp_agent.velocity.y += tady >> 9;
									temp_agent.velocity.z += tadz >> 9;

									temp2_agent.velocity.x -= ((temp_x << 1) - tadx) >> 9;
									temp2_agent.velocity.y -= ((temp_y << 1) - tady) >> 9;
									temp2_agent.velocity.z -= ((temp_z << 1) - tadz) >> 9;
								} else {
									tadx = temp_x >> temp_ma;
									tady = temp_y >> temp_ma;
									tadz = temp_z >> temp_ma;

									temp_agent.velocity.x += ((temp_x << 1) - tadx) >> 9;
									temp_agent.velocity.y += ((temp_y << 1) - tady) >> 9;
									temp_agent.velocity.z += ((temp_z << 1) - tadz) >> 9;

									temp2_agent.velocity.x -= tadx >> 9;
									temp2_agent.velocity.y -= tady >> 9;
									temp2_agent.velocity.z -= tadz >> 9;
								}
								shrinkNodesOnCollision();
								spreadInfectionOnCollisions();
							}
						}
					}
				}
			}
		}
	}

	private void spreadInfectionOnCollisions() {
		final int counter1 = temp_agent.type.counter;
		final int counter2 = temp2_agent.type.counter;
		if (FrEnd.pandemic_paradigm_direct_contact) {
			spreadInfectionOneWay(temp2_agent, counter1, counter2);
			spreadInfectionOneWay(temp_agent, counter2, counter1);
		} else {
			spreadUsingVirus(temp_agent, temp2_agent, counter1);
			spreadUsingVirus(temp2_agent, temp_agent, counter2);
		}
	}

	private void spreadUsingVirus(Node node1, Node node2, final int counter) {
		if (counter == 0) {
			if (node2.clazz.colour == virus_color) {
				node1.type.counter = PerformSelection.INFECTION_START;
			}
		}
	}

	private void spreadInfectionOneWay(Node uninfectedNode, int counter1, int counter2) {
		if (counter1 > PerformSelection.IMMUNITY_START) {
			if (counter2 == 0) {
				uninfectedNode.type.counter = PerformSelection.INFECTION_START;
			}
		}
	}

	private void shrinkNodesOnCollision() {
		if (FrEnd.node_growth) {
			if (temp_agent.type.radius > NodeManager.min_poss_dyn_size) {
				temp_agent.type.setSize(temp_agent.type.radius - 10);
			}
			if (temp2_agent.type.radius > NodeManager.min_poss_dyn_size) {
				temp2_agent.type.setSize(temp2_agent.type.radius - 10);
			}
		}
	}

	// picker algorithm - simple - and slow...
	final Node isThereOneHelper(int _x, int _y) {
		// Log.log("isThereOne:" + _x + "," + _y);
		int tadx;
		int tady;
		Node candidate = null;

		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			temp_agent = (Node) this.element.get(temp);
			if (!temp_agent.type.hidden || FrEnd.render_hidden_nodes) {
				final int temp_detection_distance = Coords.getRadiusInternal(temp_agent.type.radius, temp_agent.pos.z);
				final int temp_x0 = Coords.getXCoordsInternal(temp_agent.pos.x, temp_agent.pos.z) - _x;
				final int temp_x1 = (temp_x0 < 0) ? -temp_x0 : temp_x0;
				if (temp_x1 < temp_detection_distance) {
					final int temp_y0 = Coords.getYCoordsInternal(temp_agent.pos.y, temp_agent.pos.z) - _y;
					final int temp_y1 = (temp_y0 < 0) ? -temp_y0 : temp_y0;
					// Log.log("temp_y1:" + temp_y1);
					// Log.log("temp_detection_distance:" + temp_detection_distance);
					if (temp_y1 < temp_detection_distance) {
						// probable collision...
						// make smaller to prevent overflow...
						tadx = temp_x1 >> Coords.shift;
						tady = temp_y1 >> Coords.shift;
						final int temp_radius = (tadx * tadx) + (tady * tady);
						// check using circle...
						final int tadx2 = temp_agent.type.radius >> Coords.shift;
						final int temp_detection_distance_squared = tadx2 * tadx2;
						if (temp_radius < temp_detection_distance_squared) {
							// Log.log("temp_detection_distance:" + temp_detection_distance);
							if ((candidate == null) || (candidate.pos.z > temp_agent.pos.z)) {
								candidate = temp_agent;
							}
						}
					}
				}
			}
		}

		return candidate;
	}

	// picker algorithm choice...
	public final Node isThereOne(int x, int y) {
		return isThereOneHelper(x, y);
	}

	public final void setRadiusOfSelected(int radius) {
		final int n_o_n = this.element.size();
		for (int temp = n_o_n; --temp >= 0;) {
			final Node node = (Node) this.element.get(temp);
			if (node.type.selected) {
				node.type.radius = radius;
			}
		}
	}

	public final void setChargeOfSelected(int charge) {
		final int n_o_n = this.element.size();
		for (int temp = n_o_n; --temp >= 0;) {
			final Node node = (Node) this.element.get(temp);
			if (node.type.selected) {
				node.type.charge = charge;
			}
		}

		new PostModification(this).cleanup();
	}

	public void travelTheAgents() {
		Node.temp_private_world = this;
		if (needToMoveNodes()) {
			final int number_of_nodes = this.element.size();
			for (int counter = number_of_nodes; --counter >= 0;) {
				((Node) this.element.get(counter)).travel();
			}
		}
	}

	public void setGlobalSize(int d) {
		NodeManager.general_size = d;

		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			((Node) this.element.get(temp)).type.setSize(d);
		}
	}

	public final boolean killThisNode(Node e) {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			if (this.element.get(temp) == e) {
				killNumberedAgent(temp);
				return true;
			}
		}

		return false;
	}

	public void killNumberedAgent(int n) {
		final Node node = (Node) this.element.get(n);
		this.getLinkManager().killAllLinks(node);
		this.getFaceManager().killAllPolygons(node);
		this.element.remove(n);

		// IMPORTANT: remove it from the bins...
		node.removeFromBin(this.node_grid);

		this.nodes_have_been_deleted = true;
	}

	public Node getSelectedNode() {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			// number_of_nodes--;
			final Node n = (Node) this.element.get(temp);

			if (n.type.selected) {
				return n;
			}
		}

		return null;
	}

	public void deselectAll() {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			if (n.type.selected) {
				n.type.selected = false;
			}
		}

		FrEnd.updateGUIToReflectSelectionChange();
	}

	public void deselectAll(int colour) {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			if (n.clazz.colour == colour) {
				if (n.type.selected) {
					n.type.selected = false;
					// BinGrid.RepaintAll = true;
				}
			}
		}
		FrEnd.updateGUIToReflectSelectionChange();
	}

	public void selectAll() {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node node = (Node) this.element.get(temp);
			if (!node.type.hidden || FrEnd.render_hidden_nodes) {
				if (!node.type.selected) {
					node.setSelected(true);
				}
			}
		}
		FrEnd.updateGUIToReflectSelectionChange();
	}

	public void selectionInvert() {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node node = (Node) this.element.get(temp);
			if (!node.type.hidden || FrEnd.render_hidden_nodes) {
				node.type.selected = !node.type.selected;
			}
		}
		FrEnd.updateGUIToReflectSelectionChange();
	}

	public void selectAll(int colour) {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node node = (Node) this.element.get(temp);
			if (node.clazz.colour == colour) {
				if (!node.type.hidden || FrEnd.render_hidden_nodes) {
					node.type.selected = true;
				}
			}
		}
		FrEnd.updateGUIToReflectSelectionChange();
	}

	public void deleteSelected() {
		final int number_of_nodes = this.element.size();
		for (int temp2 = number_of_nodes; --temp2 >= 0;) {
			final Node n = (Node) this.element.get(temp2);
			if (n.type.selected) {
				killNumberedAgent(temp2);
			}
		}
		FrEnd.updateGUIToReflectSelectionChange();
	}

	public final void setColourOfSelected(int c) {
		boolean clean_up = false;
		final int number_of_nodes = this.element.size();
		for (int temp2 = 0; temp2 < number_of_nodes; temp2++) {
			final Node n = (Node) this.element.get(temp2);
			if (n.type.selected) {
				n.clazz.colour = (char) c;
				clean_up = true;
			}
		}
		if (clean_up) {
			FrEnd.postCleanup();
		}
	}

	public void setSizeOfSelected(int s) {
		boolean clean_up = false;
		final int number_of_nodes = this.element.size();
		for (int temp2 = 0; temp2 < number_of_nodes; temp2++) {
			final Node n = (Node) this.element.get(temp2);
			if (n.type.selected) {
				n.type.setSize(s);
				clean_up = true;
			}
		}
		if (clean_up) {
			FrEnd.postCleanup();
		}
	}

	public Point3D getCentre() {
		long x = 0;
		long y = 0;
		long z = 0;

		int number_of_nodes = this.element.size();
		for (int counter = number_of_nodes; --counter >= 0;) {
			final Node candidate = (Node) this.element.get(counter);
			x += candidate.pos.x;
			y += candidate.pos.y;
			z += candidate.pos.z;
		}

		if (number_of_nodes == 0) {
			number_of_nodes = 1;
		}

		return new Point3D((int) x / number_of_nodes, (int) y / number_of_nodes, (int) z / number_of_nodes);
	}

	public void applyViscousDrag() {
		if (Node.viscocity != 0) {
			final int number_of_nodes = this.element.size();
			final int vd = 1024 - Node.viscocity;

			for (int counter = number_of_nodes; --counter >= 0;) {
				final Node node = (Node) this.element.get(counter);
				final Vector3D velocity = node.velocity;
				velocity.x = (velocity.x * vd + 512) >> 10;
				velocity.y = (velocity.y * vd + 512) >> 10;
				velocity.z = (velocity.z * vd + 512) >> 10;
			}
		}
	}

	public void makeSureNoClazzesOrTypesAreFlagged() {
		this.each_has_its_own_type = false;
		this.getLinkManager().each_has_its_own_type = false;
		this.getFaceManager().each_has_its_own_type = false;
		this.each_has_its_own_clazz = false;
	}

	public int getNumberOfSelected() {
		int number = 0;
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			if (n.type.selected) {
				number++;
			}
		}

		return number;
	}

	public int getNodeNumberFromName(String name) {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			if (name.equals(n.name)) {
				return temp;
			}
		}

		return -1;
	}

	public void selectAllWithNLinks(int n_links) {
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			final int num = n.list_of_links.size();
			if (n_links == num) {
				n.type.selected = true;
			}
		}
	}

	public Range getDepthRange() {
		if (this.depth_range == null) {
			calculateDepthRange();
		}

		return this.depth_range;
	}

	private void calculateDepthRange() {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;

		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = (Node) this.element.get(temp);
			int z = n.pos.z;
			if (z > max) {
				max = z;
			} else if (z < min) {
				min = z;
			}
		}
		this.depth_range = new Range(min, max);
	}

	public List<Node> getListOfSelectedNodes() {
		final List<Node> list_of_nodes = new ArrayList<>();
		final int number_of_nodes = this.element.size();
		for (int temp = 0; temp < number_of_nodes; temp++) {
			final Node n = this.element.get(temp);
			if (n.isSelected()) {
				list_of_nodes.add(n);
			}
		}
		return list_of_nodes;
	}

	public void moveSelection(int _dx, int _dy) {
		final int number_of_nodes = this.element.size();
		for (int temp2 = 0; temp2 < number_of_nodes; temp2++) {
			final Node n = this.element.get(temp2);
			if (n.type.selected) {
				n.pos.x += _dx;
				n.pos.y += _dy;
				n.velocity.x = 0;
				n.velocity.y = 0;

				n.boundaryCheck();

				// make sure bins are updated...
				n.findNewBin(this.node_grid);
			}
		}
	}

	public void moveNodesInList(List<Node> list_of_nodes, int d_x, int d_y) {
		final int number_of_nodes = list_of_nodes.size();
		for (int temp2 = 0; temp2 < number_of_nodes; temp2++) {
			final Node n = list_of_nodes.get(temp2);
			n.pos.x += d_x;
			n.pos.y += d_y;
			n.velocity.x = 0;
			n.velocity.y = 0;

			n.boundaryCheck();

			// make sure bins are updated...
			n.findNewBin(this.node_grid);
		}
	}
}
