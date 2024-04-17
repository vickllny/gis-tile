package com.vickllny;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;

public class CrsTransformTest {
    
    @Test
    public void test() throws FactoryException, TransformException {
        final CoordinateReferenceSystem crs4490 = CRS.decode("EPSG:4490", true);
        final CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326", true);
        final CoordinateReferenceSystem crs3857 = CRS.decode("EPSG:3857", true);

        final Coordinate coordinate = new Coordinate(101.301241, 29.657412);
        final Coordinate coordinate1 = new Coordinate();
        JTS.transform(coordinate, coordinate1, CRS.findMathTransform(crs4326, crs3857));
        System.out.println(coordinate1);

    }
}
