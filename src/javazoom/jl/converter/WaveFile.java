/*
 * 11/19/04 1.0 moved to LGPL.
 * 02/23/99 JavaConversion by E.B
 * Don Cross, April 1993.
 * RIFF file format classes.
 * See Chapter 8 of "Multimedia Programmer's Reference" in
 * the Microsoft Windows SDK.
 *  
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package javazoom.jl.converter;

/**
 * Class allowing WaveFormat Access
 */
public class WaveFile extends RiffFile
{
    public static final int 	MAX_WAVE_CHANNELS = 2;
    
	class WaveFormat_ChunkData
	{
   	   public short         wFormatTag = 0;       // Format category (PCM=1)
   	   public short         nChannels = 0;        // Number of channels (mono=1, stereo=2)
   	   public int         	nSamplesPerSec = 0;   // Sampling rate [Hz]
   	   public int         	nAvgBytesPerSec = 0;
   	   public short         nBlockAlign = 0;
   	   public short         nBitsPerSample = 0;
	   
	   public WaveFormat_ChunkData()
	   {
	      wFormatTag = 1;     // PCM
	      Config(44100,(short)16,(short)1);
   		}

   	   public void Config (int NewSamplingRate, short NewBitsPerSample, short NewNumChannels)
       {
      	  nSamplesPerSec  = NewSamplingRate;
      	  nChannels       = NewNumChannels;
      	  nBitsPerSample  = NewBitsPerSample;
      	  nAvgBytesPerSec = (nChannels * nSamplesPerSec * nBitsPerSample) / 8;
      	  nBlockAlign     = (short) ((nChannels * nBitsPerSample) / 8);
       }
   }


	class WaveFormat_Chunk
	{
   		public RiffChunkHeader         header;
   		public WaveFormat_ChunkData    data;

   		public WaveFormat_Chunk()
   		{
   		    header = new RiffChunkHeader();
   		    data = new WaveFormat_ChunkData();
      		header.ckID     =   FourCC("fmt ");
      		header.ckSize   =   16;
   		}

   		public int VerifyValidity()
   		{
      	    boolean ret = header.ckID == FourCC("fmt ") &&

            (data.nChannels == 1 || data.nChannels == 2) &&

             data.nAvgBytesPerSec == ( data.nChannels *
                                       data.nSamplesPerSec *
                                       data.nBitsPerSample    ) / 8   &&

             data.nBlockAlign == ( data.nChannels *
                                   data.nBitsPerSample ) / 8;
            if (ret == true) return 1;
            else return 0;
   		}
	}

	public class WaveFileSample
	{
   		public short[] 				chan;
		
		public WaveFileSample()
		{chan = new short[WaveFile.MAX_WAVE_CHANNELS];}
	}

   private WaveFormat_Chunk   	 wave_format;
   private RiffChunkHeader    	 pcm_data;
   private long            		 pcm_data_offset = 0;  // offset of 'pcm_data' in output file
   private int 	           		 num_samples = 0;


   /**
    * Constructs a new WaveFile instance. 
	*/
   public WaveFile()
   {
       pcm_data = new RiffChunkHeader();
       wave_format = new WaveFormat_Chunk();
   	   pcm_data.ckID = FourCC("data");
	   pcm_data.ckSize = 0;
   	   num_samples = 0;
   }

   /**
    *
	*
   public int OpenForRead (String Filename)
   {
      // Verify filename parameter as best we can...
      if (Filename == null)
      {
   	  	return DDC_INVALID_CALL;
      }
      int retcode = Open ( Filename, RFM_READ );
   
      if ( retcode == DDC_SUCCESS )
      {
   	  	retcode = Expect ( "WAVE", 4 );
   
   	  	if ( retcode == DDC_SUCCESS )
   	    {
   		 	retcode = Read(wave_format,24);
   
   		 	if ( retcode == DDC_SUCCESS && !wave_format.VerifyValidity() )
   		    {
   				// This isn't standard PCM, so we don't know what it is!
   				retcode = DDC_FILE_ERROR;
   		    }
   
   		    if ( retcode == DDC_SUCCESS )
   		    {
   			  pcm_data_offset = CurrentFilePosition();
   
   			  // Figure out number of samples from
   			  // file size, current file position, and
   			  // WAVE header.
   			  retcode = Read (pcm_data, 8 );
   			  num_samples = filelength(fileno(file)) - CurrentFilePosition();
   			  num_samples /= NumChannels();
   			  num_samples /= (BitsPerSample() / 8);
   		    }
   	    }
     }
     return retcode;
   }*/

   /**
    *
	*/
   public int OpenForWrite (String Filename, int SamplingRate, short BitsPerSample, short NumChannels)
   {
      // Verify parameters...
      if ( (Filename==null) ||
   		(BitsPerSample != 8 && BitsPerSample != 16) ||
   		NumChannels < 1 || NumChannels > 2 )
      {
   	  	return DDC_INVALID_CALL;
      }
   
      wave_format.data.Config ( SamplingRate, BitsPerSample, NumChannels );
   
      int retcode = Open ( Filename, RFM_WRITE );
   
      if ( retcode == DDC_SUCCESS )
      {
        byte [] theWave = {(byte)'W',(byte)'A',(byte)'V',(byte)'E'};
   	  	retcode = Write ( theWave, 4 );
   
   	  	if ( retcode == DDC_SUCCESS )
   	  	{
            // Ecriture de wave_format
            retcode = Write (wave_format.header, 8);   
            retcode = Write (wave_format.data.wFormatTag, 2);
            retcode = Write (wave_format.data.nChannels, 2);
            retcode = Write (wave_format.data.nSamplesPerSec, 4);
            retcode = Write (wave_format.data.nAvgBytesPerSec, 4);
            retcode = Write (wave_format.data.nBlockAlign, 2);
            retcode = Write (wave_format.data.nBitsPerSample, 2);
            /* byte[] br = new byte[16];
	        br[0] = (byte) ((wave_format.data.wFormatTag >> 8) & 0x00FF);
        	br[1] = (byte) (wave_format.data.wFormatTag & 0x00FF);
	  
        	br[2] = (byte) ((wave_format.data.nChannels >> 8) & 0x00FF);
	        br[3] = (byte) (wave_format.data.nChannels & 0x00FF);
	  
	        br[4] = (byte) ((wave_format.data.nSamplesPerSec >> 24)& 0x000000FF);
	        br[5] = (byte) ((wave_format.data.nSamplesPerSec >> 16)& 0x000000FF);
	        br[6] = (byte) ((wave_format.data.nSamplesPerSec >> 8)& 0x000000FF);
	        br[7] = (byte) (wave_format.data.nSamplesPerSec & 0x000000FF);
	  
	        br[8] = (byte) ((wave_format.data.nAvgBytesPerSec>> 24)& 0x000000FF);
	        br[9] = (byte) ((wave_format.data.nAvgBytesPerSec >> 16)& 0x000000FF);
	        br[10] = (byte) ((wave_format.data.nAvgBytesPerSec >> 8)& 0x000000FF);
	        br[11] = (byte) (wave_format.data.nAvgBytesPerSec & 0x000000FF);

	        br[12] = (byte) ((wave_format.data.nBlockAlign >> 8) & 0x00FF);
	        br[13] = (byte) (wave_format.data.nBlockAlign & 0x00FF);
	  
	        br[14] = (byte) ((wave_format.data.nBitsPerSample >> 8) & 0x00FF);
	        br[15] = (byte) (wave_format.data.nBitsPerSample & 0x00FF);   		 	
   		 	retcode = Write (br, 16); */
   		 	
   
   		 	if ( retcode == DDC_SUCCESS )
   		 	{
   				pcm_data_offset = CurrentFilePosition();
   				retcode = Write ( pcm_data, 8 );
   		 	}
   	  	}
      }
   
   	return retcode;
   }

   /**
    *
	*
   public int ReadSample ( short[] Sample )
   {
   
   }*/
   
   /**
    *
	*
   public int WriteSample( short[] Sample )
   {
      int retcode = DDC_SUCCESS;
      switch ( wave_format.data.nChannels )
      {
   	  	case 1:
   		   switch ( wave_format.data.nBitsPerSample )
   		   {
   			  case 8:
   				   pcm_data.ckSize += 1;
   				   retcode = Write ( Sample, 1 );
   				   break;
   
   			  case 16:
   				   pcm_data.ckSize += 2;
   				   retcode = Write ( Sample, 2 );
   				   break;
   
   			  default:
   				   retcode = DDC_INVALID_CALL;
   		   }
   		   break;
   
   	  	case 2:
   		   switch ( wave_format.data.nBitsPerSample )
   		   {
   			  case 8:
   				   retcode = Write ( Sample, 1 );
   				   if ( retcode == DDC_SUCCESS )
   				   {
   				      // &Sample[1]
   					  retcode = Write (Sample, 1 );
   					  if ( retcode == DDC_SUCCESS )
   					  {
   						 pcm_data.ckSize += 2;
   					  }
   				   }
   				   break;
   
   			  case 16:
   				   retcode = Write ( Sample, 2 );
   				   if ( retcode == DDC_SUCCESS )
   				   {
                      // &Sample[1]
   					  retcode = Write (Sample, 2 );
   					  if ( retcode == DDC_SUCCESS )
   					  {
   						 pcm_data.ckSize += 4;
   					  }
   				   }
   				   break;
   
   			  default:
   				   retcode = DDC_INVALID_CALL;
   		   }
   		   break;
   
   	  	default:
   		   retcode = DDC_INVALID_CALL;
      }
   
   	  return retcode;
	}*/
	
	/**
	 *
	 *
   public int SeekToSample ( long SampleIndex )
   {
      if ( SampleIndex >= NumSamples() )
      {
   	    return DDC_INVALID_CALL;
      }
      int SampleSize = (BitsPerSample() + 7) / 8;
      int rc = Seek ( pcm_data_offset + 8 +
   					  SampleSize * NumChannels() * SampleIndex );
   	  return rc;
   }*/

   /**
    * Write 16-bit audio
	*/
   public int WriteData ( short[] data, int numData )
   {
   	  int extraBytes = numData * 2;
   	  pcm_data.ckSize += extraBytes;
	  return super.Write ( data, extraBytes );
   }

   /**
    * Read 16-bit audio.
	*
   public int ReadData  (short[] data, int numData)
   {return super.Read ( data, numData * 2);} */

   /**
    * Write 8-bit audio.
	*
   public int WriteData ( byte[] data, int numData )
   {
   	  pcm_data.ckSize += numData;
	  return super.Write ( data, numData );
   }*/

   /**
    * Read 8-bit audio.
	*
   public int ReadData ( byte[] data, int numData )
   {return super.Read ( data, numData );} */
   
   
   /**
    *
	*
   public int ReadSamples  (int num, int [] WaveFileSample)
   {
   
   }*/

   /**
    *
	*
   public int WriteMonoSample ( short[] SampleData )
   {
      switch ( wave_format.data.nBitsPerSample )
      {
   	  	case 8:
   		   pcm_data.ckSize += 1;
   		   return Write ( SampleData, 1 );
   
   	  	case 16:
   		   pcm_data.ckSize += 2;
   		   return Write ( SampleData, 2 );
      }
   	  return DDC_INVALID_CALL;
   }*/

   /**
    *
	*
   public int WriteStereoSample  ( short[] LeftSample, short[] RightSample )
   {
      int retcode = DDC_SUCCESS;
      switch ( wave_format.data.nBitsPerSample )
      {
   	  	case 8:
   		   retcode = Write ( LeftSample, 1 );
   		   if ( retcode == DDC_SUCCESS )
   		   {
   			  retcode = Write ( RightSample, 1 );
   			  if ( retcode == DDC_SUCCESS )
   			  {
   				 pcm_data.ckSize += 2;
   			  }
   		   }
   		   break;
   
   	  	case 16:
   		   retcode = Write ( LeftSample, 2 );
   		   if ( retcode == DDC_SUCCESS )
   		   {
   			  retcode = Write ( RightSample, 2 );
   			  if ( retcode == DDC_SUCCESS )
   			  {
   				 pcm_data.ckSize += 4;
   			  }
   		   }
   		   break;
   
   	  	default:
   		   retcode = DDC_INVALID_CALL;
      }   
      return retcode;
   }*/
   
   /**
    *
	*
   public int ReadMonoSample ( short[] Sample )
   {
      int retcode = DDC_SUCCESS;
      switch ( wave_format.data.nBitsPerSample )
      {
   	  	case 8:
   		   byte[] x = {0};
   		   retcode = Read ( x, 1 );
   		   Sample[0] = (short)(x[0]);
   		   break;
   
   	  	case 16:
   		   retcode = Read ( Sample, 2 );
   		   break;
   
   	  default:
   		   retcode = DDC_INVALID_CALL;
      }
   	return retcode;
   }*/

   /**
    *
	*
   public int ReadStereoSample ( short[] LeftSampleData, short[] RightSampleData )
   {
      int retcode = DDC_SUCCESS;
      byte[] x = new byte[2];
      short[] y = new short[2];
      switch ( wave_format.data.nBitsPerSample )
      {
   	  	case 8:
   		   retcode = Read ( x, 2 );
   		   L[0] = (short) ( x[0] );
   		   R[0] = (short) ( x[1] );
   		   break;
   
   	    case 16:
   		   retcode = Read ( y, 4 );
   		   L[0] = (short) ( y[0] );
   		   R[0] = (short) ( y[1] );
   		   break;
   
   	    default:
   		   retcode = DDC_INVALID_CALL;
      }
   	 return retcode;
   }*/

   
   /**
    *
	*/
   public int Close()
   {
      int rc = DDC_SUCCESS;
      
      if ( fmode == RFM_WRITE )
         rc = Backpatch ( pcm_data_offset, pcm_data, 8 );
      if ( rc == DDC_SUCCESS )
   	  rc = super.Close();
      return rc;
   }

   // [Hz]
   public int SamplingRate()
   {return wave_format.data.nSamplesPerSec;}

   public short BitsPerSample()
   {return wave_format.data.nBitsPerSample;}

   public short NumChannels()
   {return wave_format.data.nChannels;}

   public int NumSamples()
   {return num_samples;}

 
   /**
    * Open for write using another wave file's parameters...
	*/
   public int OpenForWrite (String Filename, WaveFile OtherWave )
   {
      return OpenForWrite ( Filename,
                            OtherWave.SamplingRate(),
                            OtherWave.BitsPerSample(),
                            OtherWave.NumChannels() );
   }

   /**
    *
	*/
   public long CurrentFilePosition()
   {
      return super.CurrentFilePosition();
   }

   /* public int FourCC(String ChunkName)
   {
      byte[] p = {0x20,0x20,0x20,0x20};
	  ChunkName.getBytes(0,4,p,0);
	  int ret = (((p[0] << 24)& 0xFF000000) | ((p[1] << 16)&0x00FF0000) | ((p[2] << 8)&0x0000FF00) | (p[3]&0x000000FF));
      return ret;
   }*/

}