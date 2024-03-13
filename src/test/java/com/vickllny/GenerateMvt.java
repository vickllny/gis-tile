//package com.vickllny;
//
//import com.wdtinc.mapbox_vector_tile.VectorTile;
//import com.wdtinc.mapbox_vector_tile.build.MvtLayerBuild;
//import com.wdtinc.mapbox_vector_tile.build.MvtLayerProps;
//import org.geotools.api.feature.simple.SimpleFeature;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.geotools.data.simple.SimpleFeatureIterator;
//import org.geotools.geometry.jts.ReferencedEnvelope;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class GenerateMvt {
//
//    public static byte[] generateMvt(SimpleFeatureCollection featureCollection, ReferencedEnvelope tileBounds) throws IOException {
//        // 创建一个用于存储MVT瓦片数据的输出流
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//
//        // 创建MVT图层
//        VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder("layer_name", 4096);
//        MvtLayerProps layerProps = new MvtLayerProps();
//
//        // 将GeoTools的SimpleFeature转换为MVT特征
//        try (SimpleFeatureIterator iterator = featureCollection.features()) {
//            while (iterator.hasNext()) {
//                SimpleFeature feature = iterator.next();
//                List<VectorTile.Tile.Feature> mvtFeatures = new ArrayList<>();
//                MvtFeatureConvert.convertToMvtFeature(feature, tileBounds, layerProps, mvtFeatures);
//                mvtFeatures.forEach(layerBuilder::addFeatures);
//            }
//        }
//
//        // 构建MVT瓦片
//        VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
//        tileBuilder.addLayers(MvtLayerBuild.build(layerBuilder, layerProps));
//
//        // 将MVT瓦片写入输出流
//        VectorTile.Tile mvt = tileBuilder.build();
//        mvt.writeTo(out);
//
//        return out.toByteArray();
//    }
//
//}
