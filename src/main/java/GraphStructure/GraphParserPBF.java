package GraphStructure;

import Data.AmenityHandling;
import Data.FilePaths;
import Data.HighwayHandling;
import Util.Distance;
import Util.PathTypes;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import gnu.trove.list.TLongList;

import java.io.*;
import java.util.*;

import static GraphStructure.GraphWriter.WriteToLineFile;

public class GraphParserPBF {
    //TODO: change to germany when needed
    private final String pbfPath = FilePaths.pbfBW;
    private final String binaryPathNodes = FilePaths.binBWNodes;
    private final String binaryPathEdges = FilePaths.binBWEdges;
    private final String binaryPathOffsets = FilePaths.binBWOffsets;

    PbfIterator iterator;
    InputStream stream;

    HashMap<Long, double[]> nodeLookup;
    private double[][] nodes;
    private int[][] edges;
    private int wayCount = 0;
    private int nodeCount = 0;

    public GraphParserPBF() {
        nodeLookup = new HashMap<>();
    }

    public void parseStrohm( ) throws IOException {
        // Set of legal highway tags.
        List<String> car = Arrays.asList(
                "motorway", "trunk", "primary", "secondary", "tertiary", "unclassified",
                "residential", "service", "motorway_link", "trunk_link", "primary_link", "secondary_link",
                "tertiary_link", "living_street");
        Set<String> legalStreetsCAR = new HashSet<>(car);

        List<String> ped = Arrays.asList("residential", "service", "living_street", "pedestrian", "track",
                "footway", "bridleway", "steps", "path", "cycleway", "trunk", "primary", "secondary", "tertiary",
                "unclassified", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "road");
        Set<String> legalStreetsPEDSTRIAN = new HashSet<>(ped);

        // Count number of ways; needed for array creation.
        int numberOfEdges = CountNumberOfEdges(legalStreetsPEDSTRIAN);

        edges = new int[numberOfEdges][4];
        int edgesPos = 0;
        int numberNodes = 0;
        HashMap<Long, Integer> nodeMap = new HashMap<>();

        // Reset iterator
        InputStream input = new FileInputStream(pbfPath);
        OsmIterator iterator = new PbfIterator(input, true);

        // Create edges and store node IDs
        for (EntityContainer container : iterator) {
            String type = container.getType().toString();
            if (type.equals("Way")) {
                Map<String, String> WayTags = OsmModelUtil.getTagsAsMap(container.getEntity());
                String highway = WayTags.get("highway");
                String sidewalk = WayTags.get("sidewalk");
                String motorroad = WayTags.get("motorroad");

                if ((sidewalk != null && (sidewalk.equals("yes") || sidewalk.equals("right") || sidewalk.equals("left")
                        || sidewalk.equals("both")))
                        || ((motorroad == null || !motorroad.equals("yes")) && highway != null
                        && legalStreetsPEDSTRIAN.contains(highway))) {
                    TLongList wayNodes = OsmModelUtil.nodesAsList((OsmWay) container.getEntity());
                    if (!nodeMap.containsKey(wayNodes.get(0))) {
                        nodeMap.put(wayNodes.get(0), numberNodes);
                        numberNodes++;
                    }
                    for (int i = 1; i < wayNodes.size(); i++) {
                        if (!nodeMap.containsKey(wayNodes.get(i))) {
                            nodeMap.put(wayNodes.get(i), numberNodes);
                            numberNodes++;
                        }
                        edges[edgesPos][0] = edgesPos;
                        edges[edgesPos][1] = nodeMap.get(wayNodes.get(i - 1));
                        edges[edgesPos][2] = nodeMap.get(wayNodes.get(i));
                        edgesPos++;
                    }
                    for (int i = wayNodes.size() - 1; i > 0; i--) {
                        edges[edgesPos][0] = edgesPos;
                        edges[edgesPos][1] = nodeMap.get(wayNodes.get(i));
                        edges[edgesPos][2] = nodeMap.get(wayNodes.get(i - 1));
                        edgesPos++;
                    }
                }
            }
        }

        System.out.println("nodemap size: " + nodeMap.size());
        nodes = new double[3][nodeMap.size()];

        // Reset iterator
        input.close();
        input = new FileInputStream(pbfPath);
        iterator = new PbfIterator(input, true);

        // Get node information.
        for (EntityContainer container : iterator) {
            String type = container.getType().toString();
            if (type.equals("Node")) {
                OsmNode node = (OsmNode) container.getEntity();
                long ID = node.getId();
                if (nodeMap.containsKey(ID)) {
                    int pos = nodeMap.get(ID);
                    nodes[0][pos] = pos;
                    nodes[1][pos] = node.getLatitude();
                    nodes[2][pos] = node.getLongitude();
                }
            }
        }

        // Calculate distance for each edge and calculate time needed to travel along this edge.
        for (int[] edge : edges) {
            double startNodeLat = nodes[1][edge[1]];
            double startNodeLng = nodes[2][edge[1]];
            double destNodeLat = nodes[1][edge[2]];
            double destNodeLng = nodes[2][edge[2]];
            double dist = Distance.euclideanDistance(startNodeLat, startNodeLng, destNodeLat, destNodeLng);

            edge[3] = (int) (dist*10000);
        }
    }

    private int CountNumberOfEdges(Set<String> legalStreets) throws IOException {
        int numWays = 0;
        InputStream input = new FileInputStream(pbfPath);
        OsmIterator iterator = new PbfIterator(input, true);
        for (EntityContainer container : iterator) {
            String type = container.getType().toString();
            if (type.equals("Way")) {
                Map<String, String> WayTags = OsmModelUtil.getTagsAsMap(container.getEntity());
                OsmWay way = (OsmWay) container.getEntity();
                String highway = WayTags.get("highway");
                if (highway != null && legalStreets.contains(highway)) {
                    int NumberOfNodes = way.getNumberOfNodes();
                    numWays += (2 * NumberOfNodes) - 2;
                }
            }
        }
        input.close();
        return numWays;
    }

    public void parseFromPbf() {
        try {
            parseStrohm();

/*            stream = new FileInputStream(pbfPath);
            retrieveRelevantNodes();
            System.out.println("Looked up relevant nodes");

            stream = new FileInputStream(pbfPath);
            retrieveDataForNodes();
            System.out.println("Added geo coordinates");

            stream = new FileInputStream(pbfPath);
            localiseAndSortNodes();
            System.out.println("localised nodes");

            stream = new FileInputStream(pbfPath);
            retrieveEdgesBetweenNodes();
            sortEdges();
            System.out.println("retrieved edges between all relevant nodes");*/

            WriteToLineFile(edges, nodes, binaryPathNodes, binaryPathEdges);
           // serializeGraph();
            System.out.println("graph serialized");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("File could not be found!");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // stream.close();
            System.out.println(String.format("nodes: %-15s edges: %-15s",
                    nodeCount,
                    wayCount
            ));
        }
    }

    public void retrieveRelevantNodes() {
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Way) {
                OsmWay currentEdge = (OsmWay) container.getEntity();
                if (HighwayHandling.isHighway(OsmModelUtil.getTagsAsMap(currentEdge).get("highway"))) {
                    wayCount++;
                    nodeCount += (2 * currentEdge.getNumberOfNodes()) - 2;
                    for (int i = 0; i < currentEdge.getNumberOfNodes(); i++) {
                        nodeLookup.put(currentEdge.getNodeId(i), new double[]{});
                    }
                }
            }
        }
    }

    private void retrieveDataForNodes() {
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode osmNode = (OsmNode) container.getEntity();
                if (nodeLookup.containsKey(osmNode.getId())) {

                    double[] nodeData = new double[]{
                            (double) osmNode.getId(),
                            osmNode.getLatitude(),
                            osmNode.getLongitude(),
                    };
                    nodeLookup.put(osmNode.getId(), nodeData);
                }
            }
        }
    }

    private void localiseAndSortNodes() {
        nodes = new double[3][nodeCount];
        List<double[]> nodeList = new ArrayList<>(nodeLookup.values());
        nodeList.sort((a, b) -> (Double.compare(a[0], b[0])));
        for (int i = 0; i < nodeList.size(); i++) {
            nodes[0][i] = nodeList.get(i)[1];   // latitude
            nodes[1][i] = nodeList.get(i)[2];   // longitude
            // store id-mapping in lookup-table
            nodeLookup.put((long) nodeList.get(i)[0],
                    new double[]{
                            (double) i, // serves as localId
                            (double) 0,
                            (double) 0
                    });
        }
        nodeList.clear();
    }

    private void retrieveEdgesBetweenNodes() {
        edges = new int[5][wayCount];
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Way) {
                OsmWay currentWay = (OsmWay) container.getEntity();
                if (HighwayHandling.isHighway(OsmModelUtil.getTagsAsMap(currentWay).get("highway"))) {
                    for (int i = 0; i < currentWay.getNumberOfNodes(); i++) {
                        convertToEdgeStructure(currentWay);
                    }
                }
            }
        }
    }

    private void convertToEdgeStructure(OsmWay way) {
        for (int i = 0; i < way.getNumberOfNodes() - 1; i++) {
            double[] node1 = nodeLookup.get(way.getNodeId(i));
            double[] node2 = nodeLookup.get(way.getNodeId(i + 1));
            float[] edgeType = PathTypes.getMaxSpeed(way);

            edges[0][i] = (int) node1[0]; // starting node
            edges[1][i] = (int) node2[0]; // target node
            edges[2][i] = (int) Distance.euclideanDistance(node1[1], node1[2], node2[1], node2[2]); //distance
            edges[3][i] = (int) edgeType[0];
            edges[4][i] = (int) edgeType[1];

            if (!PathTypes.isOneWay(way)) {
                convertToReverseEdges(way);
            }
        }
    }

    private void convertToReverseEdges(OsmWay way) {
        for (int i = way.getNumberOfNodes() - 1; i > 0; i--) {
            double[] node1 = nodeLookup.get(way.getNodeId(i));
            double[] node2 = nodeLookup.get(way.getNodeId(i + 1));
            float[] edgeType = PathTypes.getMaxSpeed(way);

            edges[0][i] = (int) node1[0];
            edges[1][i] = (int) node2[0];
            edges[2][i] = (int) Distance.euclideanDistance(node1[1], node1[2], node2[1], node2[2]);
            edges[3][i] = (int) edgeType[0];
            edges[4][i] = (int) edgeType[1];
        }
    }

    private void sortEdges() {
        java.util.Arrays.sort(edges, (a, b) -> (Integer.compare(a[0], b[0])));
    }

    private void retrieveAmenityPOIs() {
        edges = new int[3][wayCount];
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);
                String amenity = tags.get("amenity");
                // TODO: check for desired leisures and add to a leisureList
                if (AmenityHandling.isAmenity(amenity)) {
                    System.out.println(String.format("%-15s %-40s %-15f %-15f",
                            node.getId(),
                            tags.get("name"),
                            node.getLatitude(),
                            node.getLongitude()
                    ));
                }
            }
        }
    }

}