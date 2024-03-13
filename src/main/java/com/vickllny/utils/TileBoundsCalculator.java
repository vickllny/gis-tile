package com.vickllny.utils;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

public class TileBoundsCalculator {

    private static final double RADIUS = 6378137; // 地球半径（米）
    private static final double HALF_CIRCUMFERENCE = Math.PI * RADIUS;

    /**
     * 3857 根据xyz计算边界范围
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static Envelope calculateTileBounds(int x, int y, int z) {
        double tileSize = 2 * Math.PI * RADIUS / Math.pow(2, z);
        double minX = x * tileSize - HALF_CIRCUMFERENCE;
        double minY = HALF_CIRCUMFERENCE - (y + 1) * tileSize;
        double maxX = (x + 1) * tileSize - HALF_CIRCUMFERENCE;
        double maxY = HALF_CIRCUMFERENCE - y * tileSize;

        return new Envelope(minX, maxX, minY, maxY);
    }

    /**
     * Web墨卡托投影 边界 转84经纬度边界
     * @param envelope
     * @return
     */
    public static Envelope convertToWGS84(Envelope envelope) throws FactoryException, TransformException {
        return convertToCRS(envelope, CRS.decode("EPSG:4326"));
    }

    public static Envelope convertToCRS(Envelope envelope, CoordinateReferenceSystem targetCRS) throws FactoryException, TransformException {
        CoordinateReferenceSystem CRS_3857 = CRS.decode("EPSG:3857");

        // 获取转换对象
        MathTransform transform = CRS.findMathTransform(CRS_3857, targetCRS);

        // 转换四个角点坐标
        Coordinate lowerLeft = new Coordinate(envelope.getMinX(), envelope.getMinY());
        Coordinate upperRight = new Coordinate(envelope.getMaxX(), envelope.getMaxY());
        JTS.transform(lowerLeft, lowerLeft, transform);
        JTS.transform(upperRight, upperRight, transform);

        // 返回转换后的边界
        return new Envelope(lowerLeft, upperRight);
    }


}
