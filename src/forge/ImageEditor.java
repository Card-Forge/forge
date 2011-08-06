package forge;

// Code from http://jspdev.blogspot.com/2009/03/java-image-resize-crop.html

import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageEditor {

    public static boolean ResizeImage(String filepath,String resultpath,int max_x,int max_y) {
        boolean ok=true;
        BufferedImage image=null;
        try {
            image = ImageIO.read(new File(filepath));
        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        int thumbWidth = max_x;
        int thumbHeight = max_y;
        double thumbRatio = (double)thumbWidth / (double)thumbHeight;
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        double imageRatio = (double)imageWidth / (double)imageHeight;
        if (thumbRatio < imageRatio) {
          thumbHeight = (int)(thumbWidth / imageRatio);
        } else {
          thumbWidth = (int)(thumbHeight * imageRatio);
        }

        if (imageWidth<max_x && imageHeight<max_y) {
            ImageWrite(image,resultpath);
        }else{
            BufferedImage thumbImage = getScaledInstance(image,thumbWidth,thumbHeight,RenderingHints.VALUE_INTERPOLATION_BICUBIC,true);
            ImageWrite(thumbImage,resultpath);
        }
        return ok;
    }

    public static BufferedImage ResizeImageBuffer(String filepath,int max_x,int max_y) {
        BufferedImage image=null;
        try {
            image = ImageIO.read(new File(filepath));
        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        // determine thumbnail size from WIDTH and HEIGHT
        int thumbWidth = max_x;
        int thumbHeight = max_y;
        double thumbRatio = (double)thumbWidth / (double)thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double)imageWidth / (double)imageHeight;
        if (thumbRatio < imageRatio) {
            thumbWidth = (int)(thumbHeight * imageRatio);

        } else {
            thumbHeight = (int)(thumbWidth / imageRatio);
        }

        if (imageWidth<max_x && imageHeight<max_y) {
            return image;
        }else{
            BufferedImage thumbImage = getScaledInstance(image,thumbWidth,thumbHeight,RenderingHints.VALUE_INTERPOLATION_BICUBIC,true);
            return thumbImage;
        }
    }

    public static BufferedImage ImageCropBuffer(String filepath,int max_x,int max_y,int ox,int oy) {
        BufferedImage image=null;
        try {
            image = ImageIO.read(new File(filepath));
        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        // determine thumbnail size from WIDTH and HEIGHT
        int thumbWidth = max_x;
        int thumbHeight = max_y;
        double thumbRatio = (double)thumbWidth / (double)thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double)imageWidth / (double)imageHeight;
        if (thumbRatio < imageRatio) {
            thumbWidth = (int)(thumbHeight * imageRatio);

        } else {
            thumbHeight = (int)(thumbWidth / imageRatio);
        }
        //BufferedImage thumbImage = resizeTrick(image,thumbWidth,thumbHeight,ox,oy);

        if (imageWidth<max_x && imageHeight<max_y) {
            return image;
        }else{
            BufferedImage thumbImage = getScaledInstance(image,thumbWidth,thumbHeight,RenderingHints.VALUE_INTERPOLATION_BICUBIC,true);
            thumbImage = getCropInstance(thumbImage,max_x,max_y,ox,oy,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            return thumbImage;
        }
    }

    public static boolean ResizeImageCrop(String filepath,String resultpath,int max_x,int max_y) {
        boolean ok=true;
        BufferedImage image=null;
        try {
            image = ImageIO.read(new File(filepath));
        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        int thumbWidth = max_x;
        int thumbHeight = max_y;
        double thumbRatio = (double)thumbWidth / (double)thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double)imageWidth / (double)imageHeight;
        if (thumbRatio < imageRatio) {
            thumbWidth = (int)(thumbHeight * imageRatio);
          
        } else {
            thumbHeight = (int)(thumbWidth / imageRatio);
        }

        if (imageWidth<max_x && imageHeight<max_y) {
            ImageWrite(image,resultpath);
        }else{
            BufferedImage thumbImage = getScaledInstance(image,thumbWidth,thumbHeight,RenderingHints.VALUE_INTERPOLATION_BICUBIC,true);
            thumbImage = getCropInstance(thumbImage,max_x,max_y,Math.round(Math.abs(thumbWidth-max_x)/2.0f),Math.round(Math.abs(thumbHeight-max_y)/2.0f),RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            ImageWrite(thumbImage,resultpath);
        }

        return ok;
    }

    public static boolean ImageCrop(String filepath,String resultpath,int max_x,int max_y,int ox,int oy) {
        boolean ok=true;
        BufferedImage image=null;
        try {
            image = ImageIO.read(new File(filepath));
        } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        // determine thumbnail size from WIDTH and HEIGHT
        int thumbWidth = max_x;
        int thumbHeight = max_y;
        double thumbRatio = (double)thumbWidth / (double)thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double)imageWidth / (double)imageHeight;
        if (thumbRatio < imageRatio) {
            thumbWidth = (int)(thumbHeight * imageRatio);

        } else {
            thumbHeight = (int)(thumbWidth / imageRatio);
        }

        if (imageWidth<max_x && imageHeight<max_y) {
            ImageWrite(image,resultpath);
        }else{
            BufferedImage thumbImage = getScaledInstance(image,thumbWidth,thumbHeight,RenderingHints.VALUE_INTERPOLATION_BICUBIC,true);
            thumbImage = getCropInstance(thumbImage,max_x,max_y,ox,oy,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            ImageWrite(thumbImage,resultpath);
        }

        return ok;
    }

    public static void FileCopy(String spath, String rpath) throws IOException {
        File inputFile = new File(spath);
        File outputFile = new File(rpath);
        InputStream in = new FileInputStream(inputFile);
        OutputStream out = new FileOutputStream(outputFile);
        byte[] buf = new byte[1024];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }


    public static void ImageWrite(BufferedImage image,String rpath) {
       // try {
             try {
            File outChanel = new File(rpath);
            ImageIO.setUseCache(true);
            ImageWriter iwriter = ImageIO.getImageWritersByFormatName("JPG").next();
            ImageOutputStream imgOut = null;
            imgOut = javax.imageio.ImageIO.createImageOutputStream(outChanel);
            iwriter.setOutput(imgOut);
            float quality =  1f;   //JPEG_QUALITY;
            ImageWriteParam imageParams = iwriter.getDefaultWriteParam();
            imageParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imageParams.setCompressionQuality(quality);
            IIOMetadata metaData = iwriter.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), imageParams);
            iwriter.write(metaData, new IIOImage(image, null, null), imageParams);
            imgOut.flush();
            } catch (IOException ex) {
            Logger.getLogger(ImageEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    public static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
            BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            w = img.getWidth();
            h = img.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    public static BufferedImage getCropInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           int ox,
                                           int oy,
                                           Object hint)
    {
        BufferedImage ret;
        ret = img.getSubimage(ox, oy, targetWidth, targetHeight);
        return ret;
    }

}