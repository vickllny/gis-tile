package com.vickllny;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

public class ReadShapeFileTest {

    @Test
    public void test() throws IOException {
        final File file = new File("/Users/vickllny/gis/gadm41_CHN_shp/gadm41_CHN_3.shp");

        final Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);

        // 获取 Shapefile 中的要素集合
        String typeName = dataStore.getTypeNames()[0];
        FeatureSource<?, ?> featureSource = dataStore.getFeatureSource(typeName);


        // 创建一个查询对象（这里是查询全部数据，不加任何条件）
        Query query = new Query(typeName);

        // 获取要素集合
        FeatureCollection<?, ?> featureCollection = featureSource.getFeatures(query);

        // 遍历要素集合
        try (SimpleFeatureIterator iterator = (SimpleFeatureIterator) featureCollection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                System.out.println("Feature ID: " + feature.getID());
                System.out.println("Geometry: " + feature.getDefaultGeometry());
                System.out.println("Attributes: " + feature.getAttributes());
                System.out.println("----------");
            }
        }

        // 关闭 DataStore
        dataStore.dispose();
    }
}
