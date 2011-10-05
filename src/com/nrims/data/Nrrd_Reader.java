package com.nrims.data;

// Nrrd_Reader
// -----------

// (c) Gregory Jefferis 2007
// Department of Zoology, University of Cambridge
// jefferis@gmail.com
// All rights reserved
// Source code released under Lesser Gnu Public License v2

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * ImageJ plugin to read a file in Gordon Kindlmann's NRRD
 * or 'nearly raw raster data' format, a simple format which handles
 * coordinate systems and data types in a very general way.
 * See <A HREF="http://teem.sourceforge.net/nrrd">http://teem.sourceforge.net/nrrd</A>
 * and <A HREF="http://flybrain.stanford.edu/nrrd">http://flybrain.stanford.edu/nrrd</A>
 */
public class Nrrd_Reader implements Opener {

   public final String uint8Types="uchar, unsigned char, uint8, uint8_t";
	public final String int16Types="short, short int, signed short, signed short int, int16, int16_t";
	public final String uint16Types="ushort, unsigned short, unsigned short int, uint16, uint16_t";
	public final String int32Types="int, signed int, int32, int32_t";
	public final String uint32Types="uint, unsigned int, uint32, uint32_t";

    private File file = null;
    private int currentIndex = 0;
    private NrrdFileInfo fi = null;
    private boolean header = false;
    private int bitSize = 0;

    public Nrrd_Reader(File imageFile) throws IOException {

        // Make sure file exists.
        this.file = imageFile;
        if (!file.exists())
            throw new NullPointerException("File " + imageFile + " does not exist.");

        // Read the header.
        try {
           fi = getHeaderInfo();
        } catch (IOException io) {System.out.println("Error reading file "+file.getAbsolutePath());}

        if (fi.massNames == null) {
           fi.massNames = new String[fi.nMasses];
           for (int i = 0; i < fi.nMasses; i++)
              fi.massNames[i] = Integer.toString(i);
        } else if (fi.nMasses != fi.massNames.length) {
            System.out.print("Error! Number of masses ("+fi.nMasses+") does not equal " +
                    "number of mass names referenced: "+fi.massNames);
            System.out.println();
            return;
        }

    }

    // Reads header and gets metadata.
	 private NrrdFileInfo getHeaderInfo() throws IOException {

      if (IJ.debugMode) IJ.log("Entering Nrrd_Reader.readHeader():");

      // Setup file header.
      fi = new NrrdFileInfo();
	   fi.directory=file.getParent(); fi.fileName=file.getName();

		// Need RAF in order to ensure that we know file offset.
		RandomAccessFile in = new RandomAccessFile(file.getAbsolutePath(),"r");

      // Initialize some strings.
		String thisLine,noteType,noteValue, noteValuelc;
		fi.fileFormat = FileInfo.RAW;
      int lineskip = 0;

		// Parse the header file, until reach an empty line.
		while(true) {
			thisLine=in.readLine();
			if(thisLine==null || thisLine.equals("")) {
				fi.longOffset = in.getFilePointer();
				break;
			}

			if(thisLine.indexOf("#")==0) continue; // ignore comments

         // Get the key/value pair
			noteType=getFieldPart(thisLine,0);
         String originalNoteType = noteType; //keep case for notes
         noteType = noteType.toLowerCase(); // case irrelevant
			noteValue=getFieldPart(thisLine,1);
			noteValuelc=noteValue.toLowerCase();

			if (IJ.debugMode) IJ.log("NoteType:"+noteType+", noteValue:"+noteValue);

			if (noteType.equals("dimension")) {
				fi.dimension=Integer.valueOf(noteValue).intValue();
				//???????? add back dimension check?
                //if(fi.dimension>3) throw new IOException("Nrrd_Reader: Dimension>3 not yet implemented!");
			}
			if (noteType.equals("sizes")) {
				fi.sizes=new int[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					fi.sizes[i]=Integer.valueOf(getSubField(thisLine,i)).intValue();
					if(i==0) fi.width=fi.sizes[0];
					if(i==1) fi.height=fi.sizes[1];
					if(i==2) fi.nImages=fi.sizes[2];
               if(i==3) fi.nMasses=fi.sizes[3];
				}
			}

			if (noteType.equals("units")) //spatialCal.setUnit(firstNoteValue);
			if (noteType.equals("spacings")) {
				double[] spacings=new double[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					//spacings[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
					//if(i==0) spatialCal.pixelWidth=spacings[0];
					//if(i==1) spatialCal.pixelHeight=spacings[1];
					//if(i==2) spatialCal.pixelDepth=spacings[2];
				}
			}
			if (noteType.equals("centers") || noteType.equals("centerings")) {
				fi.centers=new String[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					fi.centers[i]=getSubField(thisLine,i);
				}
			}

			if (noteType.equals("axis mins") || noteType.equals("axismins")) {
				double[] axismins=new double[fi.dimension];
				for(int i=0;i<fi.dimension;i++) {
					// TOFIX - this order of allocations is not a given!
					// NB xOrigin are in pixels, whereas axismins are of course
					// in units; these are converted later
					//axismins[i]=Double.valueOf(getSubField(thisLine,i)).doubleValue();
					//if(i==0) spatialCal.xOrigin=axismins[0];
					//if(i==1) spatialCal.yOrigin=axismins[1];
					//if(i==2) spatialCal.zOrigin=axismins[2];
				}
			}

			if (noteType.equals("type")) {
				if (uint8Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY8;
				}

            //16 bit signed/unsigned checks were flipped?
            else if(uint16Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY16_UNSIGNED;
               bitSize = 2;
				} else if(int16Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY16_SIGNED;
				} else if(uint32Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY32_UNSIGNED;
				} else if(int32Types.indexOf(noteValuelc)>=0) {
					fi.fileType=FileInfo.GRAY32_INT;
				} else if(noteValuelc.equals("float")) {
					fi.fileType=FileInfo.GRAY32_FLOAT;
               bitSize = 4;
				} else if(noteValuelc.equals("double")) {
					fi.fileType=FileInfo.GRAY64_FLOAT;
				} else {
					throw new IOException("Unimplemented data type ="+noteValue);
				}
			}

			if (noteType.equals("byte skip")||noteType.equals("byteskip"))
                fi.longOffset=Long.valueOf(noteValue).longValue();
			if (noteType.equals("endian")) {
				if(noteValuelc.equals("little")) {
					fi.intelByteOrder = true;
				} else {
					fi.intelByteOrder = false;
				}
			}

			if (noteType.equals("encoding")) {
				if(noteValuelc.equals("gz")) noteValuelc="gzip";
				fi.encoding=noteValuelc;
			}

         if (noteType.equals("data file")) {
            header = true;
            fi = setDataFile(fi, noteValue);
         }

         if (noteType.equals("line skip"))
            lineskip = Integer.valueOf(noteValue).intValue();

            // All the following are Mims specific headers.
            int i = noteType.indexOf(Opener.Nrrd_seperator);
            int j = Opener.Nrrd_seperator.length();
            int k = i+j;
            String value = noteType.substring(k);
            String originalvalue = originalNoteType.substring(k);
            if (value == null) value = "";

            if (thisLine.startsWith(Opener.Mims_mass_numbers)) {               
                fi.massNames=noteType.substring(i+Opener.Nrrd_seperator.length()).split(" ");
            }

            if (thisLine.startsWith(Opener.Mims_count_time))
                fi.countTime=value;

            if (thisLine.startsWith(Opener.Mims_date))
                fi.sampleDate=value;

            if (thisLine.startsWith(Opener.Mims_duration))
                fi.duration=value;

            if (thisLine.startsWith(Opener.Mims_dwell_time))
                fi.dwellTime=value;

            if (thisLine.startsWith(Opener.Mims_hour))
                fi.sampleHour=value;

            if (thisLine.startsWith(Opener.Mims_position))
                fi.position=value;

            if (thisLine.startsWith(Opener.Mims_sample_name))
                fi.sampleName=value;

            if (thisLine.startsWith(Opener.Mims_user_name))
                fi.userName=value;

            if (thisLine.startsWith(Opener.Mims_raster)) {
                try {
                   Integer ras = new Integer(value);
                   fi.raster=ras.intValue();
                } catch (Exception e) {fi.raster = -1;}
            }

            if (thisLine.startsWith(Opener.Mims_pixel_height)) {
                try {
                   Float fl = new Float(value);
                   fi.pixel_height=fl.floatValue();
                } catch (Exception e) {fi.pixelHeight = (new Float(-1.0)).floatValue();}
            }

            if (thisLine.startsWith(Opener.Mims_pixel_width)) {
                try {
                   Float fl = new Float(value);
                   fi.pixel_width=fl.floatValue();
                } catch (Exception e) {fi.pixelWidth = (new Float(-1.0)).floatValue();}
            }
            //case sensitive text
            if (thisLine.startsWith(Opener.Mims_notes)) {
                fi.notes = originalvalue;
            }           
		}

      if (header) {
         RandomAccessFile datain = new RandomAccessFile(new File(fi.directory, fi.fileName),"r");
         for (int i = 0; i < lineskip; i++) {
            thisLine=datain.readLine();
         }
         fi.longOffset = datain.getFilePointer();
      }

		return (fi);
	}

    // Gets the field name from the header.
	String getFieldPart(String str, int fieldIndex) {
		str=str.trim(); // trim the string
		String[] fieldParts=str.split(":\\s+");
		if(fieldParts.length<2) return(fieldParts[0]);
		if(fieldIndex==0) return fieldParts[0];
		else return fieldParts[1];
	}

   private NrrdFileInfo setDataFile(NrrdFileInfo fi, String noteValue) {
      File datafile = new File(noteValue);
      if (datafile.exists()){
        fi.directory = datafile.getParent();
        fi.fileName = datafile.getName();
        return fi;
      } else
         datafile = new File(fi.directory, noteValue);

      if (datafile.exists()) {
        fi.directory = datafile.getParent();
        fi.fileName = datafile.getName();
        return fi;
      } else {
         IJ.error("FileOpener", "File " + noteValue + " does not exist. \n " +
                 "File " + fi.directory + System.getProperty("file.separator") + noteValue + " does not exist.");
      }
      return fi;
   }

	String getSubField(String str, int fieldIndex) {
		String fieldDescriptor=getFieldPart(str,1);
		fieldDescriptor=fieldDescriptor.trim(); // trim the string

		if (IJ.debugMode) IJ.log("fieldDescriptor = "+fieldDescriptor+"; fieldIndex = "+fieldIndex);

		String[] fields_values=fieldDescriptor.split("\\s+");

		if (fieldIndex>=fields_values.length) {
			return "";
		} else {
			String rval=fields_values[fieldIndex];
			if(rval.startsWith("\"")) rval=rval.substring(1);
			if(rval.endsWith("\"")) rval=rval.substring(0, rval.length()-1);
			return rval;
		}
	}

    public Object getPixels(int index) throws IndexOutOfBoundsException, IOException {
       
      // Set up a temporary header to read the pixels from the file.
      NrrdFileInfo fi_clone = (NrrdFileInfo)fi.clone();

      // Calculate offset
      long offset = fi_clone.longOffset + // move down header
              (bitSize * index * fi_clone.width * fi_clone.height * fi_clone.nImages) + // move down to correct channel
              (bitSize * fi_clone.width * fi_clone.height * currentIndex); // move down to correct image within that channel
      fi_clone.longOffset = offset;
      fi_clone.nImages = 1; // only going to read 1 image.
      FileOpener fo = new FileOpener(fi_clone);

      // Get image from file.
      ImagePlus imp = fo.open(false);
      if (imp == null) {
         throw new IOException();
      }

      Object pixels;
      if (fi.fileType == FileInfo.GRAY16_UNSIGNED)
         pixels = (short[])imp.getProcessor().getPixels();
      else if (fi.fileType == FileInfo.GRAY32_FLOAT)
         pixels = (float[])imp.getProcessor().getPixels();
      else
         pixels = null;

      return pixels;
    }

    public void close() {
    }

    public void setStackIndex(int index) throws IndexOutOfBoundsException {
        this.currentIndex = index;
    }

    public File getImageFile() {
        return file;
    }

    public int getNMasses() {
        return fi.nMasses;
    }

    public int getNImages() {
        return fi.nImages;
    }

    public int getWidth() {
        return fi.width;
    }

    public int getHeight() {
        return fi.height;
    }

    public String[] getMassNames() {
        return fi.massNames;
    }

    public float getPixelWidth() {
        return fi.pixel_width;
    }

    public float getPixelHeight() {
        return fi.pixel_height;
    }

    public String getSampleDate() {
        return fi.sampleDate;
    }

    public String getSampleHour() {
        return fi.sampleHour;
    }

    public String getSampleName() {
        return fi.sampleName;
    }

    public String getUserName() {
        return fi.userName;
    }

    public String getPosition() {
        return fi.position;
    }

    public int getRaster() {
        return fi.raster;
    }

    public String getDwellTime() {
        return fi.dwellTime;
    }

    public String getCountTime() {
        return fi.countTime;
    }

    public String getDuration() {
        return fi.duration;
    }

    public String getNotes() {
        return fi.notes;
    }

    public void setNotes(String notes) {
        fi.notes = notes;
    }

    public int getFileType() {
        return fi.fileType;
    }
}
