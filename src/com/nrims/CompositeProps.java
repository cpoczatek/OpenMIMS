package com.nrims;

/**
 * A container class for storing properties needed to generate a Composite image.
 *
 * @author cpoczatek
 */
public class CompositeProps {

    //-----------------------------
    static final long serialVersionUID = 2L;
    //-----------------------------
    // DO NOT! Change variable order/type
    // DO NOT! Delete variables
    private MimsPlus[] images;
    //------------------------------
    //End of v2


   /** Default constructor. */
   public CompositeProps(){}

   /**
    * Instantiates a CompositeProps object with images <code>imgs</code>.
    * @param imgs set of images used to create the composite image.
    */
   public CompositeProps(MimsPlus[] imgs) {
       this.images = imgs;

   }

   /**
    * Sets the images to be used for generating the composite image.
    * @param imgs set of images used to create the composite image.
    */
   public void setImages(MimsPlus[] imgs) { this.images = imgs ; }

   /**
    * Gets the images used to generate the composite image.
    * @return the array of images used to create the composite image.
    */
   public MimsPlus[] getImages() { return this.images ; }

}
