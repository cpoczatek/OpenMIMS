/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nrims;
/*package libreoffice ;*/
//small change

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
import com.sun.star.container.XIndexAccess;
import com.sun.star.drawing.XDrawPage;
import com.sun.star.drawing.XDrawPagesSupplier;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XTextEmbeddedObjectsSupplier;
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
    private XComponent getCurrentDocument(){
        try {
            context = Bootstrap.bootstrap();
            xMCF = context.getServiceManager();
            Object oDesktop = xMCF.createInstanceWithContext(
                    "com.sun.star.frame.Desktop", context);
            XDesktop desktop = (com.sun.star.frame.XDesktop) UnoRuntime.queryInterface(
                    com.sun.star.frame.XDesktop.class, oDesktop);
            //Get the document with focus here
            XComponent currentDocument = desktop.getCurrentComponent();
            return currentDocument;
        }catch (Exception e){
            System.out.println("Failure to connect");
        }
        return null;
    }
    /**
     * Method to handle dropping images in LibreOffice.
     * If the user drops outside a text frame, nothing happens.
     * If the user drops inside a text frame, and over no images, a new image is inserted into the text frame
     * If the user drops inside a text frame and over an image, the existing image is replaced with the new one, albeit with same size and position
     * @param image the java.awt.image to be inserted
     * @return true if an image was inserted/replaced/no action needed to be taken, false if an error occured
     */
    public boolean dropImage(Image i, String text, String title, String description) {
        ImageInfo image = new ImageInfo(i, text, title, description);
        XComponent currentDocument = getCurrentDocument();
        if (currentDocument == null) {
            return false;
        }
        try {
             // Querying for the text interface
            XTextDocument xTextDocument = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, currentDocument);
            //current document is not a writer
            if (xTextDocument == null) {
                //check if an draw doc
                XDrawPagesSupplier xDrawPagesSupplier = (XDrawPagesSupplier) UnoRuntime.queryInterface(
                        XDrawPagesSupplier.class, currentDocument);
                if (xDrawPagesSupplier != null){
                    
                    Object drawPages = xDrawPagesSupplier.getDrawPages();
                    XIndexAccess xIndexedDrawPages = (XIndexAccess) UnoRuntime.queryInterface(
                            XIndexAccess.class, drawPages);
                    Object drawPage = xIndexedDrawPages.getByIndex(0);
                    XDrawPage xDrawPage = (XDrawPage) UnoRuntime.queryInterface(XDrawPage.class, drawPage);
                    if (xDrawPage != null) System.out.println("Current document is a draw");
                    insertIntoDraw(currentDocument, image);
                }
            }else{
                System.out.println("Current document is a writer");
                insertIntoWriter(xTextDocument, image);
            }
            
        } catch (Exception e) {
            System.out.println("error reading frames");
            e.printStackTrace(System.err);
        }
        return false;
    }
     private boolean insertIntoWriter(XTextDocument xTextDocument, ImageInfo image){
        try{
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
                System.out.println(xChildAccessibleContext.getAccessibleRole());
                if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.TEXT_FRAME && withinRange(xChildAccessibleContext)) {
                    //get text frame
                    XTextFrame xTextFrame = getFrame(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    
                    //loop through all images in text frame to see if we are over any of them
                    numChildren = xChildAccessibleContext.getAccessibleChildCount();
                    for (int j = 0; j < numChildren; j++) {
                        xAccessible = xAccessibleContext.getAccessibleChild(j);
                        xChildAccessibleContext = xAccessible.getAccessibleContext();
                        if (xChildAccessibleContext.getAccessibleRole() == AccessibleRole.GRAPHIC && withinRange(xChildAccessibleContext)) {
                            //if we are over the image, then we insert a new image scaled to the width of the one we're dropping on
                            XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                                XAccessibleComponent.class, xChildAccessibleContext);
                            image.width = xAccessibleComponent.getSize().Width;
                            j = numChildren;
                        }
                    }
                    return insertTextContent(image, xTextFrame, xTextDocument);
                }else if(xChildAccessibleContext.getAccessibleRole() == AccessibleRole.EMBEDDED_OBJECT && withinRange(xChildAccessibleContext)){
                    XComponent xComponent = getOLE(xChildAccessibleContext.getAccessibleName(), xTextDocument);
                    insertDrawContent(image, xComponent);
                }
            }
            if (withinRange(xAccessibleContext)){
                xAccessible = xAccessibleContext.getAccessibleChild(0);
                xAccessibleContext = xAccessible.getAccessibleContext();
                XAccessibleComponent xAccessibleComponent = UnoRuntime.queryInterface(
                        XAccessibleComponent.class, xAccessibleContext);
                Point point = xAccessibleComponent.getLocationOnScreen();
                java.awt.Point location = MouseInfo.getPointerInfo().getLocation();
                image.x = (int) Math.round((location.getX() - point.X) * 26.4583);
                image.y = (int) Math.round((location.getY() - point.Y) * 26.4583);
                insertTextContent(image, null, xTextDocument);
            }
        }catch (Exception e){
            
        }
        return true;
    }
    private boolean insertIntoDraw(XComponent xComponent, ImageInfo image){
        try{
        XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xComponent);
        XMultiServiceFactory xMSF = (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xModel);
            XAccessible mXRoot = makeRoot(xMSF, xModel);
            XAccessibleContext xAccessibleContext = mXRoot.getAccessibleContext();
            withinRange(xAccessibleContext);
            XAccessible xAccessible = xAccessibleContext.getAccessibleChild(0);
            xAccessibleContext = xAccessible.getAccessibleContext();
            withinRange(xAccessibleContext);
            xAccessible = xAccessibleContext.getAccessibleChild(0);
            xAccessibleContext = xAccessible.getAccessibleContext();
            if (withinRange(xAccessibleContext)){
                insertDrawContent(image, xComponent);
            }
        System.out.println(xModel == null);
        }catch(Exception e){
            
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
    private boolean insertTextContent(ImageInfo image, XTextFrame xTextFrame, XTextDocument xTextDocument){
        int height;
        int width;
        //create blank graphic in document
        Object graphic = createBlankGraphic(xTextDocument);
        
        //query for the interface XTextContent on the GraphicObject 
        image.xImage = (com.sun.star.text.XTextContent) UnoRuntime.queryInterface(
                com.sun.star.text.XTextContent.class, graphic);
        
        //query for the properties of the graphic
        com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                com.sun.star.beans.XPropertySet.class, graphic);
        if (image.width > 0){
            //calculate the width and height
            double ratio = (double) image.width / (double) image.image.getWidth(null);
            width = (int) Math.round(image.width * 26.4583);
            height = (int) Math.round(ratio * image.image.getHeight(null) * 26.4583);
        }else{
            //calculate the width and height
            width = (int) Math.round(image.image.getWidth(null) * 26.4583);
            height = (int) Math.round(image.image.getHeight(null) * 26.4583);
        }

        //if the image is greater than the width, then we scale it down the barely fit in the page
        if (width > 165100) {
            int ratio = width;
            width = 165100 - 1000;
            ratio = width / ratio;
            height = height * ratio;
        }
        image.width = width;
        image.height = height;
        //set the TextContent properties
        try {
            xPropSet.setPropertyValue("AnchorType", TextContentAnchorType.AT_FRAME);
            xPropSet.setPropertyValue("Width", width);
            xPropSet.setPropertyValue("Height", height);
            xPropSet.setPropertyValue("Graphic", convertImage(image.image));
            xPropSet.setPropertyValue("TextWrap", WrapTextMode.NONE);
            xPropSet.setPropertyValue("Title", image.title);
            xPropSet.setPropertyValue("Description", image.description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        if (xTextFrame != null){
        //insert the content
            return insertImageIntoTextFrame(image, xTextFrame, xTextDocument);
        }else{
            return insertImageAtCoords(image, xTextDocument);
        }
    }
    private boolean insertDrawContent(ImageInfo image, XComponent xComponent){
        try {
        int height;
        int width;
        //create blank graphic in document
        Object graphic = createBlankGraphic(xComponent);
        
        //query for the interface XTextContent on the GraphicObject 
        image.xShape = (XShape) UnoRuntime.queryInterface(
                XShape.class, graphic);
        
        //query for the properties of the graphic
        com.sun.star.beans.XPropertySet xPropSet = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                com.sun.star.beans.XPropertySet.class, graphic);
        if (image.width > 0){
            //calculate the width and height
            double ratio = (double) image.width / (double) image.image.getWidth(null);
            width = (int) Math.round(image.width * 26.4583);
            height = (int) Math.round(ratio * image.image.getHeight(null) * 26.4583);
        }else{
            //calculate the width and height
            width = (int) Math.round(image.image.getWidth(null) * 26.4583);
            height = (int) Math.round(image.image.getHeight(null) * 26.4583);
        }
        //if the image is greater than the width, then we scale it down the barely fit in the page
        if (width > 165100) {
            int ratio = width;
            width = 165100 - 1000;
            ratio = width / ratio;
            height = height * ratio;
        }
        Size size = new Size();
        image.height = size.Height = height;
        image.width = size.Width = width;
        image.xShape.setSize(size);
        Point point = new Point();
        point.X = 0;
        point.Y = 0;
        image.xShape.setPosition(point);
            xPropSet.setPropertyValue("Graphic", convertImage(image.image));
            xPropSet.setPropertyValue("Title", image.title);
            xPropSet.setPropertyValue("Description", image.description);
        } catch (Exception exception) {
            System.out.println("Couldn't set image properties");
            exception.printStackTrace(System.err);
            return false;
        }
        try{
            XDrawPagesSupplier xDrawPagesSupplier = (XDrawPagesSupplier) UnoRuntime.queryInterface(
                    XDrawPagesSupplier.class, xComponent);
            if (xDrawPagesSupplier != null) {
                Object drawPages = xDrawPagesSupplier.getDrawPages();
                XIndexAccess xIndexedDrawPages = (XIndexAccess) UnoRuntime.queryInterface(
                        XIndexAccess.class, drawPages);
                Object drawPage = xIndexedDrawPages.getByIndex(0);
                XDrawPage xDrawPage = (XDrawPage) UnoRuntime.queryInterface(XDrawPage.class, drawPage);
                if (xDrawPage != null) {
                    xDrawPage.add(image.xShape);
                }
            }
        }catch (Exception e){
            
        }
        return true;
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
    private boolean insertImageIntoTextFrame(ImageInfo image, XTextFrame destination, XTextDocument xTextDocument){
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
            aSize.Height = image.height;
            aSize.Width = image.width;
             System.out.println(image.height);
            System.out.println(image.width);
            System.out.println(aSize.Height);
            System.out.println(aSize.Width);
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
            xText.insertTextContent(xTextRange, image.xImage, true);
            
            //insert the caption
            xTextRange.setString(image.text);
        }catch (Exception exception){
            System.out.println("Couldn't insert image");
            exception.printStackTrace(System.err);
            return false;
        }
        return true;
    } 
        private boolean insertImageAtCoords(ImageInfo image, XTextDocument xTextDocument){
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
            aSize.Height = image.height;
            aSize.Width = image.width;
            xTextFrameShape.setSize(aSize);
            int x = image.x- (image.width/2);
            int y = image.y - (image.height/2);
            //Set the properties of the textframe
            int[] blank = new int[]{0, 0, 0, 0};
            com.sun.star.beans.XPropertySet xTFPS = (com.sun.star.beans.XPropertySet) UnoRuntime.queryInterface(
                    com.sun.star.beans.XPropertySet.class, xTextFrame);
            xTFPS.setPropertyValue("AnchorType",
                    com.sun.star.text.TextContentAnchorType.AT_PAGE);
            xTFPS.setPropertyValue("FrameIsAutomaticHeight", true);
            xTFPS.setPropertyValue("LeftBorder", blank);
            xTFPS.setPropertyValue("RightBorder", blank);
            xTFPS.setPropertyValue("TopBorder", blank);
            xTFPS.setPropertyValue("BottomBorder", blank);
            
            xTFPS.setPropertyValue("VertOrient", com.sun.star.text.VertOrientation.NONE);
            xTFPS.setPropertyValue("HoriOrient", com.sun.star.text.HoriOrientation.NONE);
            xTFPS.setPropertyValue("HoriOrientRelation", com.sun.star.text.RelOrientation.PAGE_FRAME);
            xTFPS.setPropertyValue("VertOrientRelation", com.sun.star.text.RelOrientation.PAGE_FRAME);
            xTFPS.setPropertyValue("HoriOrientPosition", x);
            xTFPS.setPropertyValue("VertOrientPosition", y);
            //insert the textframe
            XText xText = xTextDocument.getText();
            XTextCursor xTextCursor = xText.createTextCursor();
            XTextRange xTextRange = xTextCursor.getStart();
            xText.insertTextContent(xTextRange, xTextFrame, true);
            
            //insert the image into the textframe
            xText = xTextFrame.getText();
            xTextCursor = xText.createTextCursor();
            xTextRange = xTextCursor.getStart();
            xText.insertTextContent(xTextRange, image.xImage, true);
            
            //insert the caption
            xTextRange.setString(image.text);
            
        }catch (Exception exception){
            System.out.println("Couldn't insert image");
            exception.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    /**
     * Find a named text frame within current Writer doc
     * @param name the name of the text frame
     * @return XTextFrame interface
     */
    private XTextFrame getFrame(String name, XTextDocument xTextDocument){
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
        private XComponent getOLE(String name, XTextDocument xTextDocument){
        XComponent xComponent = null;
        try{
            //get the text frame supplier from the document
            XTextEmbeddedObjectsSupplier xTextEmbeddedObjectsSupplier =
                    (XTextEmbeddedObjectsSupplier) UnoRuntime.queryInterface(
                    XTextEmbeddedObjectsSupplier.class, xTextDocument);
            
            //get text frame objects
            XNameAccess xNameAccess = xTextEmbeddedObjectsSupplier.getEmbeddedObjects();

            //query for the object with the desired name
            Object xTextEmbeddedObject = xNameAccess.getByName(name);
            XTextContent xTextContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, xTextEmbeddedObject);
            //get the XTextFrame interface
            com.sun.star.document.XEmbeddedObjectSupplier xEOS = (com.sun.star.document.XEmbeddedObjectSupplier)
             UnoRuntime.queryInterface(com.sun.star.document.XEmbeddedObjectSupplier.class, xTextContent);
             com.sun.star.lang.XComponent xModel = xEOS.getEmbeddedObject();
             return xModel;
        }catch (Exception e){
            System.out.println("Could not find frame with name " + name);
            e.printStackTrace(System.err);
        }
        return xComponent;
        
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
    private Object createBlankGraphic(XTextDocument xTextDocument){
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
    private Object createBlankGraphic(XComponent xDrawPage){
        Object graphic = null;
        try {
            //create unique name based on timestamp
            long unixTime = System.currentTimeMillis() / 1000L;
            XMultiServiceFactory docServiceFactory =
                    (XMultiServiceFactory) UnoRuntime.queryInterface(
                    XMultiServiceFactory.class, xDrawPage);
            graphic = docServiceFactory.createInstance("com.sun.star.drawing.GraphicObjectShape");
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
            System.out.println("Not within range of " + xAccessibleContext.getAccessibleRole());
            return false;
        } else {
             System.out.println("Within range of " + xAccessibleContext.getAccessibleRole());
             System.out.println(point.X);
             System.out.println(point.Y);
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
    public class ImageInfo {
        public int x;
        public int y;
        public Image image;
        public XTextContent xImage;
        public XShape xShape;
        public int height;
        public int width;
        public String text;
        public String title;
        public String description;
        public ImageInfo(Image i){
            this.image = i;
        }
        public ImageInfo(Image i, String n, String t, String d){
            this.image = i;
            this.text = n;
            this.title = t;
            this.description = d;
        }
    }
}
