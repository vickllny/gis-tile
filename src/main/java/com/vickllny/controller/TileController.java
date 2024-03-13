package com.vickllny.controller;

import com.vickllny.utils.TileBoundsCalculator;
import com.wdtinc.mapbox_vector_tile.adapt.jts.JtsAdapter;
import com.wdtinc.mapbox_vector_tile.adapt.jts.TileGeomResult;
import com.wdtinc.mapbox_vector_tile.build.MvtLayerParams;
import no.ecc.vectortile.VectorTileEncoder;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.File;

@RestController
@RequestMapping(value = "/tile")
public class TileController {

    @GetMapping(value = "/tiff/{x}/{y}/{z}")
    public byte[] tiff(@PathVariable("x") final int x,
                         @PathVariable("y") final int y,
                         @PathVariable("z") final int z ){
        return null;
    }

    @GetMapping(value = "/vector/{x}/{y}/{z}")
    public byte[] vector(@PathVariable("x") final int x,
                         @PathVariable("y") final int y,
                         @PathVariable("z") final int z ){
        return null;
    }

    static final File FILE = new File("/Users/vickllny/gis/gadm41_CHN_shp/gadm41_CHN_3.shp");

    static byte[] convertShpToMvt(int x, int y, int z){
        FileDataStore store = null;
        try {
            store = FileDataStoreFinder.getDataStore(FILE);
            final String typeName = store.getTypeNames()[0];
            final SimpleFeatureSource featureSource = store.getFeatureSource(typeName);

            final CoordinateReferenceSystem oldCrs = store.getSchema().getCoordinateReferenceSystem();
            final Envelope envelopeW = TileBoundsCalculator.calculateTileBounds(x, y, z);
            final Envelope envelope = TileBoundsCalculator.convertToCRS(envelopeW, oldCrs);

            // 创建基于 Envelope 范围的查询
//            ReferencedEnvelope queryEnvelope = new ReferencedEnvelope(envelope, featureSource.getSchema().getCoordinateReferenceSystem());

            MvtLayerParams layerParams = new MvtLayerParams(); // Default extent

            TileGeomResult tileGeom = JtsAdapter.createTileGeom(
                    jtsGeom, // Your geometry
                    envelope,
                    geomFactory,
                    layerParams,
                    acceptAllGeomFilter);
        }catch (Exception e){

        } finally {
            if(store != null){
                store.dispose();
            }
        }



    }
}
