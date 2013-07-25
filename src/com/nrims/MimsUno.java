/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims;
/*package libreoffice ;*/

import java.awt.image.BufferedImage;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import com.sun.star.accessibility.AccessibleRole;
import com.sun.star.awt.Point;
import com.sun.star.awt.Size;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.drawing.XShape;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.WrapTextMode;
import com.sun.star.text.TextContentAnchorType;
import com.sun.star.uno.XComponentContext;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XNameAccess;
import com.sun.star.lib.uno.adapter.ByteArrayToXInputStreamAdapter;
import com.sun.star.container.XNamed;
import com.sun.star.text.XTextFrame;
import com.sun.star.text.XTextFramesSupplier;
import com.sun.star.accessibility.XAccessible;
import com.sun.star.accessibility.XAccessibleComponent;
import com.sun.star.accessibility.XAccessibleContext;
import com.sun.star.awt.XWindow;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextGraphicObjectsSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XInterface;
import java.awt.MouseInfo;

/**
 *
 * @author wang2
 */
public class MimsUno {
    private XComponentContext context;
    private XMultiComponentFactory xMCF;
    private XTextDocument xTextDocument;
    public MimsUno(){
        try {
            context = Bootstrap.bootstrap();
            xMCF = context.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", context);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            //Get the document with focus here
            XComponent currentDocument = desktop.getCurrentComponent();
            
            // Querying for the text interface
            xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //if no current document or current document is not a writer document, create a new writer
            if (xTextDocument == null) {
                XComponentLoader xComponentLoader = (XComponentLoader) UnoRuntime.queryInterface(
                        XComponentLoader.class, desktop);
                PropertyValue[] loadProps = new PropertyValue[0];
                currentDocument = xComponentLoader.loadComponentFromURL("private:factory/swriter", "_blank", 0, loadProps);
                xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                        XTextDocument.class, currentDocument);
            }
        }catch (Exception e){
            System.out.println("Failure to connect");
        }
    }
    /**
     * method to convert a java Image to a byte array representing a PNG image
     *
     * @param image desired image to convert
     * @return a byte array representing the given image
     */
    private byte[] imageToByteArray(Image image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedImage bimg = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            bimg.createGraphics().drawImage(image, 0, 0, null);
            ImageIO.write(bimg, "png", baos);
            baos.flush();
            byte[] res = baos.toByteArray();
            baos.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failure to convert image to byte array");
            return null;
        }
    }
    /**
     * Method to handle dropping images in LibreOffice.
     * If the user drops outside a text frame, nothing happens.
     * If the user drops inside a text frame, and over no images, a new image is inserted into the text frame
     * If the user drops inside a text frame and over an image, the existing image is replaced with the new one, albeit with same size and position
     * @param image the java.awt.image to be inserted
     * @return true if an image was inserted/replaced/no action needed to be taken, false if an error occured
     */
    public boolean dropImage(Image image, String text, String title, String description) {
        try {
             
            // Querying for the text service factory
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            XAccessible mXRoot = makeRoot(xMSF, xTextDocument);
            XAccessibleContext xAccessibleContext = mXRoot.getAccessibleContext();

            //scope looks like this: xTextDocument -> ScrollPane -> Document
            //get the scroll pane object
            XAccessible xAccessible = xAccessibleContext.getAccessibleChild(0);
            xAccessibleContext = xAccessible.getAccessibleContext();

            //get the document object
            xAccessible = xAccessibleContext.getAccessibleChild(0);
            xAccessibleContext = xAccessible.getAccessibleContext();
            int numChildren = xAccessibleContext.getAccessibleChildCount();
            //loop through all the children of the document and find the text frames
            for (int i = 0; i < numChildren; i++) {
                xAccessible = xAccessibleContext.getAccessibleChild(i);
                XAccessibleContext xChildAccessibleContext = xAccessible.getAccessibleContext();
                if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.TEXT_FRAME && withinRange(xChildAccessibleContext)) {
                    //get text frame
                    XTextFrame xTextFrame = getFrame(xChildAccessibleContext.getAccessibleName());
                    
                    //loop through all images in text frame to see if we are over any of them
                    numChildren = xAccessibleContext.getAccessibleChildCount();
                    for (int j = 0; j < numChildren; j++) {
                        xAccessible = xAccessibleContext.getAccessibleChild(j);
                        xChildAccessibleContext = xAccessible.getAccessibleContext();
                        if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.GRAPHIC && withinRange(xChildAccessibleContext)) {
                            //if we are over the image, then we insert a new image scaled to the width of the one we're dropping on
                            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                                XAccessibleComponent.class, xChildAccessibleContext);
                            int width = xAccessibleComponent.getSize().Width;
                            boolean result = insertContent(image, width, xTextFrame, text, title, description);
                            if (result) {
                                return true;
                            }
                        }
                    }
                    //we're over a text frame, but not an image, so we create a new image based on it's own dimensions
                    insertContent(image, xTextFrame, text, title, description);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("error reading frames");
            e.printStackTrace(System.err);
        }
        return false;
    }
    /**
     * Method to insert a textframe and image together into a text document
     * @param height height of the image
     * @param width width of the image
     * @param xImage image to insert
     * @param destination textframe we are inserting into
     * @param text caption of image
     * @return true if succeeded, false if not
     */
    private boolean insertImageIntoTextFrame(int height, int width, XTextContent xImage, XTextFrame destination, String text){
         XTextFrame xTextFrame = null;
        try{
            XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            //create a new text frame
            Object frame = xMSF.createInstance("com.sun.star.text.TextFrame");
            xTextFrame = (com.sun.star.text.XTextFrame) UnoRuntime.queryInterface(
                    com.sun.star.text.XTextFrame.class, frame);
            
            //set the dimensions of the new text frame
            XShape xTextFrameShape = (com.sun.star.drawing.XShape) UnoRuntime.queryInterface(
                    com.sun.star.drawing.XShape.class, frame);
            com.sun.star.awt.Size aSize = new com.sun.star.awt.Size();
            aSize.Height = height;
            aSize.Width = width;
            xTextFrameShape.setSize(aSize);
            
            //Set the properties of the textframe
            int[] blank = new int[]{0, 0, 0, 0};
            com.sun.star.beans.XPropertySet xTFPS = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, xTextFrame);
            xTFPS.setPropertyValue("AnchorType",
                    com.sun.star.text.TextContentAnchorType.AT_FRAME);
            xTFPS.setPropertyValue("FrameIsAutomaticHeight", true);
            xTFPS.setPropertyValue("LeftBorder", blank);
            xTFPS.setPropertyValue("RightBorder", blank);
            xTFPS.setPropertyValue("TopBorder", blank);
            xTFPS.setPropertyValue("BottomBorder", blank);
            
            //insert the textframe
            XText xText = destination.getText();
            XTextCursor xTextCursor = xText.createTextCursor();
            XTextRange xTextRange = xTextCursor.getStart();
            xText.insertTextContent(xTextRange, xTextFrame, true);
            
            //insert the image into the textframe
            xText = xTextFrame.getText();
            xTextCursor = xText.createTextCursor();
            xTextRange = xTextCursor.getStart();
            xText.insertTextContent(xTextRange, xImage, true);
            
            //insert the caption
            xTextRange.setString(text);
        }catch (Exception exception){
            System.out.println("Couldn't insert image");
            exception.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    /**
     * Convert and insert image and relevant info into Writer doc
     * @param image java.awt.image to insert
     * @param xTextFrame text frame we are inserting into
     * @param text caption for image
     * @param title title of image
     * @param description description of image
     * @return true if succeeded, false if not
     */
    private boolean insertContent(Image image, XTextFrame xTextFrame, String text, String title, String description){
        //create blank graphic in document
        Object graphic = createBlankGraphic();
        
        //query for the interface XTextContent on the GraphicObject 
        com.sun.star.text.XTextContent xImage = (com.sun.star.text.XTextContent) UnoRuntime.queryInterface(
                com.sun.star.text.XTextContent.class, graphic);
        
        //query for the properties of the graphic
        com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                com.sun.star.beans.XPropertySet.class, graphic);

        //calculate the width and height
        int width = (int) Math.round(image.getWidth(null) * 26.4583);
        int height = (int) Math.round(image.getHeight(null) * 26.4583);

        //if the image is greater than the width, then we scale it down the barely fit in the page
        if (width > 165100) {
            int ratio = width;
            width = 165100 - 1000;
            ratio = width / ratio;
            height = height * ratio;
        }
        
        //set the TextContent properties
        try {
            xPropSet.setPropertyValue("AnchorType", TextContentAnchorType.AT_FRAME);
            xPropSet.setPropertyValue("Width", width);
            xPropSet.setPropertyValue("Height", height);
            xPropSet.setPropertyValue("Graphic", convertImage(image));
            xPropSet.setPropertyValue("TextWrap", WrapTextMode.NONE);
            xPropSet.setPropertyValue("Title", title);
            xPropSet.setPropertyValue("Description", description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        //insert the content
        return insertImageIntoTextFrame(height, width, xImage, xTextFrame, text);
    }
    /**
     * Creates an XTextContent from an image that can be inserted into a Writer Document
     * @param image an java.awt.image 
     * @return XTextContent containing the passed image
     */
    private boolean insertContent(Image image, int width, XTextFrame xTextFrame, String text, String title, String description){
        //create blank graphic in document
        Object graphic = createBlankGraphic();
        
        //query for the interface XTextContent on the GraphicObject 
        com.sun.star.text.XTextContent xImage = (com.sun.star.text.XTextContent) UnoRuntime.queryInterface(
                com.sun.star.text.XTextContent.class, graphic);
        
        //query for the properties of the graphic
        com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                com.sun.star.beans.XPropertySet.class, graphic);

        //calculate the width and height
        double ratio =  (double)width/(double) image.getWidth(null);
        width = (int) Math.round(width * 26.4583);
        int height = (int) Math.round(ratio*image.getHeight(null) * 26.4583);

        //if the image is greater than the width, then we scale it down the barely fit in the page

        //set the TextContent properties
        try {
            xPropSet.setPropertyValue("AnchorType", TextContentAnchorType.AT_FRAME);
            xPropSet.setPropertyValue("Width", width);
            xPropSet.setPropertyValue("Height", height);
            xPropSet.setPropertyValue("Graphic", convertImage(image));
            xPropSet.setPropertyValue("TextWrap", WrapTextMode.NONE);
            xPropSet.setPropertyValue("Title", title);
            xPropSet.setPropertyValue("Description", description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        return insertImageIntoTextFrame((int) height, width, xImage, xTextFrame, text);
    }
    /**
     * Find a named text frame within current Writer doc
     * @param name the name of the text frame
     * @return XTextFrame interface
     */
    private XTextFrame getFrame(String name){
        XTextFrame xTextFrame = null;
        try{
            //get the text frame supplier from the document
            XTextFramesSupplier xTextFrameSupplier =
                    (XTextFramesSupplier) UnoRuntime.queryInterface(
                    XTextFramesSupplier.class, xTextDocument);
            
            //get text frame objects
            XNameAccess xNameAccess = xTextFrameSupplier.getTextFrames();

            //query for the object with the desired name
            Object frame = xNameAccess.getByName(name);
            
            //get the XTextFrame interface
            xTextFrame = (XTextFrame) UnoRuntime.queryInterface(
                    com.sun.star.text.XTextFrame.class, frame);
        }catch (Exception e){
            System.out.println("Could not find frame with name " + name);
            e.printStackTrace(System.err);
        }
        return xTextFrame;
        
    }
    /**
     * Convert an image into a XGraphic
     * @param image the java.awt.image to convert
     * @return an XGraphic which can be placed into a XTextContent
     */
    private XGraphic convertImage(Image image){
        XGraphic xGraphic = null;
        try {
            ByteArrayToXInputStreamAdapter xSource = new ByteArrayToXInputStreamAdapter(imageToByteArray(image));
            PropertyValue[] sourceProps = new PropertyValue[2];
            
            //specify the byte array source
            sourceProps[0] = new PropertyValue();
            sourceProps[0].Name = "InputStream";
            sourceProps[0].Value = xSource;
            
            //specify the image type
            sourceProps[1] = new PropertyValue();
            sourceProps[1].Name = "MimeType";
            sourceProps[1].Value = "image/png";
            
            //get the graphic object
            XGraphicProvider xGraphicProvider = (XGraphicProvider) UnoRuntime.queryInterface(
                    XGraphicProvider.class,
                    xMCF.createInstanceWithContext("com.sun.star.graphic.GraphicProvider", context));
            xGraphic = xGraphicProvider.queryGraphic(sourceProps);
        }catch (Exception e){
            System.out.println("Failed to convert image into LibreOffice graphic");
            e.printStackTrace(System.err);
        }
        return xGraphic;
    }
    /**
     * Create a blank graphic for insertion
     * @return Object representing a blank Graphic
     */
    private Object createBlankGraphic(){
        Object graphic = null;
        try {
            //create unique name based on timestamp
            long unixTime = System.currentTimeMillis() / 1000L;
            XMultiServiceFactory docServiceFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xTextDocument);
            graphic = docServiceFactory.createInstance("com.sun.star.text.TextGraphicObject");
            XNamed name = (XNamed) UnoRuntime.queryInterface(XNamed.class, graphic);
            name.setName("" + unixTime);
        } catch (Exception exception) {
            System.out.println("Could not create image");
            exception.printStackTrace(System.err);
        };
        return graphic;
    }
    private static XWindow getCurrentWindow(XMultiServiceFactory msf,
        XModel xModel) {
        return getWindow(msf, xModel, false);
    }
    /**
     * Check if the mouse pointer is within range of particular component
     * @param xAccessibleContext the context of particular component
     * @return true if within, false if not
     */
    private boolean withinRange(XAccessibleContext xAccessibleContext){
        //get the accessible component
        XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                XAccessibleComponent.class, xAccessibleContext);
       
        //get the bounds and check whether cursor is within it
        Point point = xAccessibleComponent.getLocationOnScreen();
        Size size = xAccessibleComponent.getSize();
        java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
        if (point.X + size.Width < location.getX() || location.getX() < point.X || point.Y + size.Height < location.getY() || point.Y > location.getY()) {
            return false;
        } else {
            return true;
        }
    }
    private static XWindow getWindow(XMultiServiceFactory msf, XModel xModel, boolean containerWindow) {
        XWindow xWindow = null;
        try {
            if (xModel == null) {
                System.out.println("invalid model (==null)");
            }
            XController xController = xModel.getCurrentController();
            if (xController == null) {
                System.out.println("can't get controller from model");
            }
            XFrame xFrame = xController.getFrame();
            if (xFrame == null) {
                System.out.println("can't get frame from controller");
            }
            if (containerWindow) {
                xWindow = xFrame.getContainerWindow();
            } else {
                xWindow = xFrame.getComponentWindow();
            }
            if (xWindow == null) {
                System.out.println("can't get window from frame");
            }
        } catch (Exception e) {
            System.out.println("caught exception while getting current window" + e);
        }
        return xWindow;
    }
    private static XAccessible getAccessibleObject(XInterface xObject) {
        XAccessible xAccessible = null;
        try {
            xAccessible = (XAccessible) UnoRuntime.queryInterface(
                    XAccessible.class, xObject);
        } catch (Exception e) {
            System.out.println("Caught exception while getting accessible object" + e);
            e.printStackTrace();
        }
        return xAccessible;
    }
    private static XAccessible makeRoot(XMultiServiceFactory msf, XModel aModel) {
        XWindow xWindow = getCurrentWindow(msf, aModel);
        return getAccessibleObject(xWindow);
    }
}
