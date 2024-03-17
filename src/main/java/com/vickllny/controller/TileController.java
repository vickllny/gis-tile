package com.vickllny.controller;

import org.apache.commons.io.FileUtils;
import org.geotools.api.coverage.Coverage;
import org.geotools.api.coverage.grid.GridCoverageWriter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.api.style.*;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.EmptyIntersectionException;
import org.geotools.coverage.processing.Operations;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.StyleBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
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

//    static File TIFF_FILE = new File("/Users/vickllny/gis/china/N43_35_2010LC030/n43_35_2010lc030.tif");
    static File TIFF_FILE = new File("/Users/vickllny/gis/china/N43_35_2010LC030/epsg_3857.tif");
    static String OUTPUT_PNG_BASE_PATH = TIFF_FILE.getParent();

    static int TILE_SIZE = 256;

    @GetMapping(value = "/wmts/{layer}/{style}/{tileMatrixSet}/{tileMatrix}/{tileRow}/{tileCol}.{format}")
    @CrossOrigin(value = "*")
    public ResponseEntity<byte[]> getTile(@PathVariable String layer,
                                          @PathVariable String style,
                                          @PathVariable String tileMatrixSet,
                                          @PathVariable int tileMatrix,
                                          @PathVariable int tileRow,
                                          @PathVariable int tileCol,
                                          @PathVariable String format) throws Exception {

        final String[] strings = tileMatrixSet.split(":");
        final ReferencedEnvelope envelope = getTileEnvelope(Integer.parseInt(strings[1]), tileMatrix, tileRow, tileCol);
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

    public static void main(String[] args) throws IOException, FactoryException {

        if(TIFF_FILE.exists()){
            AbstractGridFormat format = GridFormatFinder.findFormat(TIFF_FILE);
            GridCoverage2DReader reader = format.getReader(TIFF_FILE);
            GridCoverage2D coverage = reader.read(null);


            final CoordinateReferenceSystem WGS = CRS.decode("EPSG:3857");
            final CoordinateReferenceSystem sourceCRS = reader.getCoordinateReferenceSystem();
            System.out.println(String.format("源坐标系为: %s", sourceCRS.getName()));
            GridCoverage2D new2D = (GridCoverage2D) Operations.DEFAULT.resample(coverage, WGS);
            System.err.println(String.format("目标坐标系为: %s", new2D.getCoordinateReferenceSystem().getName()));


            // Write the reprojected coverage to a new TIFF file
            File outputFile = new File("/Users/vickllny/gis/china/N43_35_2010LC030/epsg_3857.tif");
            GeoTiffFormat geoTiffFormat = new GeoTiffFormat();
            GridCoverageWriter writer = geoTiffFormat.getWriter(outputFile);
            writer.write(new2D, null);
            writer.dispose();

        }
    }


    public static byte[] convertTiffToPng(String tiffPath, ReferencedEnvelope envelope) throws Exception {
        // Read the TIFF file
        File tiffFile = new File(tiffPath);
        GridCoverage2DReader reader = new GeoTiffReader(tiffFile);
        GridCoverage2D coverage = reader.read(null);

        // Check the intersection of the TIFF bounds and the specified envelope
        ReferencedEnvelope tiffBounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
        ReferencedEnvelope intersection = new ReferencedEnvelope(tiffBounds.intersection(envelope), envelope.getCoordinateReferenceSystem());

        // If there is no intersection, return a transparent PNG
        if (intersection.isEmpty()) {
            BufferedImage transparentImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            return bufferedImageToByteArray(transparentImage);
        }

        // Create a style based on the number of bands in the TIFF file
        Style style = createStyleForBands(coverage.getNumSampleDimensions());

        // Create a MapContent and add a layer with the TIFF coverage
        MapContent mapContent = new MapContent();
        mapContent.addLayer(new GridReaderLayer(reader, style));

        // Render the TIFF coverage to a BufferedImage
        BufferedImage image = new BufferedImage((int) intersection.getWidth(), (int) intersection.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.setComposite(AlphaComposite.SrcOver);

        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        renderer.paint(g2, new Rectangle(0, 0, image.getWidth(), image.getHeight()), intersection);

        g2.dispose();
        mapContent.dispose();

        // Convert the BufferedImage to a PNG byte array
        return bufferedImageToByteArray(image);
    }

    private static Style createStyleForBands(int numBands) {
        // Create a style based on the number of bands in the TIFF file
        if (numBands == 1) {
            // For single-band images, use a grayscale style
            return createGrayStyle();
        } else {
            // For multi-band images, use a default RGB style
            return createRGBStyle1();
        }
    }

    private static byte[] bufferedImageToByteArray(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private static Style createGrayStyle() {

        ContrastEnhancement ce = styleFactory.contrastEnhancement(filterFactory.literal(1.0), ContrastMethod.NORMALIZE);
        SelectedChannelType sct = styleFactory.createSelectedChannelType(String.valueOf(1), ce);

        // Use the first band for the gray channel
        ChannelSelection sel = styleFactory.channelSelection(sct);
        RasterSymbolizer sym = styleFactory.getDefaultRasterSymbolizer();
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }

    private static Style createRGBStyle1() {

        ContrastEnhancement ce = styleFactory.contrastEnhancement(filterFactory.literal(1.0), ContrastMethod.NORMALIZE);
        SelectedChannelType sctRed = styleFactory.createSelectedChannelType(String.valueOf(1), ce);
        SelectedChannelType sctGreen = styleFactory.createSelectedChannelType(String.valueOf(2), ce);
        SelectedChannelType sctBlue = styleFactory.createSelectedChannelType(String.valueOf(3), ce);

        // Use the first three bands for the RGB channels
        ChannelSelection sel = styleFactory.channelSelection(sctRed, sctGreen, sctBlue);
        RasterSymbolizer sym = styleFactory.getDefaultRasterSymbolizer();
        sym.setChannelSelection(sel);

        return SLD.wrapSymbolizers(sym);
    }



    public static byte[] clip(ReferencedEnvelope envelope, File tiffFile, String outputPNGPath) throws Exception {
        // 读取TIFF文件
        GridCoverage2DReader reader = null;
        GridCoverage2D coverage = null;
        MapContent mapContent = null;
        Graphics2D graphics = null;
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

            final File file = new File(outputPNGPath);
            final BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            graphics = image.createGraphics();

            final StreamingRenderer renderer = new StreamingRenderer();
            mapContent = new MapContent();
            mapContent.addLayer(new GridReaderLayer(reader, createRGBStyle()));
            renderer.setMapContent(mapContent);



            renderer.paint(graphics, new Rectangle(TILE_SIZE, TILE_SIZE), envelope);


            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }

            ImageIO.write(image, "PNG", file);
//
//            CoverageProcessor processor = CoverageProcessor.getInstance();
//
//            // An example of manually creating the operation and parameters we want
//            final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
//            param.parameter("Source").setValue(coverage);
//            param.parameter("Envelope").setValue(envelope);
//
//            final GridCoverage2D doOperation = (GridCoverage2D) processor.doOperation(param);
//
//            // Render the coverage to a BufferedImage
//            GridCoverageRenderer renderer = new GridCoverageRenderer(null, envelope, null, null);
////            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//            // Create a default RasterSymbolizer
//            StyleBuilder sb = new StyleBuilder();
//            RasterSymbolizer symbolizer = sb.createRasterSymbolizer();
//            renderer.paint(graphics, doOperation,  symbolizer);
//            ImageIO.write(image, "PNG", file);

            return FileUtils.readFileToByteArray(file);
        } finally {
            if(reader != null){
                reader.dispose();
            }
            if(coverage != null){
                coverage.dispose(true);
            }
            if(mapContent != null){
                mapContent.dispose();
            }
            if(graphics != null){
                graphics.dispose();
            }
        }
    }

    static StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory();
    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory();

    public static Style createRGBStyle() {



        // 创建一个栅格符号生成器
        RasterSymbolizer rasterSymbolizer = styleFactory.createRasterSymbolizer();

        final ContrastEnhancement ce = styleFactory.createContrastEnhancement();
        // 创建并设置通道选择
        SelectedChannelType redChannel = styleFactory.createSelectedChannelType("1", ce);
        SelectedChannelType greenChannel = styleFactory.createSelectedChannelType("2", ce);
        SelectedChannelType blueChannel = styleFactory.createSelectedChannelType("3", ce);
        ChannelSelection channelSelection = styleFactory.createChannelSelection(redChannel, greenChannel, blueChannel);
        rasterSymbolizer.setChannelSelection(channelSelection);

        // 创建规则并将栅格符号生成器添加到规则中
        Rule rule = styleFactory.createRule();
        rule.symbolizers().add(rasterSymbolizer);

        // 创建特征类型样式并将规则添加到其中
        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(rule);

        // 将特征类型样式添加到样式中
        Style style = styleFactory.createStyle();
        style.featureTypeStyles().add(fts);

        return style;
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

    public static ReferencedEnvelope getTileEnvelope(int tileMatrixSet, int tileMatrix, int tileRow, int tileCol) throws Exception {
        final double[] bounds = tileBounds(tileCol, tileRow, tileMatrix, tileMatrixSet);

        // 创建并返回ReferencedEnvelope
        return new ReferencedEnvelope(bounds[0], bounds[2], bounds[1], bounds[3], CRS.decode("EPSG:" + tileMatrixSet));
    }

    static final File FILE = new File("/Users/vickllny/gis/gadm41_CHN_shp/gadm41_CHN_3.shp");

    static double INITIAL_RESOLUTION = (Math.PI * 2 * 6378137) / TILE_SIZE;

    public static double[] tileBounds(final int tileCol,
                                    final int tileRow,
                                    final int tileMatrix,
                                    final int crsCode){
        if(crsCode == 3857){
            return tileBounds3857(tileCol, tileRow, tileMatrix);
        }else if(crsCode == 4326){
            return tileBounds4326(tileCol, tileRow, tileMatrix);
        }
        throw new RuntimeException("Unsupported tileMatrixSet: " + crsCode);
    }

    public static double[] tileBounds3857(int tileCol, int tileRow, int tileMatrix) {
        double resolution = INITIAL_RESOLUTION / Math.pow(2, tileMatrix); // 分辨率
        double originShift = 2 * Math.PI * 6378137 / 2.0; // 原点偏移

        double minX = tileCol * TILE_SIZE * resolution - originShift;
        double maxY = originShift - tileRow * TILE_SIZE * resolution;
        double maxX = (tileCol + 1) * TILE_SIZE * resolution - originShift;
        double minY = originShift - (tileRow + 1) * TILE_SIZE * resolution;

        return new double[]{minX, minY, maxX, maxY};
    }

    public static double[] tileBounds4326(int tileCol, int tileRow, int tileMatrix) {
        double resolution = 180 / (TILE_SIZE * Math.pow(2, tileMatrix)); // 分辨率

        double minX = tileCol * TILE_SIZE * resolution - 180;
        double maxY = 90 - tileRow * TILE_SIZE * resolution;
        double maxX = (tileCol + 1) * TILE_SIZE * resolution - 180;
        double minY = 90 - (tileRow + 1) * TILE_SIZE * resolution;

        return new double[]{minX, minY, maxX, maxY};
    }



}
