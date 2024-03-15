package com.vickllny.controller;

import org.apache.commons.io.FileUtils;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

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

    static File TIFF_FILE = new File("/Users/vickllny/gis/china/N43_35_2010LC030/n43_35_2010lc030.tif");
    static String OUTPUT_PNG_BASE_PATH = TIFF_FILE.getParent();

    static int TILE_SIZE = 256;

    @GetMapping(value = "/wmts/{layer}/{style}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}.{format}")
    public ResponseEntity<byte[]> getTile(@PathVariable String layer,
                                          @PathVariable String style,
                                          @PathVariable String tileMatrixSet,
                                          @PathVariable int tileMatrix,
                                          @PathVariable int tileRow,
                                          @PathVariable int tileCol,
                                          @PathVariable String format) throws Exception {

        final String[] strings = tileMatrixSet.split(":");
        final ReferencedEnvelope envelope = getTileEnvelope(Integer.parseInt(strings[1]), tileMatrix, tileRow, tileCol, TILE_SIZE);
        final String outputPngPath = OUTPUT_PNG_BASE_PATH + File.separator + tileMatrixSet.replace(":", "_") + "_" + tileMatrix + File.separator + tileCol + File.separator + tileRow + "." + format;

        try {
            final byte[] bytes = clip(envelope, TIFF_FILE, outputPngPath);
            // Set the content type based on the requested format
            MediaType contentType = getContentType(format);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        }catch (EmptyIntersectionException e){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    public static byte[] clip(ReferencedEnvelope envelope, File tiffFile, String outputPNGPath) throws IOException, TransformException, IOException, FactoryException, TransformException {
        // 读取TIFF文件
        GridCoverage2DReader reader = null;
        GridCoverage2D coverage = null;
        GridCoverage2D clippedCoverage = null;
        ReferencedEnvelope readEnvelope = null;
        try {
            reader = new GeoTiffReader(tiffFile);
            coverage = reader.read(null);
            // 获取TIFF文件的坐标系
            final CoordinateReferenceSystem tiffCRS = coverage.getCoordinateReferenceSystem();

            // 如果ReferencedEnvelope的坐标系与TIFF文件的坐标系不一致，则进行转换
            if (!CRS.equalsIgnoreMetadata(envelope.getCoordinateReferenceSystem(), tiffCRS)) {
                MathTransform transform = CRS.findMathTransform(envelope.getCoordinateReferenceSystem(), tiffCRS, true);
                envelope = new ReferencedEnvelope(JTS.transform(envelope, transform), tiffCRS);
            }

            readEnvelope = coverage.getGridGeometry().getEnvelope2D();

            final GridEnvelope gridRange = coverage.getGridGeometry().getGridRange();

            double resolutionX  = envelope.getSpan(0) / gridRange.getSpan(0);
            double resolutionY  = envelope.getSpan(1) / gridRange.getSpan(1);


            int xOff = (int) Math.floor((envelope.getMinX() - readEnvelope.getMinX()) / resolutionX);
            int yOff = (int) Math.floor((envelope.getMaxY() - readEnvelope.getMaxY()) / resolutionY);

            // 缩放到指定宽高
            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            GridCoverage2D scaledCoverage = (GridCoverage2D) coverage.resample(width, height, hints);


            // 使用转换后的范围切割TIFF文件
            clippedCoverage = (GridCoverage2D) Operations.DEFAULT.crop(coverage, envelope);

            // 将切割后的影像转换为BufferedImage
            final RenderedImage renderedImage = clippedCoverage.getRenderedImage().getData();
            // 将BufferedImage保存为PNG文件
            final File outputPNGFile = new File(outputPNGPath);
            if(!outputPNGFile.getParentFile().exists()){
                outputPNGFile.getParentFile().mkdirs();
            }
            ImageIO.write(renderedImage, "png", outputPNGFile);
            return FileUtils.readFileToByteArray(outputPNGFile);
        } finally {
            if(reader != null){
                reader.dispose();
            }
            if(coverage != null){
                coverage.dispose(false);
            }
            if(clippedCoverage != null){
                clippedCoverage.dispose(false);
            }
        }
    }

    private double calculateResolution(int tileMatrix) {
        // Calculate the resolution based on the tile matrix level
        return 156543.03390625 / Math.pow(2, tileMatrix);
    }

    private BufferedImage getBufferedImage(GridCoverage2D coverage) {
        // Convert GridCoverage2D to BufferedImage
        return coverage.getRenderedImage() instanceof BufferedImage ?
                (BufferedImage) coverage.getRenderedImage() :
                convertRenderedImage(coverage.getRenderedImage());
    }

    private BufferedImage convertRenderedImage(java.awt.image.RenderedImage img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        ColorModel cm = img.getColorModel();
        int width = img.getWidth();
        int height = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable<String, Object> properties = new Hashtable<>();
        String[] keys = img.getPropertyNames();
        if (keys != null) {
            for (String key : keys) {
                properties.put(key, img.getProperty(key));
            }
        }
        BufferedImage result = new BufferedImage(cm, raster, isAlphaPremultiplied, properties);
        img.copyData(raster);
        return result;
    }

    private MediaType getContentType(String format) {
        switch (format.toLowerCase()) {
            case "png":
                return MediaType.IMAGE_PNG;
            case "jpg":
            case "jpeg":
                return MediaType.IMAGE_JPEG;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    public static ReferencedEnvelope getTileEnvelope(int tileMatrixSet, int tileMatrix, int tileRow, int tileCol, int tileSize) throws Exception {
        // 定义瓦片矩阵集的总范围和初始分辨率
        double minX, minY, maxX, maxY, resolution;
        CoordinateReferenceSystem crs;

        if (tileMatrixSet == 4326) {
            crs = CRS.decode("EPSG:4326");
            minX = -180.0;
            minY = -90.0;
            maxX = 180.0;
            maxY = 90.0;
            resolution = 0.703125; // 假设级别0的分辨率为0.703125度/像素
        } else if (tileMatrixSet == 3857) {
            crs = CRS.decode("EPSG:3857");
            minX = -20037508.34;
            minY = -20037508.34;
            maxX = 20037508.34;
            maxY = 20037508.34;
            resolution = 156543.033928041; // 假设级别0的分辨率为156543.033928041米/像素
        } else {
            throw new IllegalArgumentException("Unsupported tile matrix set.");
        }

        // 计算当前级别的分辨率
        resolution /= Math.pow(2, tileMatrix);

        // 计算瓦片的地理范围
        double tileMinX = minX + tileCol * resolution * tileSize;
        double tileMaxX = tileMinX + tileSize * resolution;
        double tileMaxY = maxY - tileRow * resolution * tileSize;
        double tileMinY = tileMaxY - tileSize * resolution;

        // 创建并返回ReferencedEnvelope
        return new ReferencedEnvelope(tileMinX, tileMaxX, tileMinY, tileMaxY, crs);
    }

    static final File FILE = new File("/Users/vickllny/gis/gadm41_CHN_shp/gadm41_CHN_3.shp");


}
