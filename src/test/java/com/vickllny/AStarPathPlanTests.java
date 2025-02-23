package com.vickllny;

import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.junit.Test;
import org.locationtech.jts.algorithm.distance.DistanceToPoint;
import org.locationtech.jts.algorithm.distance.PointPairDistance;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

public class AStarPathPlanTests {

    static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Test
    public void test() throws MalformedURLException {
        final String path = "/Users/zouqzouq/gis/cd_road/shape/roads.shp";
        final SimpleFeatureCollection featureCollection = loadRoadFeatures(path);
        final Point start = GEOMETRY_FACTORY.createPoint(new Coordinate(103.968919, 30.597531));
        final Point endPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(104.000533, 30.600422));
        final List<Geometry> geometries = planPath(start, endPoint, featureCollection);
    }

    // 加载道路要素的实现
    public static SimpleFeatureCollection loadRoadFeatures(final String path) throws MalformedURLException {
        final File shapefile = new File(path);  // 指定 Shapefile 文件路径
        final Map<String, Object> params = new HashMap<>();
        params.put("url", shapefile.toURI().toURL());

        try {
            // 创建 Shapefile 数据源
            final ShapefileDataStore dataStore = (ShapefileDataStore) DataStoreFinder.getDataStore(params);
            if (dataStore == null) {
                System.out.println("Failed to load shapefile.");
                return null;
            }

            // 获取 Shapefile 中的第一个图层名称
            String typeName = dataStore.getTypeNames()[0];

            // 获取要素源
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);

            // 获取要素集合

            return featureSource.getFeatures();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // A* 算法
    static class AStarAlgorithm {
        // 实现 A* 算法
        // 使用开销函数、启发式函数、以及遍历过程，找到最短路径
        // 节点类，表示路径中的一个节点
        static class Node {
            Geometry geometry;  // 当前节点对应的几何体
            Node parent;        // 父节点，路径回溯时使用
            double g;           // 从起点到当前节点的代价
            double h;           // 从当前节点到目标节点的启发式估计代价
            double f;           // f = g + h，总的代价

            public Node(Geometry geometry, Node parent, double g, double h) {
                this.geometry = geometry;
                this.parent = parent;
                this.g = g;
                this.h = h;
                this.f = g + h;
            }
        }

        // A*算法计算路径
        public List<Geometry> computePath(Point start, Point end, SimpleFeatureCollection roadFeatures) {
            // 初始化开放列表（待探索的节点）和封闭列表（已探索的节点）
            PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
            Set<Node> closedList = new HashSet<>();

            // 将起点添加到开放列表
            Node startNode = new Node(start, null, 0, start.distance(end));
            openList.add(startNode);

            // 开始A*算法
            while (!openList.isEmpty()) {
                Node currentNode = openList.poll();  // 取出f值最小的节点

                // 如果找到了目标节点，回溯路径
                if (currentNode.geometry.equals(end)) {
                    return reconstructPath(currentNode);
                }

                closedList.add(currentNode);

                // 处理相邻的每个节点
                try (final SimpleFeatureIterator iterator = roadFeatures.features()){
                    while (iterator.hasNext()){
                        final SimpleFeature feature = iterator.next();
                        Geometry neighbor = (Geometry) feature.getDefaultGeometry();
                        if (neighbor.equals(currentNode.geometry)) {
                            continue; // 跳过当前节点本身
                        }

                        // 计算当前节点到邻居节点的代价
                        double gCost = currentNode.g + currentNode.geometry.distance(neighbor);
                        double hCost = neighbor.distance(end);  // 启发式函数：从邻居到目标点的估计距离

                        Node neighborNode = new Node(neighbor, currentNode, gCost, hCost);

                        // 如果邻居节点已经在封闭列表中，则跳过
                        if (closedList.contains(neighborNode)) {
                            continue;
                        }

                        // 如果邻居节点不在开放列表中，加入开放列表
                        if (!openList.contains(neighborNode)) {
                            openList.add(neighborNode);
                        } else {
                            // 如果邻居节点已经在开放列表中，检查是否需要更新路径
                            for (Node node : openList) {
                                if (node.geometry.equals(neighborNode.geometry) && node.f > neighborNode.f) {
                                    openList.remove(node);
                                    openList.add(neighborNode);
                                    break;
                                }
                            }
                        }
                    }
                }

            }

            return null;  // 如果没有找到路径，返回null
        }

        // 回溯路径
        private List<Geometry> reconstructPath(Node node) {
            List<Geometry> path = new ArrayList<>();
            while (node != null) {
                path.add(node.geometry);
                node = node.parent;
            }
            Collections.reverse(path);  // 反转路径，因为我们是从目标回溯到起点
            return path;
        }
    }

    // 查找最近的点
    public static Geometry findNearestFeature(Point startPoint, SimpleFeatureCollection roadFeatures) {
        double minDistance = Double.MAX_VALUE;
        Geometry nearestFeature = null;
        try (final SimpleFeatureIterator iterator = roadFeatures.features()){
            while (iterator.hasNext()){
                final SimpleFeature feature = iterator.next();
                final Geometry geometry = (Geometry)feature.getDefaultGeometry();
                final double distance = geometry.distance(startPoint);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestFeature = geometry;
                }
            }
        }
        return nearestFeature;
    }

    // 计算路径
    public static List<Geometry> planPath(Point start, Point end, SimpleFeatureCollection roadFeatures) {
        // 使用 A* 算法计算从 start 到 end 的路径
        AStarAlgorithm aStar = new AStarAlgorithm();
        return aStar.computePath(start, end, roadFeatures);
    }


    // 主逻辑
    public static List<Geometry> executePathPlanning(Point start, Point end, SimpleFeatureCollection roadFeatures) {
        List<Geometry> path = null;

        // 如果起始点不在给定交通要素上
        if (!isPointOnFeature(start, roadFeatures)) {
            // 查找最近的交通要素
            Geometry nearestFeature = findNearestFeature(start, roadFeatures);

            // 确定起始点的垂直点
            Point newStartPoint = findVerticalPointToFeature(start, nearestFeature);

            // 使用新的起始点进行路径规划
            path = planPath(newStartPoint, end, roadFeatures);

            // 如果路径规划失败，尝试多个新的起始点
            int attempt = 1;
            while (path == null && attempt < 5) {
                nearestFeature = findNearestFeature(start, roadFeatures);
                newStartPoint = findVerticalPointToFeature(start, nearestFeature);
                path = planPath(newStartPoint, end, roadFeatures);
                attempt++;
            }
        } else {
            // 如果起始点已经在交通要素上，直接计算路径
            path = planPath(start, end, roadFeatures);
        }

        return path;
    }


    // 判断点是否在交通要素上
    public static boolean isPointOnFeature(Geometry point, SimpleFeatureCollection roadFeatures) {
        try (final SimpleFeatureIterator iterator = roadFeatures.features()){
            while (iterator.hasNext()){
                final SimpleFeature feature = iterator.next();
                final Geometry geometry = (Geometry)feature.getDefaultGeometry();
                if (geometry.contains(point)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 查找点到最近道路要素的垂直点
    public static Point findVerticalPointToFeature(Point startPoint, Geometry feature) {
        // 计算点到要素的垂直投影，返回一个新的点作为起始点
        PointPairDistance pairDistance = new PointPairDistance();
        DistanceToPoint.computeDistance(feature, startPoint.getCoordinate(), pairDistance);
        for (Coordinate coordinate : pairDistance.getCoordinates()) {
            return GEOMETRY_FACTORY.createPoint(coordinate);
        }
        return GEOMETRY_FACTORY.createPoint(feature.getCoordinate());
    }
}
