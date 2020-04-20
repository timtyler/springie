package com.springie.io.out.writers.pov;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Vector;

import com.springie.FrEnd;
import com.springie.context.ContextMananger;
import com.springie.elements.clazz.Clazz;
import com.springie.elements.faces.Face;
import com.springie.elements.faces.FaceManager;
import com.springie.elements.links.Link;
import com.springie.elements.links.LinkManager;
import com.springie.elements.links.LinkType;
import com.springie.elements.nodes.Node;
import com.springie.elements.nodes.NodeManager;
import com.springie.elements.nodes.NodeType;
import com.springie.geometry.Point3D;
import com.springie.gui.panels.preferences.PanelPreferencesIO;
import com.springie.io.out.AreThereAny;
import com.springie.io.out.GarbageCollection;
import com.springie.io.out.WriteFloatingPoint;
import com.springie.metrics.BoundingBox;
import com.springie.modification.redundancy.RedundancyRemover;
import com.springie.preferences.Preferences;
import com.springie.utilities.log.Log;
import com.tifsoft.Forget;

public class WriterPOV {
	double scale_factor;

	BoundingBox bb;

	Point3D middle = new Point3D(0, 0, 0);

	Writer out;

	Vector nodes;

	private NodeManager node_manager;

	private LinkManager link_manager;

	private FaceManager face_manager;

	public WriterPOV(NodeManager node_manager) {
		this.node_manager = node_manager;
		this.link_manager = node_manager.getLinkManager();
		this.face_manager = node_manager.getFaceManager();
	}

	public void write(String filename) {
		new GarbageCollection(this.node_manager).cleanUp();
		new RedundancyRemover(this.node_manager).removeRedundancy();

		this.bb = new BoundingBox();
		this.bb.find(this.node_manager);

		final float sfx = 2f / (this.bb.max.x - this.bb.min.x);
		final float sfy = 2f / (this.bb.max.y - this.bb.min.y);
		final float sfz = 2f / (this.bb.max.z - this.bb.min.z);
		this.scale_factor = Math.min(sfx, Math.min(sfy, sfz));

		this.middle.x = (this.bb.max.x + this.bb.min.x) >> 1;
		this.middle.y = (this.bb.max.y + this.bb.min.y) >> 1;
		this.middle.z = (this.bb.max.z + this.bb.min.z) >> 1;

		this.nodes = new Vector();

		try {
			try {
				this.out = new FileWriter(filename);

				final float height_above_water = PanelPreferencesIO.pov_view_height / 50F;
				final float immersion_level = PanelPreferencesIO.pov_immersion_depth / 50F;

				writeHeader(height_above_water, immersion_level);

				final String output_pov_ground = (String) FrEnd.preferences.map.get("output.pov.ground");
				final String output_pov_sky = (String) FrEnd.preferences.map.get("output.pov.sky");

				if ("rock".equals(output_pov_ground)) {
					writeGroundRock(immersion_level);
				} else if ("water".equals(output_pov_ground)) {
					writeGroundWater(immersion_level);
				} else if ("wood".equals(output_pov_ground)) {
					writeGroundWood(immersion_level, 19);
					// } else {
					// writeGroundNone(immersion_level);
				}

				if ("cirrus".equals(output_pov_sky)) {
					writeCirrus();
				} else if ("cloud 1".equals(output_pov_sky)) {
					writeCloud1();
				} else if ("cloud 2".equals(output_pov_sky)) {
					writeCloud2();
				} else if ("white".equals(output_pov_sky)) {
					writeWhite();
				} else if ("black".equals(output_pov_sky)) {
					writeBlack();
				} else {
					writeCumulous();
				}

				if (!"none".equals(output_pov_ground)) {
					writeFog();
				}

				writeClasses();

				writeLine("// nodes");
				outputNodeClasses();

				writeLine("// links");
				outputLinkClasses();

				writeLine("// faces");
				outputPolygonClasses();

			} finally {
				this.out.flush();
				this.out.close();
			}
		} catch (IOException e) {
			Log.log("Error in write: " + e);
		}
	}

	boolean usingWood() {
		final String output_pov_ground = (String) FrEnd.preferences.map.get("output.pov.ground");

		return "wood".equals(output_pov_ground);
	}

	private void writeHeader(float height_above_water, float immersion_level) {
		final float h = -1 + height_above_water + immersion_level;

		final double x = Math.sqrt((2.25 * 2.25) - (h * h));

		writeLine("#include \"colors.inc\"");
		writeLine("#include \"shapes.inc\"");
		writeLine("#include \"textures.inc\"");
		writeLine("#include \"metals.inc\"");
		writeLine("#include \"skies.inc\"");

		if (usingWood()) {
			writeLine("#include \"woods.inc\"");
		}

		writeLine("");
		writeLine("#declare CameraLocation = <0, " + h + ", -" + x + ">;");
		writeLine("camera {");
		writeLine(" location CameraLocation");
		writeLine(" look_at <0, 0, 0>");
		writeLine("}");
		writeLine("");
		writeLine("light_source {");
		writeLine(" <-7,7,-4>");
		writeLine(" color rgb <1,1,1>");
		writeLine("}");
		writeLine("");
		writeLine("light_source {");
		writeLine(" CameraLocation");
		writeLine(" color rgb <0.5,0.5,0.5>");
		writeLine("}");
		writeLine("");

		// writeLine("light_source {");
		// writeLine(" <-10,3,-30>");
		// writeLine(" color rgb <0.6,0.55,0.6>");
		// writeLine("}");
		// writeLine("");

		// writeLine("#declare cameraRight = <0.9,0.0,-0.2>;");
		// writeLine("#declare stereoSpacing = 1;");
		// writeLine("#declare stereoCameraTranslation = <0,0,0>;");
		// writeLine("#switch (int(clock))");
		// writeLine(" #case (0)");
		// writeLine(" #debug \"Left\"");
		// writeLine(" #declare stereoCameraTranslation =
		// (cameraRight*(-stereoSpacing/2));");
		// writeLine(" #break");
		// writeLine(" #case (1)");
		// writeLine(" #debug \"Right\"");
		// writeLine(" #declare stereoCameraTranslation =
		// (cameraRight*(stereoSpacing/2));");
		// writeLine(" #break");
		// writeLine("#end");

		// writeLine("sky_sphere{S_Cloud3}");
		// writeLine("sky_sphere{ pigment { color rgb <0,0,0> } }");
	}

	private void writeClasses() {
		final int clazz_number = this.node_manager.clazz_factory.array.size();
		final int number_of_faces = this.face_manager.element.size();

		for (int c = 0; c < clazz_number; c++) {
			final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array.get(c);
			final int colour = clazz.colour;
			final float r = (((colour >> 16) & 0xFF)) / 255f;
			final float g = (((colour >> 8) & 0xFF)) / 255f;
			final float b = (colour & 0xFF) / 255f;
			final float o = (((colour >> 24) & 0xFF)) / 255f;
			final float reflection = o;

			writeLine("#declare CN" + c + " = texture { pigment { color rgb <" + emit(r) + "," + emit(g) + "," + emit(b)
					+ "> } finish { phong 0.9 phong_size 40 } }");
			// normal { bumps 0.8 scale 0.005 }
			writeLine("#declare CL" + c + " = texture { pigment { color rgb <" + emit(r) + "," + emit(g) + "," + emit(b)
					+ "> } finish { phong 0.9 phong_size 40 } }");
			// normal { bumps 0.4 scale 0.008 }
			if (number_of_faces > 0) {
				if (o < 0.99) {
					writeLine("#declare CF" + c + " = texture { pigment { color rgbt <" + emit(r) + "," + emit(g) + ","
							+ emit(b) + "," + emit(o) + "> } finish { ambient 0.05 " + "diffuse 0.05 reflection { "
							+ emit(reflection) + " fresnel off } } }");
				} else {
					writeLine("#declare CF" + c + " = pigment { color rgb <" + emit(r) + "," + emit(g) + "," + emit(b)
							+ "> }");
				}
			}
		}
	}

	private void writeCumulous() {
		writeLine("sky_sphere { S_Cloud3 }");
		writeLine("");
	}

	private void writeCloud1() {
		writeLine("sky_sphere { S_Cloud3 }");
		writeLine("object { O_Cloud1 scale .003 }");
		writeLine("");
	}

	private void writeCloud2() {
		writeLine("sky_sphere { S_Cloud3 }");
		writeLine("object { O_Cloud2 scale .005 }");
		writeLine("");
	}

	private void writeWhite() {
		writeLine("background { color White }");
		writeLine("");
	}

	private void writeBlack() {
		writeLine("background { color Black }");
		writeLine("");
	}

	private void writeCirrus() {
		writeLine("#declare CLOUD1 =");
		writeLine("texture {");
		writeLine(" pigment { bozo");
		writeLine("  turbulence 1.5");
		writeLine("  octaves 10");
		writeLine("  omega 0.5");
		writeLine("  lambda 2.5");
		writeLine("  color_map {");
		writeLine("   [0.0 color rgbf<0.85, 0.85, 0.85, 0.00>*1.3 ]");
		writeLine("   [0.5 color rgbf<0.95, 0.95, 0.95, 0.90>*1.1 ]");
		writeLine("   [0.7 color rgbf<1, 1, 1, 1> ]");
		writeLine("   [1.0 color rgbf<1, 1, 1, 1> ]");
		writeLine("  }");
		writeLine(" }");
		writeLine("");
		writeLine(" finish {ambient 0.95 diffuse 0.1}");
		writeLine("}");
		writeLine("");
		writeLine("#declare CLOUD2 =");
		writeLine("texture {");
		writeLine(" pigment {");
		writeLine("  bozo");
		writeLine("  turbulence 0.8 //0.6");
		writeLine("  octaves 10");
		writeLine("  omega 0.5");
		writeLine("  lambda 2.5");
		writeLine("  color_map {");
		writeLine("   [0.0 color rgbf<0.95, 0.95, 0.95, 0.00>]");
		writeLine("   [0.4 color rgbf<0.90, 0.90, 0.90, 0.90>]");
		writeLine("   [0.7 color rgbf<1, 1, 1, 1> ]");
		writeLine("   [1.0 color rgbf<1, 1, 1, 1> ]");
		writeLine("  }");
		writeLine(" }");
		writeLine(" finish {ambient 1.0 diffuse 0.0 }");
		writeLine("}");
		writeLine("");
		writeLine("texture {");
		writeLine(" pigment {");
		writeLine("  bozo");
		writeLine("  turbulence 0.8");
		writeLine("  octaves 10");
		writeLine("  omega 0.5");
		writeLine("  lambda 2.5");
		writeLine("  color_map {");
		writeLine("   [0.00, color rgbf<.85, .85, .85, 0.5>*1.2 ]");
		writeLine("   [0.35 color rgbf<.95, .95, .95, .95>]");
		writeLine("   [0.5 color rgbf<1, 1, 1, 1> ]");
		writeLine("   [1.0 color rgbf<1, 1, 1, 1> ]");
		writeLine("  }");
		writeLine(" }");
		writeLine("");
		writeLine(" finish {");
		writeLine("  ambient 0.95 diffuse 0.0");
		writeLine(" }");
		writeLine(" scale 0.9");
		writeLine(" translate y*-0.15");
		writeLine("}");
		writeLine("");
		writeLine("#declare CLOUD3 =");
		writeLine("union {");
		writeLine(" plane {");
		writeLine("  <0,1,0>, 500");
		writeLine("  hollow");
		writeLine("  texture {");
		writeLine("   CLOUD2  scale 500");
		writeLine("  }");
		writeLine(" }");
		writeLine("");
		writeLine(" plane {");
		writeLine("  <0,1,0>, 3000");
		writeLine("  hollow on");
		writeLine("  texture {");
		writeLine("   CLOUD1 scale <900,1,3000> ");
		writeLine("   translate <3000,0,0>");
		writeLine("   rotate <0,-30,0>");
		writeLine("  }");
		writeLine(" }");
		writeLine("");
		writeLine(" plane {");
		writeLine("  <0,1,0>, 10000");
		writeLine("  hollow on");
		writeLine("  texture {");
		writeLine("   pigment {");
		writeLine("    color SkyBlue*0.95");
		writeLine("   }");
		writeLine("   finish {");
		writeLine("    ambient 1 diffuse 0");
		writeLine("   }");
		writeLine("  }");
		writeLine(" }");
		writeLine("");
		writeLine(" scale<1.5,1,1.25>");
		writeLine("}");
		writeLine("");
		writeLine("object{CLOUD3 rotate<0,0,0> translate<0,0,0>}");
		writeLine("");
	}

	void writeFog() {
		writeLine("fog{fog_type 2");
		writeLine(" distance 300");
		writeLine(" color White*0.9");
		writeLine(" fog_offset 0.1");
		writeLine(" fog_alt 5");
		writeLine(" turbulence 0.8");
		writeLine("}");
		writeLine("");
	}

	private void writeGroundRock(float immersion_level) {
		writeLine("plane {");
		writeLine(" y, " + (-1 + immersion_level));
		writeLine(" hollow on");
		writeLine(" material {");
		writeLine("  texture {");
		writeLine("   pigment {");
		writeLine("    color rgb < 1, 0.9, 0.65>");
		writeLine("   }");
		writeLine("   normal {");
		writeLine("    granite 1 scale 2");
		writeLine("   }");
		writeLine("   finish {");
		writeLine("    brilliance 1.2");
		writeLine("    specular 0.3");
		writeLine("   }");
		writeLine("  }");
		writeLine(" }");
		writeLine("}");
		writeLine("");
	}

	private void writeGroundWood(float immersion_level, int wood_number) {
		writeLine("plane {");
		writeLine(" y, " + (-1 + immersion_level));
		writeLine(" hollow on");
		writeLine(" material {");
		writeLine("  texture {");
		writeLine("   T_Wood" + wood_number);
		writeLine("   finish {");
		// writeLine(" brilliance 1.2");
		writeLine("    specular 0.5");
		writeLine("    roughness 0.1");
		writeLine("    ambient 0.25");
		writeLine("   }");
		writeLine("  }");
		writeLine(" }");
		writeLine("}");
		writeLine("");
	}

	private void writeGroundWater(float immersion_level) {
		writeLine("plane {");
		writeLine(" y, " + (-1.01 + immersion_level));
		writeLine(" hollow on");
		writeLine(" material {");
		writeLine("  texture {");
		writeLine("   pigment {");
		writeLine("    color rgbt <1, 1, 1, 1>");
		writeLine("   }");
		writeLine("   finish {");
		writeLine("    ambient 0");
		writeLine("    diffuse 0");
		writeLine("");
		writeLine("    reflection { 0, 0.9 fresnel off }");
		writeLine("");
		writeLine("    specular 0.3");
		writeLine("    roughness 0.003");
		writeLine("   }");
		writeLine("   normal {");
		writeLine("    function {");
		writeLine("     f_ridged_mf(x, y, z, 0.1, 3, 7, 0.6, 0.6, 2)");
		writeLine("    }");
		writeLine("    0.8 scale 0.13");
		writeLine("   }");
		writeLine("  }");
		writeLine("  interior {");
		writeLine("   ior 1.3");
		writeLine("  }");
		writeLine(" }");
		writeLine("}");
		writeLine("");
		writeLine("plane {");
		writeLine(" y, " + (-1 + immersion_level));
		writeLine(" hollow on");
		writeLine(" material {");
		writeLine("  texture {");
		writeLine("   pigment {");
		writeLine("    color rgbt <1, 1, 1, 1>");
		writeLine("   }");
		writeLine("   finish {");
		writeLine("    ambient 0");
		writeLine("    diffuse 0");
		writeLine("");
		writeLine("    reflection { 0, 0.9 fresnel off }");
		writeLine("");
		writeLine("    specular 0.3");
		writeLine("    roughness 0.003");
		writeLine("   }");
		writeLine("   normal {");
		writeLine("    function {");
		writeLine("     f_ridged_mf(x, y, z, 0.1, 3, 7, 0.6, 0.6, 2)");
		writeLine("    }");
		writeLine("    0.8 scale 0.13");
		writeLine("   }");
		writeLine("  }");
		writeLine("  interior {");
		writeLine("   ior 1.3");
		writeLine("  }");
		writeLine(" }");
		writeLine("}");
		writeLine("");
		writeLine("plane {");
		writeLine(" y, " + (-1.02 + immersion_level));
		writeLine(" hollow on");
		writeLine(" texture {");
		writeLine("  pigment {");
		writeLine("   color rgb<0,0,0>");
		writeLine("  }");
		writeLine(" }");
		writeLine("}");
		writeLine("");
	}

	private void outputNodeClasses() {
		final int clazz_number = this.node_manager.clazz_factory.array.size();
		for (int c = 0; c < clazz_number; c++) {
			final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array.get(c);
			outputNodeTypes(c, clazz);
		}
	}

	private void outputLinkClasses() {
		final int clazz_number = this.node_manager.clazz_factory.array.size();
		for (int c = 0; c < clazz_number; c++) {
			final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array.get(c);
			outputLinkTypes(clazz, c);
		}
	}

	private void outputPolygonClasses() {
		final int clazz_number = this.node_manager.clazz_factory.array.size();
		for (int n_class = 0; n_class < clazz_number; n_class++) {
			final Clazz clazz = (Clazz) this.node_manager.clazz_factory.array.get(n_class);
			outputPolygonTypes(clazz, n_class);
		}
	}

	private void outputNodeTypes(int n_clazz, Clazz clazz) {
		final int node_type_number = this.node_manager.node_type_factory.array.size();

		if (!new AreThereAny().nodes(clazz)) {
			return;
		}

		for (int n_type = 0; n_type < node_type_number; n_type++) {
			// outputNodeType(clazz, n_type);
			outputNodes(clazz, n_clazz, n_type);
			// writeLine("");
		}
	}

	private void outputLinkTypes(Clazz clazz, int n_clazz) {
		final int link_type_number = this.link_manager.link_type_factory.array.size();

		if (!new AreThereAny().links(clazz)) {
			return;
		}

		for (int n_type = 0; n_type < link_type_number; n_type++) {
			final LinkType type = (LinkType) this.link_manager.link_type_factory.array.elementAt(n_type);

			if (new AreThereAny().links(clazz, type)) {
				outputLinks(clazz, type, n_clazz, n_type);
			}
		}
	}

	private void outputLinks(Clazz clazz, LinkType link_type, int n_clazz, int n_type) {
		Forget.about(link_type);

		outputLinks(clazz, n_clazz, n_type);
		writeLine("");
	}

	private void outputPolygonTypes(Clazz clazz, int n_clazz) {
		// final int type_number = this.face_manager.face_type_factory.array.size();
		if (!new AreThereAny().polygons(clazz)) {
			return;
		}

		outputFaces(clazz, n_clazz);
	}

	private void outputFaces(Clazz clazz, int n_clazz) {
		Forget.about(clazz);

		// final FaceType type = (FaceType)
		// this.face_manager.face_type_factory.array
		// .elementAt(number);

		final int number_of_faces = this.face_manager.element.size();

		for (int n = 0; n < number_of_faces; n++) {
			final Face face = (Face) this.face_manager.element.elementAt(n);
			if (!face.type.hidden || FrEnd.render_hidden_faces) {
				writeFaceClockwise(face, n_clazz);
			}
		}
	}

	private void writeFaceClockwise(final Face polygon, int n_clazz) {
		final int npolygon = polygon.nodes.size();

		final Point3D p3 = polygon.getCoordinatessOfCentre();

		for (int i = npolygon; --i >= 0;) {
			final Node node1 = (Node) polygon.nodes.elementAt(i);
			final Node node2 = (Node) polygon.nodes.elementAt((i + 1) % npolygon);
			final Point3D p1 = node1.pos;
			final Point3D p2 = node2.pos;

			writeLine("polygon {");
			writeLine(" 4,");

			outputCoordinates(p1, ",");
			outputCoordinates(p2, ",");
			outputCoordinates(p3, ",");
			outputCoordinates(p1, "");
			if ((polygon.clazz.colour & 0xFF000000) == 0xFF000000) {
				final int colour = polygon.clazz.colour;
				final float r = (((colour >> 16) & 0xFF)) / 255f;
				final float g = (((colour >> 8) & 0xFF)) / 255f;
				final float b = (colour & 0xFF) / 255f;
				// final float o = (((colour >> 24) & 0xFF)) / 255f;
				writeLine("pigment { color rgb <" + emit(r) + "," + emit(g) + "," + emit(b) + "> }");
			} else {
				writeLine(" texture { CF" + n_clazz + " } ");
			}
			writeLine("}");
		}
	}

	private void outputCoordinates(Point3D pos, String appended) {
		final double x = toVRMLCoords(pos.x - this.middle.x);
		final double y = toVRMLCoords(this.middle.y - pos.y);
		final double z = toVRMLCoords(pos.z - this.middle.z);

		writeLine(" <" + emit(x) + ", " + emit(y) + ", " + emit(z) + ">" + appended);
	}

	private void outputNodes(Clazz clazz, int n_clazz, int n_type) {
		final NodeType node_type = (NodeType) ContextMananger.getNodeManager().node_type_factory.array
				.elementAt(n_type);
		final int number_of_nodes = this.node_manager.element.size();
		for (int n = 0; n < number_of_nodes; n++) {
			final Node node = (Node) this.node_manager.element.elementAt(n);
			if (node.type == node_type) {
				if (node.clazz == clazz) {
					if (!node.type.hidden || FrEnd.render_hidden_nodes) {
						outputNode(node, n_clazz, n_type);
					}
				}
			}
		}
	}

	private void outputNode(final Node node, int n_clazz, int n_type) {
		Forget.about(n_type);

		final double x = toVRMLCoords(node.pos.x - this.middle.x);
		// final float y = toVRMLCoords(node.pos.y - this.middle.y);
		final double y = toVRMLCoords(this.middle.y - node.pos.y);
		final double z = toVRMLCoords(node.pos.z - this.middle.z);
		// final float z = toVRMLCoords(this.middle.z - node.pos.z);
		final double radius = toVRMLCoords(node.type.radius);

		writeLine("sphere {");
		writeLine(" <" + emit(x) + ", " + emit(y) + ", " + emit(z) + ">, " + emit(radius));
		writeLine(" texture { CN" + n_clazz + " }");
		writeLine("}");
	}

	private void outputLinks(Clazz clazz, int n_clazz, int n_type) {
		final LinkType type = (LinkType) this.link_manager.link_type_factory.array.elementAt(n_type);

		final int n_o_l = this.link_manager.element.size();
		for (int temp = n_o_l; --temp >= 0;) {
			final Link link = (Link) this.link_manager.element.elementAt(temp);
			if (link.type == type) {
				if (link.clazz == clazz) {
					if (!link.type.hidden || FrEnd.render_hidden_links) {
						outputLink(link, n_clazz, n_type);
					}
				}
			}
		}
	}

	private void outputLink(Link link, int n_clazz, int n_type) {
		Forget.about(n_type);
		final int total = link.nodes.length;
		for (int i = 0; i < total - 1; i++) {
			final Point3D pos_A = link.nodes[i].pos;
			final Point3D pos_B = link.nodes[i + 1].pos;

			outputLinkSection(link, n_clazz, pos_A, pos_B);
		}
	}

	private void outputLinkSection(Link link, int n_clazz, final Point3D pos_A, final Point3D pos_B) {
		final boolean one = pos_A.x < pos_B.x;

		final Point3D pos_1 = one ? pos_A : pos_B;
		final Point3D pos_2 = one ? pos_B : pos_A;

		final double x = toVRMLCoords(((pos_1.x + pos_2.x) >> 1) - this.middle.x);
		final double y = toVRMLCoords(this.middle.y - ((pos_1.y + pos_2.y) >> 1));
		final double z = toVRMLCoords(((pos_1.z + pos_2.z) >> 1) - this.middle.z);

		final long rx = pos_1.x - pos_2.x;
		final long ry = pos_1.y - pos_2.y;
		final long rz = pos_1.z - pos_2.z;

		final double degree_conversion = 180 / Math.PI;
		final double amp1 = Math.sqrt((rx * rx) + (rz * rz));

		final double theta1;
		final double theta2;
		// double pi_over_2 = (Math.PI / 2);
		// if (amp1 > ry) {
		// theta1 = pi_over_2 - ((ry == 0) ? pi_over_2 : -cvf * Math.atan(ry /
		// amp1));
		// } else {
		theta1 = (ry == 0) ? -degree_conversion * (Math.PI / 2) : -degree_conversion * Math.atan(amp1 / ry);
		// }
		// if (rx > rz) {
		// theta2 = pi_over_2 - ((rx == 0) ? pi_over_2 : -cvf * Math.atan(((double)
		// rx) / rz));
		// } else {
		theta2 = (rx == 0) ? -degree_conversion * (Math.PI / 2) : -degree_conversion * Math.atan(((double) rz) / rx);
		// }

		// if (theta1 < 0) {
		// theta1 += pi_over_2 + pi_over_2;
		// }
		// if (theta2 < 0) {
		// theta2 += pi_over_2 + pi_over_2;
		// }
		// final double theta1 = (ry == 0) ? 90 : -cvf * Math.atan(amp1 / ry);
		// final double theta2 = (rx == 0) ? 90 : -cvf * Math.atan(((double) rz) /
		// rx);
		// final double theta1 = (ry == 0) ? 90 : -cvf * Math.atan(amp1 / ry);
		// final double theta2 = (rx == 0) ? 90 : -cvf * Math.atan(((double) rz) /
		// rx);

		writeLinkObject(link, n_clazz, x, y, z, theta1, theta2);
	}

	private void writeLinkObject(Link link, int n_clazz, final double x, final double y, final double z,
			final double theta1, final double theta2) {
		int a_len = link.getActualLength();
		if (a_len < 1) {
			a_len = 1;
		}
		final double length = toVRMLCoords(a_len);
		final double radius = toVRMLCoords(link.type.radius);
		double divisor;

		final String output_pov_compression = (String) FrEnd.preferences.map
				.get(Preferences.key_output_pov_compression);

		final boolean bulge = output_pov_compression.equals("bulge");
		final boolean point = output_pov_compression.equals("point");

		writeLine("object {");
		if (link.type.compression && bulge) {
			divisor = radius * 2;
			if (divisor < 0.00001) {
				divisor = 0.00001;
			}
			writeLine(" sphere {");
			writeLine("  <0,0,0>, " + emit(radius));
			writeLine("  texture { CL" + n_clazz + " } }");
		} else if (link.type.compression && point) {
			final Node n1 = link.nodes[0];
			final Node n2 = link.nodes[link.nodes.length - 1];
			final int max_r = Math.max(n1.type.radius, n2.type.radius);
			final double barrel = a_len - max_r * 3;
			double proportion_d = (barrel / a_len);
			if (proportion_d < 0.1) {
				proportion_d = 0.1;
			}
			final String proportion = "" + proportion_d;
			writeLine(" union {");
			writeLine("  cylinder { <0,-" + proportion + ",0>, <0," + proportion + ",0>, " + emit(radius));
			writeLine("   texture { CL" + n_clazz + " } }");
			writeLine("  cone { <0,-1.0,0>, 0, <0,-" + proportion + ",0>, " + emit(radius));
			writeLine("   texture { CL" + n_clazz + " } }");
			writeLine("  cone { <0,1.0,0>, 0, <0," + proportion + ",0>, " + emit(radius));
			writeLine("   texture { CL" + n_clazz + " } }");
			writeLine(" }");
			divisor = 2.0;
		} else {
			writeLine(" cylinder { <0,-1.0,0>, <0,1.0,0>, " + emit(radius));
			writeLine("  texture { CL" + n_clazz + " } }");
			divisor = 2.0;
		}

		// writeLine(" texture { CL" + n_clazz + " } }");
		writeLine(" scale <1," + emit12(length / divisor) + ",1 >");
		writeLine(" rotate <0,0," + emit12(theta1) + ">");
		writeLine(" rotate <0," + emit12(theta2) + ",0>");
		writeLine(" translate <" + emit12(x) + "," + emit12(y) + "," + emit12(z) + ">");
		writeLine("}");
	}

	double toVRMLCoords(int v) {
		return v * this.scale_factor;
	}

	public String emit(double f) {
		return WriteFloatingPoint.emit(f, 5, false);
	}

	public String emit(float f) {
		return WriteFloatingPoint.emit(f, 5, false);
	}

	public String emit10(float f) {
		return WriteFloatingPoint.emit(f, 10, false);
	}

	public String emit12(float f) {
		return WriteFloatingPoint.emit(f, 12, false);
	}

	// ouch! TODO: fix this.
	public String emit12(double f) {
		float temp = (float) f;
		if ("NaN".equals("" + temp)) {
			temp = 90F;
		}

		return WriteFloatingPoint.emit(temp, 12, false);
	}

	void writeLine(String s) {
		try {
			this.out.write(s + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}