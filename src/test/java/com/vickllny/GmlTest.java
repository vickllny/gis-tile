package com.vickllny;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.gml3.v3_2.GMLConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

public class GmlTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmlTest.class);

    @Test
    public void readGml32(){
        final File file = new File("/Users/vickllny/gis/gml/tiger_roads_v3.2.xml");
        try (final InputStream is = Files.newInputStream(file.toPath())){
            final GMLConfiguration gmlConfig = new GMLConfiguration(true);
            final Parser parser = new Parser(gmlConfig);
            parser.setStrict(false);
            final Object object = parser.parse(is);
            System.out.println(object);
            if(object instanceof HashMap){
                Map<String, Object> map = (Map) object;
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    final Object value = entry.getValue();
                    if(value instanceof Collection){
                        List<SimpleFeature> list = new ArrayList<>((Collection<? extends SimpleFeature>) value);
                        for (final SimpleFeature feature : list) {
                            LOGGER.debug("1111");
                        }
                    }
                }
            }
        }catch (Exception e){
            LOGGER.error("解析异常", e);
        }
    }
}
