package GraphStructure;

import Data.FilePaths;
import Data.Highway;
import Data.HighwayType;
import Util.Distance;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.*;
import java.util.*;

public class GraphManager {
    private final String pbfPath = FilePaths.pbfBW;
    private final String binaryPath = FilePaths.binBW;

    private double[][] nodes;
    private int[][] edges;
    private int[] offset;

    HashMap<Long, double[]> nodeLookup;

    private int wayCount = 0;
    private int nodeCount = 0;

    // nodes currently : 2969443
    private int numNodes = 39472043;
    private int numEdges = 52325469;

    PbfIterator iterator;
    InputStream stream;

    public GraphManager() {
        offset = new int[numNodes];
        Arrays.fill(offset, -1);

        nodeLookup = new HashMap<>();
    }

    public void parseFromPbf() {
        try {
            stream = new FileInputStream(pbfPath);
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
            System.out.println("retrieved edges between all relevant nodes");

            System.out.println(nodeCount);
            System.out.println(wayCount);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("File could not be found!");
        } finally {
            try {
                stream.close();
                System.out.println(String.format("nodes: %-15s edges: %-15s",
                        numNodes,
                        numEdges
                ));
            } catch (IOException e) {
                System.out.println("Closing of InputStream failed!");
            }
        }
    }

    public void retrieveRelevantNodes() {
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Way) {
                OsmWay currentEdge = (OsmWay) container.getEntity();
                if (Highway.isHighway(OsmModelUtil.getTagsAsMap(currentEdge).get("highway"))) {
                    wayCount++;
                    nodeCount += (2 * currentEdge.getNumberOfNodes()) - 2;
                    for (int i = 0; i < currentEdge.getNumberOfNodes(); i++) {
                        nodeLookup.put(currentEdge.getNodeId(i), new double[]{});
                        System.out.println("relevant node added: " + currentEdge.getNodeId(i));
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
                            osmNode.getId(),
                            osmNode.getLatitude(),
                            osmNode.getLongitude(),
                    };
                    nodeLookup.put(osmNode.getId(), nodeData);
                    System.out.println("data for node " + osmNode.getId() + ": "
                            + "lat: " + nodeData[1]
                            + "lon: " + nodeData[2]);
                }
            }
        }
    }

    private void localiseAndSortNodes() {
        nodes = new double[3][nodeCount];
        List<double[]> nodeList = new ArrayList<>(nodeLookup.values());
        Collections.sort(nodeList, (a, b) -> (Double.compare(a[0], b[0])));
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
        edges = new int[3][wayCount];
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Way) {
                OsmWay currentWay = (OsmWay) container.getEntity();
                if (Highway.isHighway(OsmModelUtil.getTagsAsMap(currentWay).get("highway"))) {
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

            edges[0][i] = (int) node1[0]; // starting node
            edges[1][i] = (int) node2[0]; // target node
            edges[2][i] = (int) Distance.calculateDistance(node1[1], node1[2], node2[1], node2[2]); //distance
        }
    }

    private void sortEdges() {
        java.util.Arrays.sort(edges, (a, b) -> (Integer.compare(a[0], b[0])));
    }

    private void retrieveLeisurePOIs() {
        edges = new int[3][wayCount];
        iterator = new PbfIterator(stream, false);
        for (EntityContainer container : iterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode node = (OsmNode) container.getEntity();
                Map<String, String> tags = OsmModelUtil.getTagsAsMap(node);
                String amenity = tags.get("amenity");
                // TODO: check for desired leisures and add to a leisureList
                boolean isPub = amenity != null && amenity.equals("pub");
                if (isPub) {
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