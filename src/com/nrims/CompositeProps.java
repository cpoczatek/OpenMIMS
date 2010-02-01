/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nrims;

/**
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


   // Create an empty ratio props object.
   public CompositeProps(){}

   // Create a ratio props object with given numerator and denominator mass indexes.
   public CompositeProps(MimsPlus[] imgs) {
       this.images = imgs;

   }

   // Two props objects are equal if numerator and denominator are the same.
   public boolean equals(CompositeProps cp) {
      return true;
   }

   // Getters and Setters.
   //
   public void setImages(MimsPlus[] imgs) { this.images = imgs ; }
   public MimsPlus[] getImages() { return this.images ; }

}
