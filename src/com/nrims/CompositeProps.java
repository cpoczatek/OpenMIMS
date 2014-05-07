package com.nrims;

import java.io.Serializable;

/**
 * A container class for storing properties needed to generate a Composite image.
 *
 * @author cpoczatek
 */
public class CompositeProps implements Serializable{

    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    private Object[] imageProps;
    private String dataFileName;
    //------------------------------
    //End of v2


   /**
    * Instantiates a CompositeProps object with images <code>imgs</code>.
    * @param imgs set of images used to create the composite image.
    */
   public CompositeProps(Object[] imgs) {
       this.imageProps = imgs;

   }

    /**
     * Two <code>CompositeProps</code> objects are equal if the <code>MimsPlus</code>
     * objects that make them up are equal.
     *
     * @param cp a <code>CompositeProps</code> object.
     * @return <code>true</code> if <code>this</code> and <code>cp</code> are equal.
     */
    public boolean equals(CompositeProps cp) {

       // If lengths are different then obviously they are not equal.
       Object[] cps = cp.getImageProps();
       if (cps.length != imageProps.length)
          return false;

       // If the contents of the images array of the two objects differ
       // then the two objects are considered different, even if the
       // contents are the same, but in a different order.
       for (int i = 0; i < imageProps.length; i++){
           //if neither is null, check if images are equal
           if (cps[i] != null && imageProps[i] != null) {
               if (imageProps[i] instanceof RatioProps) {
                   if(!(cps[i] instanceof RatioProps)){
                       if (!((RatioProps) cps[i]).equals((RatioProps) imageProps[i])){
                           return false;
                       }
                   }
               }
               if (imageProps[i] instanceof HSIProps) {
                   if (!(cps[i] instanceof HSIProps)) {
                       if (!((HSIProps) cps[i]).equals((HSIProps) imageProps[i])) {
                           return false;
                       }
                   }
               }
               if (imageProps[i] instanceof SumProps) {
                   if (!(cps[i] instanceof SumProps)) {
                       if (!((SumProps) cps[i]).equals((SumProps) imageProps[i])) {
                           return false;
                       }
                   }
               }
               if (imageProps[i] instanceof MassProps) {
                   if (!(cps[i] instanceof MassProps)) {
                       if (!((MassProps) cps[i]).equals((MassProps) imageProps[i])) {
                           return false;
                       }
                   }
               }
           }
           //if one is null and the other not they are unequal
           if( (cps[i] != null && imageProps[i] == null) || (cps[i] == null && imageProps[i] != null))
            return false;
       }

       return true;
   }

   /**
    * Sets the images to be used for generating the composite image.
    * @param imgs set of images used to create the composite image.
    */
   public void setImageProps(Object[] imgs) { this.imageProps = imgs ; }
   public Object[] getImageProps() { return this.imageProps; }

   /**
    * Gets the images used to generate the composite image.
    * @return the array of images used to create the composite image.
    */
   public MimsPlus[] getImages(UI ui) {
       MimsPlus[] images = new MimsPlus[imageProps.length];
       MimsPlus[] massImages = ui.getOpenMassImages();
       MimsPlus[] sumImages = ui.getOpenSumImages();
       MimsPlus[] ratioImages = ui.getOpenRatioImages();
       MimsPlus[] hsiImages = ui.getOpenHSIImages();
       for (int i = 0; i < imageProps.length; i ++){
           if (imageProps[i] instanceof RatioProps) {
               for (int j = 0; j < ratioImages.length; j++) {
                   if (ratioImages[j].getRatioProps().equals((RatioProps) imageProps[i])){
                       images[i] =  ratioImages[j];
                   }
               }
           }
           if (imageProps[i] instanceof HSIProps) {
               for (int j = 0; j < hsiImages.length; j++) {
                   if (hsiImages[j].getHSIProps().equals((HSIProps) imageProps[i])) {
                       images[i] = hsiImages[j];
                   }
               }
           }
           if (imageProps[i] instanceof SumProps) {
               for (int j = 0; j < sumImages.length; j++) {
                   if (sumImages[j].getSumProps().equals((SumProps) imageProps[i])){
                       images[i] =  sumImages[j];
                   }                   
               }
           }
           if (imageProps[i] instanceof MassProps) {
               for (int j = 0; j < massImages.length; j++) {
                   MassProps massProp = (MassProps) imageProps[i];
                   if (massImages[j].getMassIndex() == massProp.getMassIdx()){
                       images[i] =  massImages[j];
                   }                      
               }
           }
       }
       return images;
   }
       /**
     * Sets the name of the data file from which this image was derived.
     * @param fileName name of file (name only, do not include directory).
     */
    public void setDataFileName(String fileName) { dataFileName = fileName;}
    public String getDataFileName() { return dataFileName;}

}
