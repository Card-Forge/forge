/*
 * 11/19/04	 1.0 moved to LGPL.
 * 
 * 18/06/01  Michael Scheerer,  Fixed bugs which causes
 *           negative indexes in method huffmann_decode and in method 
 *           dequanisize_sample.
 *
 * 16/07/01  Michael Scheerer, Catched a bug in method
 *           huffmann_decode, which causes an outOfIndexException.
 *           Cause : Indexnumber of 24 at SfBandIndex,
 *           which has only a length of 22. I have simply and dirty 
 *           fixed the index to <= 22, because I'm not really be able
 *           to fix the bug. The Indexnumber is taken from the MP3 
 *           file and the origin Ma-Player with the same code works 
 *           well.      
 * 
 * 02/19/99  Java Conversion by E.B, javalayer@javazoom.net
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

package javazoom.jl.decoder;

/**
 * Class Implementing Layer 3 Decoder.
 *
 * @since 0.0
 */
final class LayerIIIDecoder implements FrameDecoder
{
	final double d43 = (4.0/3.0);
	
	public int[]				scalefac_buffer;

	// MDM: removed, as this wasn't being used.
	//private float               CheckSumOut1d = 0.0f;
    private int                 CheckSumHuff = 0;
	private int[] 				is_1d;
    private float[][][]			ro;
    private float[][][]			lr;
	private float[]				out_1d;
    private float[][]		    prevblck;
    private float[][]			k;
    private int[] 				nonzero;
	private Bitstream 			stream;
    private Header 				header;
    private SynthesisFilter 	filter1, filter2;
    private Obuffer 			buffer;
    private int 				which_channels;
    private BitReserve 			br;
    private III_side_info_t 	si;

    private temporaire2[]        III_scalefac_t;
    private temporaire2[]        scalefac;
    // private III_scalefac_t 		scalefac;

    private int 				max_gr;
    private int					frame_start;
    private int 				part2_start;
    private int 				channels;
    private int 				first_channel;
    private int 				last_channel;
    private int					sfreq;


	/**
	 * Constructor.
	 */
	// REVIEW: these constructor arguments should be moved to the
	// decodeFrame() method, where possible, so that one
	public LayerIIIDecoder(Bitstream stream0, Header header0,
   	                        SynthesisFilter filtera, SynthesisFilter filterb,
                            Obuffer buffer0, int which_ch0)
	{
        huffcodetab.inithuff();
		is_1d = new int[SBLIMIT*SSLIMIT+4];
		ro = new float[2][SBLIMIT][SSLIMIT];
		lr = new float[2][SBLIMIT][SSLIMIT];
		out_1d = new float[SBLIMIT*SSLIMIT];
		prevblck = new float[2][SBLIMIT*SSLIMIT];
		k = new float[2][SBLIMIT*SSLIMIT];
		nonzero = new int[2];

        //III_scalefact_t
        III_scalefac_t = new temporaire2[2];
        III_scalefac_t[0] = new temporaire2();
        III_scalefac_t[1] = new temporaire2();
        scalefac = III_scalefac_t;
        // L3TABLE INIT

		sfBandIndex = new SBI[9];	// SZD: MPEG2.5 +3 indices
		int[] l0 = {0,6,12,18,24,30,36,44,54,66,80,96,116,140,168,200,238,284,336,396,464,522,576};
		int[] s0 = {0,4,8,12,18,24,32,42,56,74,100,132,174,192};
		int[] l1 = {0,6,12,18,24,30,36,44,54,66,80,96,114,136,162,194,232,278,330,394,464,540,576};
		int[] s1 = {0,4,8,12,18,26,36,48,62,80,104,136,180,192};
		int[] l2 = {0,6,12,18,24,30,36,44,54,66,80,96,116,140,168,200,238,284,336,396,464,522,576};
		int[] s2 = {0,4,8,12,18,26,36,48,62,80,104,134,174,192};

		int[] l3 = {0,4,8,12,16,20,24,30,36,44,52,62,74,90,110,134,162,196,238,288,342,418,576};
		int[] s3 = {0,4,8,12,16,22,30,40,52,66,84,106,136,192};
		int[] l4 = {0,4,8,12,16,20,24,30,36,42,50,60,72,88,106,128,156,190,230,276,330,384,576};
		int[] s4 = {0,4,8,12,16,22,28,38,50,64,80,100,126,192};
		int[] l5 = {0,4,8,12,16,20,24,30,36,44,54,66,82,102,126,156,194,240,296,364,448,550,576};
		int[] s5 = {0,4,8,12,16,22,30,42,58,78,104,138,180,192};
		// SZD: MPEG2.5
		int[] l6 = {0,6,12,18,24,30,36,44,54,66,80,96,116,140,168,200,238,284,336,396,464,522,576};
		int[] s6 = {0,4,8,12,18,26,36,48,62,80,104,134,174,192};
		int[] l7 = {0,6,12,18,24,30,36,44,54,66,80,96,116,140,168,200,238,284,336,396,464,522,576};
		int[] s7 = {0,4,8,12,18,26,36,48,62,80,104,134,174,192};
		int[] l8 = {0,12,24,36,48,60,72,88,108,132,160,192,232,280,336,400,476,566,568,570,572,574,576};
		int[] s8 = {0,8,16,24,36,52,72,96,124,160,162,164,166,192};

		sfBandIndex[0]= new SBI(l0,s0);
		sfBandIndex[1]= new SBI(l1,s1);
		sfBandIndex[2]= new SBI(l2,s2);

		sfBandIndex[3]= new SBI(l3,s3);
		sfBandIndex[4]= new SBI(l4,s4);
		sfBandIndex[5]= new SBI(l5,s5);
		//SZD: MPEG2.5
		sfBandIndex[6]= new SBI(l6,s6);
		sfBandIndex[7]= new SBI(l7,s7);
		sfBandIndex[8]= new SBI(l8,s8);
		// END OF L3TABLE INIT

		if(reorder_table == null) {	// SZD: generate LUT
			reorder_table = new int[9][];
			for(int i = 0; i < 9; i++)
				reorder_table[i] = reorder(sfBandIndex[i].s);
		}

		// Sftable
		int[] ll0 = {0, 6, 11, 16, 21};
		int[] ss0 = {0, 6, 12};
		sftable = new Sftable(ll0,ss0);
		// END OF Sftable

        // scalefac_buffer
		scalefac_buffer = new int[54];
		// END OF scalefac_buffer

	  	stream         = stream0;
	  	header         = header0;
	  	filter1        = filtera;
	  	filter2        = filterb;
	  	buffer         = buffer0;
	  	which_channels = which_ch0;

	  	frame_start = 0;
	  	channels    = (header.mode() == Header.SINGLE_CHANNEL) ? 1 : 2;
	  	max_gr      = (header.version() == Header.MPEG1) ? 2 : 1;

	  	sfreq       =  header.sample_frequency() +
	                 ((header.version() == Header.MPEG1) ? 3 :
	                 (header.version() == Header.MPEG25_LSF) ? 6 : 0);	// SZD

	  	if (channels == 2)
		{
	  	  switch (which_channels)
		  {
	     	case OutputChannels.LEFT_CHANNEL:
		     case OutputChannels.DOWNMIX_CHANNELS:
		     first_channel = last_channel = 0;
		     break;

		     case OutputChannels.RIGHT_CHANNEL:
		     first_channel = last_channel = 1;
		     break;

		     case OutputChannels.BOTH_CHANNELS:
			  default:
		     first_channel  = 0;
		     last_channel   = 1;
		     break;
	     }
	   }
	   else
	   {
	    first_channel = last_channel = 0;
	   }

	  for(int ch=0;ch<2;ch++)
	     for (int j=0; j<576; j++)
	   		prevblck[ch][j] = 0.0f;

	  nonzero[0] = nonzero[1] = 576;

	  br = new BitReserve();
  	  si = new III_side_info_t();
	}

   /**
    * Notify decoder that a seek is being made.
	*/
   public void seek_notify()
   {
	  frame_start = 0;
	  for(int ch=0;ch<2;ch++)
	  for (int j=0; j<576; j++)
   	   prevblck[ch][j] = 0.0f;
   	  br = new BitReserve();
   }

   public void decodeFrame()
   {
	   decode();
   }

   /**
    * Decode one frame, filling the buffer with the output samples.
	*/

   // subband samples are buffered and passed to the
   // SynthesisFilter in one go.
	private float[] samples1 = new float[32];
	private float[] samples2 = new float[32];

	public void decode()
	{
		int nSlots = header.slots();
	    int flush_main;
	    int gr, ch, ss, sb, sb18;
	    int main_data_end;
		int bytes_to_discard;
	    int i;

		get_side_info();

	    for (i=0; i<nSlots; i++)
	    	br.hputbuf(stream.get_bits(8));

	    main_data_end = br.hsstell() >>> 3; // of previous frame

	    if ((flush_main = (br.hsstell() & 7)) != 0) {
	         br.hgetbits(8 - flush_main);
				main_data_end++;
		 }

		 bytes_to_discard = frame_start - main_data_end
								  - si.main_data_begin;

		 frame_start += nSlots;

	    if (bytes_to_discard < 0)
				return;

		 if (main_data_end > 4096) {
				frame_start -= 4096;
				br.rewindNbytes(4096);
		 }

		 for (; bytes_to_discard > 0; bytes_to_discard--)
	    		br.hgetbits(8);

		 for (gr=0;gr<max_gr;gr++) {

				for (ch=0; ch<channels; ch++) {
	           part2_start = br.hsstell();

	           if (header.version() == Header.MPEG1)
					  get_scale_factors(ch, gr);
			   else  // MPEG-2 LSF, SZD: MPEG-2.5 LSF
	              get_LSF_scale_factors(ch, gr);

				  huffman_decode(ch, gr);
				  // System.out.println("CheckSum HuffMan = " + CheckSumHuff);
				  dequantize_sample(ro[ch], ch, gr);
				}

	         stereo(gr);

	         if ((which_channels == OutputChannels.DOWNMIX_CHANNELS) && (channels > 1))
	         	do_downmix();

	         for (ch=first_channel; ch<=last_channel; ch++) {

	         		reorder(lr[ch], ch, gr);
						antialias(ch, gr);
                 //for (int hb = 0;hb<576;hb++) CheckSumOut1d = CheckSumOut1d + out_1d[hb];
				 //System.out.println("CheckSumOut1d = "+CheckSumOut1d);

	               hybrid(ch, gr);

	             //for (int hb = 0;hb<576;hb++) CheckSumOut1d = CheckSumOut1d + out_1d[hb];
				 //System.out.println("CheckSumOut1d = "+CheckSumOut1d);

						for (sb18=18;sb18<576;sb18+=36) // Frequency inversion
	                   for (ss=1;ss<SSLIMIT;ss+=2)
	                  	  out_1d[sb18 + ss] = -out_1d[sb18 + ss];

						if ((ch == 0) || (which_channels == OutputChannels.RIGHT_CHANNEL)) {
						  for (ss=0;ss<SSLIMIT;ss++) { // Polyphase synthesis
	                  	sb = 0;
	                 		for (sb18=0; sb18<576; sb18+=18) {
								samples1[sb] =  out_1d[sb18+ss];
								//filter1.input_sample(out_1d[sb18+ss], sb);
	                         sb++;
	                     }
	                    	filter1.input_samples(samples1);
							filter1.calculate_pcm_samples(buffer);
						  }
						} else {
						  for (ss=0;ss<SSLIMIT;ss++) { // Polyphase synthesis
	                  	sb = 0;
	                 		for (sb18=0; sb18<576; sb18+=18) {
								samples2[sb] =  out_1d[sb18+ss];
									 //filter2.input_sample(out_1d[sb18+ss], sb);
	                         sb++;
	                     }
	                    	filter2.input_samples(samples2);
							filter2.calculate_pcm_samples(buffer);
						  }

	               }
				}	// channels
		 }	// granule


	        // System.out.println("Counter = ................................."+counter);
  	        //if (counter <  609)
  	        //{
  	            counter++;
  	            buffer.write_buffer(1);
  	        //}
  	        //else if (counter == 609)
  	        //{
  	        //    buffer.close();
  	        //    counter++;
  	        //}
  	        //else
  	        //{
  	        //}

	}

    /**
	 * Reads the side info from the stream, assuming the entire.
	 * frame has been read already.
	 * Mono   : 136 bits (= 17 bytes)
     * Stereo : 256 bits (= 32 bytes)
	 */
	private boolean get_side_info()
	{
		int ch, gr;
		if (header.version() == Header.MPEG1)
		{

			si.main_data_begin = stream.get_bits(9);
			if (channels == 1)
				si.private_bits = stream.get_bits(5);
			else si.private_bits = stream.get_bits(3);

			for (ch=0; ch<channels; ch++) {
				si.ch[ch].scfsi[0] = stream.get_bits(1);
				si.ch[ch].scfsi[1] = stream.get_bits(1);
				si.ch[ch].scfsi[2] = stream.get_bits(1);
				si.ch[ch].scfsi[3] = stream.get_bits(1);
		   }

			for (gr=0; gr<2; gr++) {
				for (ch=0; ch<channels; ch++) {
					si.ch[ch].gr[gr].part2_3_length = stream.get_bits(12);
	  				si.ch[ch].gr[gr].big_values = stream.get_bits(9);
					si.ch[ch].gr[gr].global_gain = stream.get_bits(8);
					si.ch[ch].gr[gr].scalefac_compress = stream.get_bits(4);
					si.ch[ch].gr[gr].window_switching_flag = stream.get_bits(1);
					if ((si.ch[ch].gr[gr].window_switching_flag) != 0) {
						si.ch[ch].gr[gr].block_type       = stream.get_bits(2);
						si.ch[ch].gr[gr].mixed_block_flag = stream.get_bits(1);

						si.ch[ch].gr[gr].table_select[0]  = stream.get_bits(5);
						si.ch[ch].gr[gr].table_select[1]  = stream.get_bits(5);

						si.ch[ch].gr[gr].subblock_gain[0] = stream.get_bits(3);
						si.ch[ch].gr[gr].subblock_gain[1] = stream.get_bits(3);
						si.ch[ch].gr[gr].subblock_gain[2] = stream.get_bits(3);

						// Set region_count parameters since they are implicit in this case.

						if (si.ch[ch].gr[gr].block_type == 0) {
							//	 Side info bad: block_type == 0 in split block
							return false;
						} else if (si.ch[ch].gr[gr].block_type == 2
		  							  && si.ch[ch].gr[gr].mixed_block_flag == 0) {
							si.ch[ch].gr[gr].region0_count = 8;
	               } else {
		               si.ch[ch].gr[gr].region0_count = 7;
	               }
						si.ch[ch].gr[gr].region1_count = 20 -
								si.ch[ch].gr[gr].region0_count;
					} else {
						si.ch[ch].gr[gr].table_select[0] = stream.get_bits(5);
						si.ch[ch].gr[gr].table_select[1] = stream.get_bits(5);
						si.ch[ch].gr[gr].table_select[2] = stream.get_bits(5);
						si.ch[ch].gr[gr].region0_count = stream.get_bits(4);
						si.ch[ch].gr[gr].region1_count = stream.get_bits(3);
						si.ch[ch].gr[gr].block_type = 0;
					}
					si.ch[ch].gr[gr].preflag = stream.get_bits(1);
					si.ch[ch].gr[gr].scalefac_scale = stream.get_bits(1);
					si.ch[ch].gr[gr].count1table_select = stream.get_bits(1);
	         }
	      }

		} else {  	// MPEG-2 LSF, SZD: MPEG-2.5 LSF

	      si.main_data_begin = stream.get_bits(8);
	      if (channels == 1)
	      	si.private_bits = stream.get_bits(1);
	      else si.private_bits = stream.get_bits(2);

	      for (ch=0; ch<channels; ch++) {

	          si.ch[ch].gr[0].part2_3_length = stream.get_bits(12);
	          si.ch[ch].gr[0].big_values = stream.get_bits(9);
	          si.ch[ch].gr[0].global_gain = stream.get_bits(8);
	          si.ch[ch].gr[0].scalefac_compress = stream.get_bits(9);
	          si.ch[ch].gr[0].window_switching_flag = stream.get_bits(1);

	          if ((si.ch[ch].gr[0].window_switching_flag) != 0) {

	             si.ch[ch].gr[0].block_type = stream.get_bits(2);
	             si.ch[ch].gr[0].mixed_block_flag = stream.get_bits(1);
	             si.ch[ch].gr[0].table_select[0] = stream.get_bits(5);
	             si.ch[ch].gr[0].table_select[1] = stream.get_bits(5);

	             si.ch[ch].gr[0].subblock_gain[0] = stream.get_bits(3);
	             si.ch[ch].gr[0].subblock_gain[1] = stream.get_bits(3);
	             si.ch[ch].gr[0].subblock_gain[2] = stream.get_bits(3);

	            // Set region_count parameters since they are implicit in this case.

	             if (si.ch[ch].gr[0].block_type == 0) {
	                // Side info bad: block_type == 0 in split block
	                return false;
	             } else if (si.ch[ch].gr[0].block_type == 2
	                      && si.ch[ch].gr[0].mixed_block_flag == 0) {
	             	 si.ch[ch].gr[0].region0_count = 8;
					 } else {
	             	 si.ch[ch].gr[0].region0_count = 7;
	                si.ch[ch].gr[0].region1_count = 20 -
	                											si.ch[ch].gr[0].region0_count;
	             }

	          } else {
	             si.ch[ch].gr[0].table_select[0] = stream.get_bits(5);
	             si.ch[ch].gr[0].table_select[1] = stream.get_bits(5);
	             si.ch[ch].gr[0].table_select[2] = stream.get_bits(5);
	             si.ch[ch].gr[0].region0_count = stream.get_bits(4);
	             si.ch[ch].gr[0].region1_count = stream.get_bits(3);
	             si.ch[ch].gr[0].block_type = 0;
	          }

	          si.ch[ch].gr[0].scalefac_scale = stream.get_bits(1);
	          si.ch[ch].gr[0].count1table_select = stream.get_bits(1);
	      }   // for(ch=0; ch<channels; ch++)
	   } // if (header.version() == MPEG1)
	  return true;
	}

    /**
	 *
	 */
	private void get_scale_factors(int ch, int gr)
	{
	   int sfb, window;
	   gr_info_s gr_info = (si.ch[ch].gr[gr]);
	   int scale_comp   = gr_info.scalefac_compress;
	   int length0      = slen[0][scale_comp];
	   int length1      = slen[1][scale_comp];

		if ((gr_info.window_switching_flag != 0) && (gr_info.block_type == 2)) {
			if ((gr_info.mixed_block_flag) != 0) { // MIXED
				for (sfb = 0; sfb < 8; sfb++)
					scalefac[ch].l[sfb] = br.hgetbits(
						  slen[0][gr_info.scalefac_compress]);
				for (sfb = 3; sfb < 6; sfb++)
					for (window=0; window<3; window++)
						scalefac[ch].s[window][sfb] = br.hgetbits(
						  slen[0][gr_info.scalefac_compress]);
				for (sfb = 6; sfb < 12; sfb++)
					for (window=0; window<3; window++)
						scalefac[ch].s[window][sfb] = br.hgetbits(
						  slen[1][gr_info.scalefac_compress]);
				for (sfb=12,window=0; window<3; window++)
					scalefac[ch].s[window][sfb] = 0;

	      } else {  // SHORT

	         scalefac[ch].s[0][0]  = br.hgetbits(length0);
	         scalefac[ch].s[1][0]  = br.hgetbits(length0);
	         scalefac[ch].s[2][0]  = br.hgetbits(length0);
	         scalefac[ch].s[0][1]  = br.hgetbits(length0);
	         scalefac[ch].s[1][1]  = br.hgetbits(length0);
	         scalefac[ch].s[2][1]  = br.hgetbits(length0);
	         scalefac[ch].s[0][2]  = br.hgetbits(length0);
	         scalefac[ch].s[1][2]  = br.hgetbits(length0);
	         scalefac[ch].s[2][2]  = br.hgetbits(length0);
	         scalefac[ch].s[0][3]  = br.hgetbits(length0);
	         scalefac[ch].s[1][3]  = br.hgetbits(length0);
	         scalefac[ch].s[2][3]  = br.hgetbits(length0);
	         scalefac[ch].s[0][4]  = br.hgetbits(length0);
	         scalefac[ch].s[1][4]  = br.hgetbits(length0);
	         scalefac[ch].s[2][4]  = br.hgetbits(length0);
	         scalefac[ch].s[0][5]  = br.hgetbits(length0);
	         scalefac[ch].s[1][5]  = br.hgetbits(length0);
	         scalefac[ch].s[2][5]  = br.hgetbits(length0);
	         scalefac[ch].s[0][6]  = br.hgetbits(length1);
	         scalefac[ch].s[1][6]  = br.hgetbits(length1);
	         scalefac[ch].s[2][6]  = br.hgetbits(length1);
	         scalefac[ch].s[0][7]  = br.hgetbits(length1);
	         scalefac[ch].s[1][7]  = br.hgetbits(length1);
	         scalefac[ch].s[2][7]  = br.hgetbits(length1);
	         scalefac[ch].s[0][8]  = br.hgetbits(length1);
	         scalefac[ch].s[1][8]  = br.hgetbits(length1);
	         scalefac[ch].s[2][8]  = br.hgetbits(length1);
	         scalefac[ch].s[0][9]  = br.hgetbits(length1);
	         scalefac[ch].s[1][9]  = br.hgetbits(length1);
	         scalefac[ch].s[2][9]  = br.hgetbits(length1);
	         scalefac[ch].s[0][10] = br.hgetbits(length1);
	         scalefac[ch].s[1][10] = br.hgetbits(length1);
	         scalefac[ch].s[2][10] = br.hgetbits(length1);
	         scalefac[ch].s[0][11] = br.hgetbits(length1);
	         scalefac[ch].s[1][11] = br.hgetbits(length1);
	         scalefac[ch].s[2][11] = br.hgetbits(length1);
	         scalefac[ch].s[0][12] = 0;
				scalefac[ch].s[1][12] = 0;
				scalefac[ch].s[2][12] = 0;
			} // SHORT

		} else {   // LONG types 0,1,3

	      if ((si.ch[ch].scfsi[0] == 0) || (gr == 0)) {
	           scalefac[ch].l[0]  = br.hgetbits(length0);
	           scalefac[ch].l[1]  = br.hgetbits(length0);
	           scalefac[ch].l[2]  = br.hgetbits(length0);
	           scalefac[ch].l[3]  = br.hgetbits(length0);
	           scalefac[ch].l[4]  = br.hgetbits(length0);
	           scalefac[ch].l[5]  = br.hgetbits(length0);
			}
	      if ((si.ch[ch].scfsi[1] == 0) || (gr == 0)) {
	           scalefac[ch].l[6]  = br.hgetbits(length0);
	           scalefac[ch].l[7]  = br.hgetbits(length0);
	           scalefac[ch].l[8]  = br.hgetbits(length0);
	           scalefac[ch].l[9]  = br.hgetbits(length0);
	           scalefac[ch].l[10] = br.hgetbits(length0);
			}
	      if ((si.ch[ch].scfsi[2] == 0) || (gr == 0)) {
	           scalefac[ch].l[11] = br.hgetbits(length1);
	           scalefac[ch].l[12] = br.hgetbits(length1);
	           scalefac[ch].l[13] = br.hgetbits(length1);
	           scalefac[ch].l[14] = br.hgetbits(length1);
	           scalefac[ch].l[15] = br.hgetbits(length1);
			}
	      if ((si.ch[ch].scfsi[3] == 0) || (gr == 0)) {
	           scalefac[ch].l[16] = br.hgetbits(length1);
	           scalefac[ch].l[17] = br.hgetbits(length1);
	           scalefac[ch].l[18] = br.hgetbits(length1);
	           scalefac[ch].l[19] = br.hgetbits(length1);
	           scalefac[ch].l[20] = br.hgetbits(length1);
			}

	      scalefac[ch].l[21] = 0;
		  scalefac[ch].l[22] = 0;
		}
	}

    /**
	 *
	 */
	// MDM: new_slen is fully initialized before use, no need
	// to reallocate array.
	private final int[] new_slen = new int[4];

	private void get_LSF_scale_data(int ch, int gr)
	{

	  	int scalefac_comp, int_scalefac_comp;
	    int mode_ext = header.mode_extension();
		int m;
		int blocktypenumber;
		int blocknumber = 0;

		gr_info_s gr_info = (si.ch[ch].gr[gr]);

		scalefac_comp =  gr_info.scalefac_compress;

	    if (gr_info.block_type == 2) {
	     if (gr_info.mixed_block_flag == 0)
	      	blocktypenumber = 1;
	      else if (gr_info.mixed_block_flag == 1)
				blocktypenumber = 2;
	      else
	      	blocktypenumber = 0;
	    } else {
	   	blocktypenumber = 0;
	    }

	   if(!(((mode_ext == 1) || (mode_ext == 3)) && (ch == 1))) {

			if(scalefac_comp < 400) {

				new_slen[0] = (scalefac_comp >>> 4) / 5 ;
				new_slen[1] = (scalefac_comp >>> 4) % 5 ;
				new_slen[2] = (scalefac_comp & 0xF) >>> 2 ;
				new_slen[3] = (scalefac_comp & 3);
	         	si.ch[ch].gr[gr].preflag = 0;
	         	blocknumber = 0;

	       } else if (scalefac_comp  < 500) {

				new_slen[0] = ((scalefac_comp - 400) >>> 2) / 5 ;
				new_slen[1] = ((scalefac_comp - 400) >>> 2) % 5 ;
				new_slen[2] = (scalefac_comp - 400 ) & 3 ;
				new_slen[3] = 0;
	         	si.ch[ch].gr[gr].preflag = 0;
	         	blocknumber = 1;

		   } else if (scalefac_comp < 512) {

				new_slen[0] = (scalefac_comp - 500 ) / 3 ;
				new_slen[1] = (scalefac_comp - 500)  % 3 ;
				new_slen[2] = 0;
				new_slen[3] = 0;
	      		si.ch[ch].gr[gr].preflag = 1;
		      	blocknumber = 2;
	 	   }
	   }

	   if((((mode_ext == 1) || (mode_ext == 3)) && (ch == 1)))
	   {
	      int_scalefac_comp = scalefac_comp >>> 1;

	      if (int_scalefac_comp < 180)
	      {
				new_slen[0] = int_scalefac_comp  / 36 ;
				new_slen[1] = (int_scalefac_comp % 36 ) / 6 ;
				new_slen[2] = (int_scalefac_comp % 36) % 6;
				new_slen[3] = 0;
	         	si.ch[ch].gr[gr].preflag = 0;
	         	blocknumber = 3;
	      } else if (int_scalefac_comp < 244) {
				new_slen[0] = ((int_scalefac_comp - 180 )  & 0x3F) >>> 4 ;
				new_slen[1] = ((int_scalefac_comp - 180) & 0xF) >>> 2 ;
				new_slen[2] = (int_scalefac_comp - 180 ) & 3 ;
				new_slen[3] = 0;
	         	si.ch[ch].gr[gr].preflag = 0;
	         	blocknumber = 4;
	      } else if (int_scalefac_comp < 255) {
				new_slen[0] = (int_scalefac_comp - 244 ) / 3 ;
				new_slen[1] = (int_scalefac_comp - 244 )  % 3 ;
				new_slen[2] = 0 ;
				new_slen[3] = 0;
	         	si.ch[ch].gr[gr].preflag = 0;
	         	blocknumber = 5;
	      }
	   }

	   for (int x=0; x<45; x++) // why 45, not 54?
	   	scalefac_buffer[x] = 0;

	   m = 0;
	   for (int i=0; i<4;i++) {
	     	for (int j = 0; j < nr_of_sfb_block[blocknumber][blocktypenumber][i];
	      	 j++)
	      {
	        scalefac_buffer[m] = (new_slen[i] == 0) ? 0 :
	        							  br.hgetbits(new_slen[i]);
	        m++;

	      } // for (unint32 j ...
   		} // for (uint32 i ...
	}

	/**
	 *
	 */
    private void get_LSF_scale_factors(int ch, int gr)
	{
		int m = 0;
	    int sfb, window;
		gr_info_s gr_info = (si.ch[ch].gr[gr]);

	    get_LSF_scale_data(ch, gr);

	    if ((gr_info.window_switching_flag != 0) && (gr_info.block_type == 2)) {
	      if (gr_info.mixed_block_flag != 0) { 	// MIXED
	         for (sfb = 0; sfb < 8; sfb++)
	         {
	              scalefac[ch].l[sfb] = scalefac_buffer[m];
	              m++;
	         }
	         for (sfb = 3; sfb < 12; sfb++) {
	            for (window=0; window<3; window++)
	            {
	               scalefac[ch].s[window][sfb] = scalefac_buffer[m];
	               m++;
	            }
	         }
	         for (window=0; window<3; window++)
	            scalefac[ch].s[window][12] = 0;

	      } else {  // SHORT

	           for (sfb = 0; sfb < 12; sfb++) {
	               for (window=0; window<3; window++)
	               {
	                  scalefac[ch].s[window][sfb] = scalefac_buffer[m];
	                  m++;
	               }
	           }

	           for (window=0; window<3; window++)
	               scalefac[ch].s[window][12] = 0;
	      }
	   } else {   // LONG types 0,1,3

	      for (sfb = 0; sfb < 21; sfb++) {
	          scalefac[ch].l[sfb] = scalefac_buffer[m];
	          m++;
	      }
	      scalefac[ch].l[21] = 0; // Jeff
	      scalefac[ch].l[22] = 0;
		}
	}

	/**
	 *
	 */
    int[] x = {0};
	int[] y = {0};
	int[] v = {0};
	int[] w = {0};
	private void huffman_decode(int ch, int gr)
	{
		x[0] = 0;
		y[0] = 0;
		v[0] = 0;
		w[0] = 0;

	   	int part2_3_end = part2_start + si.ch[ch].gr[gr].part2_3_length;
	   	int num_bits;
		int region1Start;
		int region2Start;
	    int index;

	    int buf, buf1;

	 	huffcodetab h;

		// Find region boundary for short block case

		if ( ((si.ch[ch].gr[gr].window_switching_flag) != 0) &&
			  (si.ch[ch].gr[gr].block_type == 2) ) {

			// Region2.
	    //MS: Extrahandling for 8KHZ
	    region1Start = (sfreq == 8) ? 72 : 36;  // sfb[9/3]*3=36 or in case 8KHZ = 72
			region2Start = 576; // No Region2 for short block case

	  } else {          // Find region boundary for long block case

	    buf = si.ch[ch].gr[gr].region0_count + 1;
	    buf1 = buf + si.ch[ch].gr[gr].region1_count + 1;

	    if(buf1 > sfBandIndex[sfreq].l.length - 1) buf1 = sfBandIndex[sfreq].l.length - 1;

			region1Start = sfBandIndex[sfreq].l[buf];
			region2Start = sfBandIndex[sfreq].l[buf1]; /* MI */
	   }

	   index = 0;
		// Read bigvalues area
		for (int i=0; i<(si.ch[ch].gr[gr].big_values<<1); i+=2) {
			if      (i<region1Start) h = huffcodetab.ht[si.ch[ch].gr[gr].table_select[0]];
			else if (i<region2Start) h = huffcodetab.ht[si.ch[ch].gr[gr].table_select[1]];
				  else                h = huffcodetab.ht[si.ch[ch].gr[gr].table_select[2]];

			huffcodetab.huffman_decoder(h, x, y, v, w, br);
		  //if (index >= is_1d.length) System.out.println("i0="+i+"/"+(si.ch[ch].gr[gr].big_values<<1)+" Index="+index+" is_1d="+is_1d.length);
	      
	      is_1d[index++] = x[0];
	      is_1d[index++] = y[0];
	      
	      CheckSumHuff = CheckSumHuff + x[0] + y[0];
	      // System.out.println("x = "+x[0]+" y = "+y[0]);
		}

		// Read count1 area
		h = huffcodetab.ht[si.ch[ch].gr[gr].count1table_select+32];
	    num_bits = br.hsstell();

		while ((num_bits < part2_3_end) && (index < 576)) {

			huffcodetab.huffman_decoder(h, x, y, v, w, br);

	      is_1d[index++] = v[0];
	      is_1d[index++] = w[0];
	      is_1d[index++] = x[0];
	      is_1d[index++] = y[0];
          CheckSumHuff = CheckSumHuff + v[0] + w[0] + x[0] + y[0];
	      // System.out.println("v = "+v[0]+" w = "+w[0]);
	      // System.out.println("x = "+x[0]+" y = "+y[0]);
	      num_bits = br.hsstell();
	   }

		if (num_bits > part2_3_end) {
			br.rewindNbits(num_bits - part2_3_end);
	      index-=4;
	   }

	   num_bits = br.hsstell();

		// Dismiss stuffing bits
		if (num_bits < part2_3_end)
	   	br.hgetbits(part2_3_end - num_bits);

		// Zero out rest

	   if (index < 576)
		   nonzero[ch] = index;
	   else
	   	nonzero[ch] = 576;

	   if (index < 0) index = 0;

	   // may not be necessary
	   for (; index<576; index++)
   		is_1d[index] = 0;
	}

	/**
	 *
	 */
    private void i_stereo_k_values(int is_pos, int io_type, int i)
	{
	   if (is_pos == 0) {
	      k[0][i] = 1.0f;
	      k[1][i] = 1.0f;
	   } else if ((is_pos & 1) != 0) {
			k[0][i] = io[io_type][(is_pos + 1) >>> 1];
	      k[1][i] = 1.0f;
	   } else {
	      k[0][i] = 1.0f;
	      k[1][i] = io[io_type][is_pos >>> 1];
   	   }
	}

	/**
	 *
	 */
	private void dequantize_sample(float xr[][], int ch, int gr)
	{
		gr_info_s gr_info = (si.ch[ch].gr[gr]);
		int  cb=0;
		int  next_cb_boundary;
		int cb_begin = 0;
		int cb_width = 0;
		int  index=0, t_index, j;
	   	float g_gain;
	    float[][] xr_1d = xr;

		// choose correct scalefactor band per block type, initalize boundary

		if ((gr_info.window_switching_flag !=0 ) && (gr_info.block_type == 2) ) {
			if (gr_info.mixed_block_flag != 0)
				next_cb_boundary=sfBandIndex[sfreq].l[1];  // LONG blocks: 0,1,3
			else {
	         cb_width = sfBandIndex[sfreq].s[1];
			   next_cb_boundary = (cb_width << 2) - cb_width;
		 	   cb_begin = 0;
			}
		} else {
			next_cb_boundary=sfBandIndex[sfreq].l[1];  // LONG blocks: 0,1,3
	   }

	   // Compute overall (global) scaling.

		g_gain = (float) Math.pow(2.0 , (0.25 * (gr_info.global_gain - 210.0)));

	  	for (j=0; j<nonzero[ch]; j++)
	  	{
	  	    // Modif E.B 02/22/99
            int reste = j % SSLIMIT;
            int quotien = (int) ((j-reste)/SSLIMIT);
	    	if (is_1d[j] == 0) xr_1d[quotien][reste] = 0.0f;
	        else
	        {
	         int abv = is_1d[j];
	         // Pow Array fix (11/17/04)
	         if (abv < t_43.length)
	         {
				if (is_1d[j] > 0) xr_1d[quotien][reste] = g_gain * t_43[abv];
				else
				{
					if (-abv < t_43.length) xr_1d[quotien][reste] = -g_gain * t_43[-abv];
					else xr_1d[quotien][reste] = -g_gain * (float)Math.pow(-abv, d43);	
				} 
	         }
	         else
	         {
				if (is_1d[j] > 0) xr_1d[quotien][reste] = g_gain * (float)Math.pow(abv, d43);
				else xr_1d[quotien][reste] = -g_gain * (float)Math.pow(-abv, d43);	         	
	         }
	        }
	   }

	   // apply formula per block type
	   for (j=0; j<nonzero[ch]; j++)
	   {
            // Modif E.B 02/22/99
            int reste = j % SSLIMIT;
            int quotien = (int) ((j-reste)/SSLIMIT);

			if (index == next_cb_boundary)  { /* Adjust critical band boundary */
	      	if ((gr_info.window_switching_flag != 0) && (gr_info.block_type == 2)) {
	         	if (gr_info.mixed_block_flag != 0)  {

	            	if (index == sfBandIndex[sfreq].l[8])  {
	                  next_cb_boundary = sfBandIndex[sfreq].s[4];
	                  next_cb_boundary = (next_cb_boundary << 2) -
	                    			           next_cb_boundary;
	                  cb = 3;
	                  cb_width = sfBandIndex[sfreq].s[4] -
	                    			  sfBandIndex[sfreq].s[3];

	                  cb_begin = sfBandIndex[sfreq].s[3];
	                  cb_begin = (cb_begin << 2) - cb_begin;

	               } else if (index < sfBandIndex[sfreq].l[8]) {

	               	next_cb_boundary = sfBandIndex[sfreq].l[(++cb)+1];

	               } else {

	               	next_cb_boundary = sfBandIndex[sfreq].s[(++cb)+1];
	                  next_cb_boundary = (next_cb_boundary << 2) -
	                    				        next_cb_boundary;

	                  cb_begin = sfBandIndex[sfreq].s[cb];
							cb_width = sfBandIndex[sfreq].s[cb+1] -
	                             cb_begin;
	                  cb_begin = (cb_begin << 2) - cb_begin;
	               }

	            } else  {

	               next_cb_boundary = sfBandIndex[sfreq].s[(++cb)+1];
	               next_cb_boundary = (next_cb_boundary << 2) -
	                                  next_cb_boundary;

	               cb_begin = sfBandIndex[sfreq].s[cb];
						cb_width = sfBandIndex[sfreq].s[cb+1] -
	                          cb_begin;
	               cb_begin = (cb_begin << 2) - cb_begin;
	            }

	         } else  { // long blocks

						next_cb_boundary = sfBandIndex[sfreq].l[(++cb)+1];

	         }
	      }

			// Do long/short dependent scaling operations

			if ((gr_info.window_switching_flag !=0)&&
				 (((gr_info.block_type == 2) && (gr_info.mixed_block_flag == 0)) ||
				  ((gr_info.block_type == 2) && (gr_info.mixed_block_flag!=0) && (j >= 36)) ))
	      {

				t_index = (index - cb_begin) / cb_width;
	/*            xr[sb][ss] *= pow(2.0, ((-2.0 * gr_info.subblock_gain[t_index])
	                                    -(0.5 * (1.0 + gr_info.scalefac_scale)
	                                      * scalefac[ch].s[t_index][cb]))); */
				int idx = scalefac[ch].s[t_index][cb]
	           				 << gr_info.scalefac_scale;
	         idx += (gr_info.subblock_gain[t_index] << 2);

				xr_1d[quotien][reste] *= two_to_negative_half_pow[idx];

			} else {   // LONG block types 0,1,3 & 1st 2 subbands of switched blocks
	/*				xr[sb][ss] *= pow(2.0, -0.5 * (1.0+gr_info.scalefac_scale)
														 * (scalefac[ch].l[cb]
														 + gr_info.preflag * pretab[cb])); */
				int idx = scalefac[ch].l[cb];

	   		if (gr_info.preflag != 0)
			   	idx += pretab[cb];

			   idx = idx << gr_info.scalefac_scale;
	         xr_1d[quotien][reste] *= two_to_negative_half_pow[idx];
			}
	      index++;
		}

	   for (j=nonzero[ch]; j<576; j++)
	   {
            // Modif E.B 02/22/99
            int reste = j % SSLIMIT;
            int quotien = (int) ((j-reste)/SSLIMIT);
            if(reste < 0) reste = 0;
            if(quotien < 0) quotien = 0;
	     	xr_1d[quotien][reste] = 0.0f;
	   }

   	   return;
	}

    /**
	 *
	 */
	private void reorder(float xr[][], int ch, int gr)
	{
	   gr_info_s gr_info = (si.ch[ch].gr[gr]);
	   int freq, freq3;
	   int index;
	   int sfb, sfb_start, sfb_lines;
	   int src_line, des_line;
	   float[][] xr_1d = xr;

	   if ((gr_info.window_switching_flag !=0) && (gr_info.block_type == 2)) {

	      for(index=0; index<576; index++)
	         out_1d[index] = 0.0f;

			if (gr_info.mixed_block_flag !=0 ) {
				// NO REORDER FOR LOW 2 SUBBANDS
	            for (index = 0; index < 36; index++)
	            {
                    // Modif E.B 02/22/99
                    int reste = index % SSLIMIT;
                    int quotien = (int) ((index-reste)/SSLIMIT);
	                out_1d[index] = xr_1d[quotien][reste];
	            }
				// REORDERING FOR REST SWITCHED SHORT
				/*for( sfb=3,sfb_start=sfBandIndex[sfreq].s[3],
					 sfb_lines=sfBandIndex[sfreq].s[4] - sfb_start;
					 sfb < 13; sfb++,sfb_start = sfBandIndex[sfreq].s[sfb],
					 sfb_lines = sfBandIndex[sfreq].s[sfb+1] - sfb_start )
					 {*/						   
		 		for( sfb=3; sfb < 13; sfb++)
	            	 {						   
							//System.out.println("sfreq="+sfreq+" sfb="+sfb+" sfBandIndex="+sfBandIndex.length+" sfBandIndex[sfreq].s="+sfBandIndex[sfreq].s.length);
							sfb_start = sfBandIndex[sfreq].s[sfb];
							sfb_lines = sfBandIndex[sfreq].s[sfb+1] - sfb_start;

						   int sfb_start3 = (sfb_start << 2) - sfb_start;

							for(freq=0, freq3=0; freq<sfb_lines;
	                             freq++, freq3+=3) {

								src_line = sfb_start3 + freq;
								des_line = sfb_start3 + freq3;
                                // Modif E.B 02/22/99
                                int reste = src_line % SSLIMIT;
                                int quotien = (int) ((src_line-reste)/SSLIMIT);

								out_1d[des_line] = xr_1d[quotien][reste];
								src_line += sfb_lines;
								des_line++;

								reste = src_line % SSLIMIT;
								quotien = (int) ((src_line-reste)/SSLIMIT);

								out_1d[des_line] = xr_1d[quotien][reste];
								src_line += sfb_lines;
								des_line++;

								reste = src_line % SSLIMIT;
								quotien = (int) ((src_line-reste)/SSLIMIT);

								out_1d[des_line] = xr_1d[quotien][reste];
						   }
	            	  }

			} else {  // pure short
	      	for(index=0;index<576;index++)
	      	{
                int j = reorder_table[sfreq][index];
	            int reste = j % SSLIMIT;
				int quotien = (int) ((j-reste)/SSLIMIT);
	            out_1d[index] = xr_1d[quotien][reste];
	        }
			}
		}
		else {   // long blocks
	      for(index=0; index<576; index++)
	      {
            // Modif E.B 02/22/99
            int reste = index % SSLIMIT;
            int quotien = (int) ((index-reste)/SSLIMIT);
	      	out_1d[index] = xr_1d[quotien][reste];
	      }
		}
	}

	/**
	 *
	 */

	int[] is_pos = new int[576];
	float[] is_ratio = new float[576];

	private void stereo(int gr)
	{
	  int sb, ss;

		if  (channels == 1) { // mono , bypass xr[0][][] to lr[0][][]

			for(sb=0;sb<SBLIMIT;sb++)
				for(ss=0;ss<SSLIMIT;ss+=3) {
					lr[0][sb][ss]   = ro[0][sb][ss];
	            lr[0][sb][ss+1] = ro[0][sb][ss+1];
					lr[0][sb][ss+2] = ro[0][sb][ss+2];
	         }

	   } else {

		gr_info_s gr_info = (si.ch[0].gr[gr]);
	    int mode_ext = header.mode_extension();
		int sfb;
		int i;
	    int lines, temp, temp2;

		boolean ms_stereo = ((header.mode() == Header.JOINT_STEREO) && ((mode_ext & 0x2)!=0));
		boolean i_stereo  = ((header.mode() == Header.JOINT_STEREO) && ((mode_ext & 0x1)!=0));
		boolean lsf = ((header.version() == Header.MPEG2_LSF || header.version() == Header.MPEG25_LSF ));	// SZD

		int io_type = (gr_info.scalefac_compress & 1);

	 	// initialization

	   for (i=0; i<576; i++)
	   {
	   		is_pos[i] = 7;

			is_ratio[i] = 0.0f;
	   }

		if (i_stereo) {
	   	if ((gr_info.window_switching_flag !=0 )&& (gr_info.block_type == 2)) {
	      	if (gr_info.mixed_block_flag != 0) {

	         	 int max_sfb = 0;

					 for (int j=0; j<3; j++) {
	            	 int sfbcnt;
						sfbcnt = 2;
						for( sfb=12; sfb >=3; sfb-- ) {
	               	i = sfBandIndex[sfreq].s[sfb];
							lines = sfBandIndex[sfreq].s[sfb+1] - i;
	                  i = (i << 2) - i + (j+1) * lines - 1;

							while (lines > 0) {
	                  	if (ro[1][i/18][i%18] != 0.0f) {
							// MDM: in java, array access is very slow.
							// Is quicker to compute div and mod values.
						//if (ro[1][ss_div[i]][ss_mod[i]] != 0.0f) {
	                     	sfbcnt = sfb;
									sfb = -10;
									lines = -10;
								}

								lines--;
								i--;

							} // while (lines > 0)

						} // for (sfb=12 ...
						sfb = sfbcnt + 1;

						if (sfb > max_sfb)
							max_sfb = sfb;

						while(sfb < 12) {
	               	temp = sfBandIndex[sfreq].s[sfb];
	               	sb   = sfBandIndex[sfreq].s[sfb+1] - temp;
	                  i    = (temp << 2) - temp + j * sb;

							for ( ; sb > 0; sb--) {
	                  	is_pos[i] = scalefac[1].s[j][sfb];
								if (is_pos[i] != 7)
	                     	if (lsf)
	                           i_stereo_k_values(is_pos[i], io_type, i);
	                        else
	                        	is_ratio[i] = TAN12[is_pos[i]];

								i++;
							} // for (; sb>0...
							sfb++;
						} // while (sfb < 12)
						sfb = sfBandIndex[sfreq].s[10];
	               sb  = sfBandIndex[sfreq].s[11] - sfb;
	               sfb = (sfb << 2) - sfb + j * sb;
	               temp  = sfBandIndex[sfreq].s[11];
	               sb = sfBandIndex[sfreq].s[12] - temp;
	               i = (temp << 2) - temp + j * sb;

						for (; sb > 0; sb--) {
	               	is_pos[i] = is_pos[sfb];

			            if (lsf) {
			               k[0][i] = k[0][sfb];
					         k[1][i] = k[1][sfb];
			            } else {
	     						is_ratio[i] = is_ratio[sfb];
	                  }
							i++;
						} // for (; sb > 0 ...
					 }
					 if (max_sfb <= 3) {
	                i = 2;
						 ss = 17;
						 sb = -1;
						 while (i >= 0) {
	                	if (ro[1][i][ss] != 0.0f) {
	                   	 sb = (i<<4) + (i<<1) + ss;
								 i = -1;
							} else {
	                      ss--;
								 if (ss < 0) {
	                         i--;
									 ss = 17;
								 }
							} // if (ro ...
						 } // while (i>=0)
						 i = 0;
						 while (sfBandIndex[sfreq].l[i] <= sb)
							 i++;
						 sfb = i;
						 i = sfBandIndex[sfreq].l[i];
						 for (; sfb<8; sfb++) {
	                   sb = sfBandIndex[sfreq].l[sfb+1]-sfBandIndex[sfreq].l[sfb];
							 for (; sb>0; sb--) {
	                      is_pos[i] = scalefac[1].l[sfb];
	                   	 if (is_pos[i] != 7)
		                      if (lsf)
	                           i_stereo_k_values(is_pos[i], io_type, i);
	                         else
	                        	is_ratio[i] = TAN12[is_pos[i]];
								 i++;
							 } // for (; sb>0 ...
						 } // for (; sfb<8 ...
					 } // for (j=0 ...
				} else { // if (gr_info.mixed_block_flag)
	         	for (int j=0; j<3; j++) {
	            	int sfbcnt;
						sfbcnt = -1;
						for( sfb=12; sfb >=0; sfb-- )
						{
							temp = sfBandIndex[sfreq].s[sfb];
	                  lines = sfBandIndex[sfreq].s[sfb+1] - temp;
	                  i = (temp << 2) - temp + (j+1) * lines - 1;

							while (lines > 0) {
								if (ro[1][i/18][i%18] != 0.0f) {
								// MDM: in java, array access is very slow.
								// Is quicker to compute div and mod values.
								//if (ro[1][ss_div[i]][ss_mod[i]] != 0.0f) {
	                     	sfbcnt = sfb;
									sfb = -10;
									lines = -10;
								}
								lines--;
								i--;
							} // while (lines > 0) */

						} // for (sfb=12 ...
						sfb = sfbcnt + 1;
						while(sfb<12) {
							temp = sfBandIndex[sfreq].s[sfb];
	                  sb   = sfBandIndex[sfreq].s[sfb+1] - temp;
	                  i    = (temp << 2) - temp + j * sb;
							for ( ; sb > 0; sb--) {
	                  	is_pos[i] = scalefac[1].s[j][sfb];
								if (is_pos[i] != 7)
		                      if (lsf)
	                           i_stereo_k_values(is_pos[i], io_type, i);
	                         else
	                        	is_ratio[i] = TAN12[is_pos[i]];
								i++;
							} // for (; sb>0 ...
							sfb++;
						} // while (sfb<12)

						temp = sfBandIndex[sfreq].s[10];
	               temp2= sfBandIndex[sfreq].s[11];
	               sb   = temp2 - temp;
	               sfb  = (temp << 2) - temp + j * sb;
	               sb   = sfBandIndex[sfreq].s[12] - temp2;
	               i    = (temp2 << 2) - temp2 + j * sb;

						for (; sb>0; sb--) {
	               	is_pos[i] = is_pos[sfb];

			            if (lsf) {
			               k[0][i] = k[0][sfb];
					         k[1][i] = k[1][sfb];
	      		      } else {
	               		is_ratio[i] = is_ratio[sfb];
	                  }
							i++;
						} // for (; sb>0 ...
					} // for (sfb=12
				} // for (j=0 ...
			} else { // if (gr_info.window_switching_flag ...
	      	i = 31;
				ss = 17;
				sb = 0;
				while (i >= 0) {
	         	if (ro[1][i][ss] != 0.0f) {
	            	sb = (i<<4) + (i<<1) + ss;
						i = -1;
					} else {
	            	ss--;
						if (ss < 0) {
	               	i--;
							ss = 17;
						}
					}
				}
				i = 0;
				while (sfBandIndex[sfreq].l[i] <= sb)
					i++;

				sfb = i;
				i = sfBandIndex[sfreq].l[i];
				for (; sfb<21; sfb++) {
	         	sb = sfBandIndex[sfreq].l[sfb+1] - sfBandIndex[sfreq].l[sfb];
	         	for (; sb > 0; sb--) {
	            	is_pos[i] = scalefac[1].l[sfb];
						if (is_pos[i] != 7)
	                  if (lsf)
	                     i_stereo_k_values(is_pos[i], io_type, i);
	                  else
	                   	is_ratio[i] = TAN12[is_pos[i]];
						i++;
					}
				}
				sfb = sfBandIndex[sfreq].l[20];
				for (sb = 576 - sfBandIndex[sfreq].l[21]; (sb > 0) && (i<576); sb--)
				{
	         	is_pos[i] = is_pos[sfb]; // error here : i >=576

	            if (lsf) {
	               k[0][i] = k[0][sfb];
			         k[1][i] = k[1][sfb];
	            } else {
	  					is_ratio[i] = is_ratio[sfb];
	            }
					i++;
				} // if (gr_info.mixed_block_flag)
			} // if (gr_info.window_switching_flag ...
		} // if (i_stereo)

	   	i = 0;
			for(sb=0;sb<SBLIMIT;sb++)
				for(ss=0;ss<SSLIMIT;ss++) {
					if (is_pos[i] == 7) {
						if (ms_stereo) {
							lr[0][sb][ss] = (ro[0][sb][ss]+ro[1][sb][ss]) * 0.707106781f;
							lr[1][sb][ss] = (ro[0][sb][ss]-ro[1][sb][ss]) * 0.707106781f;
						} else {
							lr[0][sb][ss] = ro[0][sb][ss];
							lr[1][sb][ss] = ro[1][sb][ss];
						}
					}
					else if (i_stereo) {

	            	if (lsf) {
	                  lr[0][sb][ss] = ro[0][sb][ss] * k[0][i];
	                  lr[1][sb][ss] = ro[0][sb][ss] * k[1][i];
	               } else {
	               	lr[1][sb][ss] = ro[0][sb][ss] / (float) (1 + is_ratio[i]);
		  				   lr[0][sb][ss] = lr[1][sb][ss] * is_ratio[i];
	               }
					}
	/*				else {
						System.out.println("Error in stereo processing\n");
					} */
	            i++;
				}

    	} // channels == 2

	}

    /**
	 *
	 */
	private void antialias(int ch, int gr)
	{
	   int sb18, ss, sb18lim;
	   gr_info_s gr_info = (si.ch[ch].gr[gr]);
	   // 31 alias-reduction operations between each pair of sub-bands
	   // with 8 butterflies between each pair

		if  ((gr_info.window_switching_flag !=0) && (gr_info.block_type == 2) &&
			 !(gr_info.mixed_block_flag != 0) )
	       return;

		if ((gr_info.window_switching_flag !=0) && (gr_info.mixed_block_flag != 0)&&
		    (gr_info.block_type == 2)) {
	      sb18lim = 18;
		} else {
			sb18lim = 558;
	   }

	   for (sb18=0; sb18 < sb18lim; sb18+=18) {
	      for (ss=0;ss<8;ss++) {
	      	int src_idx1 = sb18 + 17 - ss;
	         int src_idx2 = sb18 + 18 + ss;
	      	float bu = out_1d[src_idx1];
				float bd = out_1d[src_idx2];
				out_1d[src_idx1] = (bu * cs[ss]) - (bd * ca[ss]);
				out_1d[src_idx2] = (bd * cs[ss]) + (bu * ca[ss]);
	      }
   	  }
	}

	/**
	 *
	 */

	// MDM: tsOutCopy and rawout do not need initializing, so the arrays
	// can be reused.
	float[] tsOutCopy = new float[18];
	float[] rawout = new float[36];

	private void hybrid(int ch, int gr)
	{
	   int bt;
	   int sb18;
	   gr_info_s gr_info = (si.ch[ch].gr[gr]);
	   float[] tsOut;

	   float[][] prvblk;

	   for(sb18=0;sb18<576;sb18+=18)
	   {
			bt = ((gr_info.window_switching_flag !=0 ) && (gr_info.mixed_block_flag !=0) &&
					 (sb18 < 36)) ? 0 : gr_info.block_type;

		   tsOut = out_1d;
	       // Modif E.B 02/22/99
	       for (int cc = 0;cc<18;cc++)
			   tsOutCopy[cc] = tsOut[cc+sb18];

		   inv_mdct(tsOutCopy, rawout, bt);


		   for (int cc = 0;cc<18;cc++)
			   tsOut[cc+sb18] = tsOutCopy[cc];
		   // Fin Modif

			// overlap addition
		   prvblk = prevblck;

		   tsOut[0 + sb18]   = rawout[0]  + prvblk[ch][sb18 + 0];
		   prvblk[ch][sb18 + 0]  = rawout[18];
		   tsOut[1 + sb18]   = rawout[1]  + prvblk[ch][sb18 + 1];
		   prvblk[ch][sb18 + 1]  = rawout[19];
		   tsOut[2 + sb18]   = rawout[2]  + prvblk[ch][sb18 + 2];
		   prvblk[ch][sb18 + 2]  = rawout[20];
		   tsOut[3 + sb18]   = rawout[3]  + prvblk[ch][sb18 + 3];
		   prvblk[ch][sb18 + 3]  = rawout[21];
		   tsOut[4 + sb18]   = rawout[4]  + prvblk[ch][sb18 + 4];
		   prvblk[ch][sb18 + 4]  = rawout[22];
		   tsOut[5 + sb18]   = rawout[5]  + prvblk[ch][sb18 + 5];
		   prvblk[ch][sb18 + 5]  = rawout[23];
		   tsOut[6 + sb18]   = rawout[6]  + prvblk[ch][sb18 + 6];
		   prvblk[ch][sb18 + 6]  = rawout[24];
		   tsOut[7 + sb18]   = rawout[7]  + prvblk[ch][sb18 + 7];
		   prvblk[ch][sb18 + 7]  = rawout[25];
		   tsOut[8 + sb18]   = rawout[8]  + prvblk[ch][sb18 + 8];
		   prvblk[ch][sb18 + 8]  = rawout[26];
		   tsOut[9 + sb18]   = rawout[9]  + prvblk[ch][sb18 + 9];
		   prvblk[ch][sb18 + 9]  = rawout[27];
	   	   tsOut[10 + sb18]  = rawout[10] + prvblk[ch][sb18 + 10];
	   	   prvblk[ch][sb18 + 10] = rawout[28];
	 	   tsOut[11 + sb18]  = rawout[11] + prvblk[ch][sb18 + 11];
	 	   prvblk[ch][sb18 + 11] = rawout[29];
	 	   tsOut[12 + sb18]  = rawout[12] + prvblk[ch][sb18 + 12];
	 	   prvblk[ch][sb18 + 12] = rawout[30];
	 	   tsOut[13 + sb18]  = rawout[13] + prvblk[ch][sb18 + 13];
	 	   prvblk[ch][sb18 + 13] = rawout[31];
	 	   tsOut[14 + sb18]  = rawout[14] + prvblk[ch][sb18 + 14];
	 	   prvblk[ch][sb18 + 14] = rawout[32];
	 	   tsOut[15 + sb18]  = rawout[15] + prvblk[ch][sb18 + 15];
	 	   prvblk[ch][sb18 + 15] = rawout[33];
	 	   tsOut[16 + sb18]  = rawout[16] + prvblk[ch][sb18 + 16];
	 	   prvblk[ch][sb18 + 16] = rawout[34];
	 	   tsOut[17 + sb18]  = rawout[17] + prvblk[ch][sb18 + 17];
	 	   prvblk[ch][sb18 + 17] = rawout[35];
   	  }
	}

    /**
	 *
	 */
	private void do_downmix()
	{
		for (int sb=0; sb<SSLIMIT; sb++) {
	   	for (int ss=0; ss<SSLIMIT; ss+=3) {
	      	lr[0][sb][ss]   = (lr[0][sb][ss]   + lr[1][sb][ss])   * 0.5f;
	      	lr[0][sb][ss+1] = (lr[0][sb][ss+1] + lr[1][sb][ss+1]) * 0.5f;
	      	lr[0][sb][ss+2] = (lr[0][sb][ss+2] + lr[1][sb][ss+2]) * 0.5f;
	      }
   		}
	}

	/**
	 * Fast INV_MDCT.
	 */

	public void inv_mdct(float[] in, float[] out, int block_type)
	{
		 float[] win_bt;
	     int   i;

		float tmpf_0, tmpf_1, tmpf_2, tmpf_3, tmpf_4, tmpf_5, tmpf_6, tmpf_7, tmpf_8, tmpf_9;
		float tmpf_10, tmpf_11, tmpf_12, tmpf_13, tmpf_14, tmpf_15, tmpf_16, tmpf_17;

		tmpf_0 = tmpf_1 = tmpf_2 = tmpf_3 = tmpf_4 = tmpf_5 = tmpf_6 = tmpf_7 = tmpf_8 = tmpf_9 =
		tmpf_10 = tmpf_11 = tmpf_12 = tmpf_13 = tmpf_14 = tmpf_15 = tmpf_16 = tmpf_17 = 0.0f;



		 if(block_type == 2)
		 {

	/*
	 *
	 *		Under MicrosoftVM 2922, This causes a GPF, or
	 *		At best, an ArrayIndexOutOfBoundsExceptin.
			for(int p=0;p<36;p+=9)
		   {
		   	  out[p]   = out[p+1] = out[p+2] = out[p+3] =
		      out[p+4] = out[p+5] = out[p+6] = out[p+7] =
		      out[p+8] = 0.0f;
		   }
	*/
			out[0] = 0.0f;
			out[1] = 0.0f;
			out[2] = 0.0f;
			out[3] = 0.0f;
			out[4] = 0.0f;
			out[5] = 0.0f;
			out[6] = 0.0f;
			out[7] = 0.0f;
			out[8] = 0.0f;
			out[9] = 0.0f;
			out[10] = 0.0f;
			out[11] = 0.0f;
			out[12] = 0.0f;
			out[13] = 0.0f;
			out[14] = 0.0f;
			out[15] = 0.0f;
			out[16] = 0.0f;
			out[17] = 0.0f;
			out[18] = 0.0f;
			out[19] = 0.0f;
			out[20] = 0.0f;
			out[21] = 0.0f;
			out[22] = 0.0f;
			out[23] = 0.0f;
			out[24] = 0.0f;
			out[25] = 0.0f;
			out[26] = 0.0f;
			out[27] = 0.0f;
			out[28] = 0.0f;
			out[29] = 0.0f;
			out[30] = 0.0f;
			out[31] = 0.0f;
			out[32] = 0.0f;
			out[33] = 0.0f;
			out[34] = 0.0f;
			out[35] = 0.0f;

	       int six_i = 0;

		   for(i=0;i<3;i++)
	   	   {
	      		// 12 point IMDCT
	       		// Begin 12 point IDCT
	   			// Input aliasing for 12 pt IDCT
		   		in[15+i] += in[12+i]; in[12+i] += in[9+i]; in[9+i]  +=  in[6+i];
	   			in[6+i]  += in[3+i];  in[3+i]  += in[0+i];

		   		// Input aliasing on odd indices (for 6 point IDCT)
	   			in[15+i] += in[9+i];  in[9+i]  += in[3+i];

		   		// 3 point IDCT on even indices
		  		float 	pp1, pp2, sum;
		    	pp2 = in[12+i] * 0.500000000f;
		   		pp1 = in[ 6+i] * 0.866025403f;
		   		sum = in[0+i] + pp2;
		   		tmpf_1 = in[0+i] - in[12+i];
		   		tmpf_0 = sum + pp1;
		   		tmpf_2 = sum - pp1;

	      		// End 3 point IDCT on even indices
		   		// 3 point IDCT on odd indices (for 6 point IDCT)
		    	pp2 = in[15+i] * 0.500000000f;
	   			pp1 = in[ 9+i] * 0.866025403f;
		   		sum = in[ 3+i] + pp2;
		   		tmpf_4 = in[3+i] - in[15+i];
		   		tmpf_5 = sum + pp1;
		   		tmpf_3 = sum - pp1;
	   	    	// End 3 point IDCT on odd indices
	   			// Twiddle factors on odd indices (for 6 point IDCT)

	   			tmpf_3 *= 1.931851653f;
	   			tmpf_4 *= 0.707106781f;
	   			tmpf_5 *= 0.517638090f;

		   		// Output butterflies on 2 3 point IDCT's (for 6 point IDCT)
	   			float save = tmpf_0;
	   			tmpf_0 += tmpf_5;
	   			tmpf_5 = save - tmpf_5;
		   		save = tmpf_1;
		   		tmpf_1 += tmpf_4;
		   		tmpf_4 = save - tmpf_4;
		   		save = tmpf_2;
		   		tmpf_2 += tmpf_3;
		   		tmpf_3 = save - tmpf_3;

	   			// End 6 point IDCT
		   		// Twiddle factors on indices (for 12 point IDCT)

		   		tmpf_0  *=  0.504314480f;
		   		tmpf_1  *=  0.541196100f;
		   		tmpf_2  *=  0.630236207f;
		   		tmpf_3  *=  0.821339815f;
		   		tmpf_4  *=  1.306562965f;
		   		tmpf_5  *=  3.830648788f;

	      		// End 12 point IDCT

		   		// Shift to 12 point modified IDCT, multiply by window type 2
		   		tmpf_8  = -tmpf_0 * 0.793353340f;
		   		tmpf_9  = -tmpf_0 * 0.608761429f;
		   		tmpf_7  = -tmpf_1 * 0.923879532f;
		   		tmpf_10 = -tmpf_1 * 0.382683432f;
		   		tmpf_6  = -tmpf_2 * 0.991444861f;
		   		tmpf_11 = -tmpf_2 * 0.130526192f;

		   		tmpf_0  =  tmpf_3;
		   		tmpf_1  =  tmpf_4 * 0.382683432f;
		   		tmpf_2  =  tmpf_5 * 0.608761429f;

		   		tmpf_3  = -tmpf_5 * 0.793353340f;
	   			tmpf_4  = -tmpf_4 * 0.923879532f;
		   		tmpf_5  = -tmpf_0 * 0.991444861f;

		   		tmpf_0 *= 0.130526192f;

	   			out[six_i + 6]  += tmpf_0;
				out[six_i + 7]  += tmpf_1;
		   		out[six_i + 8]  += tmpf_2;
				out[six_i + 9]  += tmpf_3;
	   			out[six_i + 10] += tmpf_4;
				out[six_i + 11] += tmpf_5;
		   		out[six_i + 12] += tmpf_6;
				out[six_i + 13] += tmpf_7;
		   		out[six_i + 14] += tmpf_8;
				out[six_i + 15] += tmpf_9;
		   		out[six_i + 16] += tmpf_10;
				out[six_i + 17] += tmpf_11;

	   			six_i += 6;
	   		}
		 }
		 else
		 {
	   		// 36 point IDCT
	   		// input aliasing for 36 point IDCT
	   		in[17]+=in[16]; in[16]+=in[15]; in[15]+=in[14]; in[14]+=in[13];
	   		in[13]+=in[12]; in[12]+=in[11]; in[11]+=in[10]; in[10]+=in[9];
	   		in[9] +=in[8];  in[8] +=in[7];  in[7] +=in[6];  in[6] +=in[5];
	   		in[5] +=in[4];  in[4] +=in[3];  in[3] +=in[2];  in[2] +=in[1];
	   		in[1] +=in[0];

	   		// 18 point IDCT for odd indices
	   		// input aliasing for 18 point IDCT
	   		in[17]+=in[15]; in[15]+=in[13]; in[13]+=in[11]; in[11]+=in[9];
	   		in[9] +=in[7];  in[7] +=in[5];  in[5] +=in[3];  in[3] +=in[1];

	   		float tmp0,tmp1,tmp2,tmp3,tmp4,tmp0_,tmp1_,tmp2_,tmp3_;
	   		float tmp0o,tmp1o,tmp2o,tmp3o,tmp4o,tmp0_o,tmp1_o,tmp2_o,tmp3_o;

			// Fast 9 Point Inverse Discrete Cosine Transform
			//
			// By  Francois-Raymond Boyer
			//         mailto:boyerf@iro.umontreal.ca
			//         http://www.iro.umontreal.ca/~boyerf
			//
			// The code has been optimized for Intel processors
			//  (takes a lot of time to convert float to and from iternal FPU representation)
			//
			// It is a simple "factorization" of the IDCT matrix.

	   		// 9 point IDCT on even indices

			// 5 points on odd indices (not realy an IDCT)
	   		float i00 = in[0]+in[0];
	   		float iip12 = i00 + in[12];

	   		tmp0 = iip12 + in[4]*1.8793852415718f  + in[8]*1.532088886238f   + in[16]*0.34729635533386f;
	   		tmp1 = i00    + in[4]                   - in[8] - in[12] - in[12] - in[16];
	   		tmp2 = iip12 - in[4]*0.34729635533386f - in[8]*1.8793852415718f  + in[16]*1.532088886238f;
	   		tmp3 = iip12 - in[4]*1.532088886238f   + in[8]*0.34729635533386f - in[16]*1.8793852415718f;
	   		tmp4 = in[0] - in[4]                   + in[8] - in[12]          + in[16];

			// 4 points on even indices
	   		float i66_ = in[6]*1.732050808f;		// Sqrt[3]

	   		tmp0_ = in[2]*1.9696155060244f  + i66_ + in[10]*1.2855752193731f  + in[14]*0.68404028665134f;
	   		tmp1_ = (in[2]                        - in[10]                   - in[14])*1.732050808f;
	   		tmp2_ = in[2]*1.2855752193731f  - i66_ - in[10]*0.68404028665134f + in[14]*1.9696155060244f;
	   		tmp3_ = in[2]*0.68404028665134f - i66_ + in[10]*1.9696155060244f  - in[14]*1.2855752193731f;

	   		// 9 point IDCT on odd indices
			// 5 points on odd indices (not realy an IDCT)
	   		float i0 = in[0+1]+in[0+1];
	   		float i0p12 = i0 + in[12+1];

	   		tmp0o = i0p12   + in[4+1]*1.8793852415718f  + in[8+1]*1.532088886238f       + in[16+1]*0.34729635533386f;
	   		tmp1o = i0      + in[4+1]                   - in[8+1] - in[12+1] - in[12+1] - in[16+1];
	   		tmp2o = i0p12   - in[4+1]*0.34729635533386f - in[8+1]*1.8793852415718f      + in[16+1]*1.532088886238f;
	   		tmp3o = i0p12   - in[4+1]*1.532088886238f   + in[8+1]*0.34729635533386f     - in[16+1]*1.8793852415718f;
	   		tmp4o = (in[0+1] - in[4+1]                   + in[8+1] - in[12+1]            + in[16+1])*0.707106781f; // Twiddled

			// 4 points on even indices
	   		float i6_ = in[6+1]*1.732050808f;		// Sqrt[3]

	   		tmp0_o = in[2+1]*1.9696155060244f  + i6_ + in[10+1]*1.2855752193731f  + in[14+1]*0.68404028665134f;
	   		tmp1_o = (in[2+1]                        - in[10+1]                   - in[14+1])*1.732050808f;
	   		tmp2_o = in[2+1]*1.2855752193731f  - i6_ - in[10+1]*0.68404028665134f + in[14+1]*1.9696155060244f;
	   		tmp3_o = in[2+1]*0.68404028665134f - i6_ + in[10+1]*1.9696155060244f  - in[14+1]*1.2855752193731f;

	   		// Twiddle factors on odd indices
	   		// and
	   		// Butterflies on 9 point IDCT's
	   		// and
	   		// twiddle factors for 36 point IDCT

	   		float e, o;
	   		e = tmp0 + tmp0_; o = (tmp0o + tmp0_o)*0.501909918f; tmpf_0 = e + o;    tmpf_17 = e - o;
	   		e = tmp1 + tmp1_; o = (tmp1o + tmp1_o)*0.517638090f; tmpf_1 = e + o;    tmpf_16 = e - o;
	   		e = tmp2 + tmp2_; o = (tmp2o + tmp2_o)*0.551688959f; tmpf_2 = e + o;    tmpf_15 = e - o;
	   		e = tmp3 + tmp3_; o = (tmp3o + tmp3_o)*0.610387294f; tmpf_3 = e + o;    tmpf_14 = e - o;
	   		tmpf_4 = tmp4 + tmp4o; tmpf_13 = tmp4 - tmp4o;
	   		e = tmp3 - tmp3_; o = (tmp3o - tmp3_o)*0.871723397f; tmpf_5 = e + o;    tmpf_12 = e - o;
	   		e = tmp2 - tmp2_; o = (tmp2o - tmp2_o)*1.183100792f; tmpf_6 = e + o;    tmpf_11 = e - o;
	   		e = tmp1 - tmp1_; o = (tmp1o - tmp1_o)*1.931851653f; tmpf_7 = e + o;    tmpf_10 = e - o;
	   		e = tmp0 - tmp0_; o = (tmp0o - tmp0_o)*5.736856623f; tmpf_8 = e + o;    tmpf_9 =  e - o;

	   		// end 36 point IDCT */
			// shift to modified IDCT
	   		win_bt = win[block_type];

			out[0] =-tmpf_9  * win_bt[0];
	   		out[1] =-tmpf_10 * win_bt[1];
			out[2] =-tmpf_11 * win_bt[2];
	   		out[3] =-tmpf_12 * win_bt[3];
	   		out[4] =-tmpf_13 * win_bt[4];
			out[5] =-tmpf_14 * win_bt[5];
			out[6] =-tmpf_15 * win_bt[6];
			out[7] =-tmpf_16 * win_bt[7];
			out[8] =-tmpf_17 * win_bt[8];
	   		out[9] = tmpf_17 * win_bt[9];
	   		out[10]= tmpf_16 * win_bt[10];
			out[11]= tmpf_15 * win_bt[11];
			out[12]= tmpf_14 * win_bt[12];
			out[13]= tmpf_13 * win_bt[13];
			out[14]= tmpf_12 * win_bt[14];
	   		out[15]= tmpf_11 * win_bt[15];
			out[16]= tmpf_10 * win_bt[16];
			out[17]= tmpf_9  * win_bt[17];
			out[18]= tmpf_8  * win_bt[18];
	   		out[19]= tmpf_7  * win_bt[19];
			out[20]= tmpf_6  * win_bt[20];
	   		out[21]= tmpf_5  * win_bt[21];
			out[22]= tmpf_4  * win_bt[22];
			out[23]= tmpf_3  * win_bt[23];
	 		out[24]= tmpf_2  * win_bt[24];
	   		out[25]= tmpf_1  * win_bt[25];
			out[26]= tmpf_0  * win_bt[26];
	   		out[27]= tmpf_0  * win_bt[27];
			out[28]= tmpf_1  * win_bt[28];
			out[29]= tmpf_2  * win_bt[29];
			out[30]= tmpf_3  * win_bt[30];
			out[31]= tmpf_4  * win_bt[31];
	   		out[32]= tmpf_5  * win_bt[32];
			out[33]= tmpf_6  * win_bt[33];
			out[34]= tmpf_7  * win_bt[34];
			out[35]= tmpf_8  * win_bt[35];
		}
	}

    private int counter = 0;
	private static final int		SSLIMIT=18;
	private static final int		SBLIMIT=32;
    // Size of the table of whole numbers raised to 4/3 power.
    // This may be adjusted for performance without any problems.
    //public static final int 	POW_TABLE_LIMIT=512;

    /************************************************************/
	/*                            L3TABLE                       */
	/************************************************************/

	static class SBI
	{
	   public int[] 		l;
   	   public int[] 		s;

	   public SBI()
	   {
	   		l = new int[23];
			s = new int[14];
	   }
	   public SBI(int[] thel, int[] thes)
	   {
	   		l = thel;
			s = thes;
	   }
	}

	static class gr_info_s
	{
		public int 		part2_3_length = 0;
		public int 		big_values = 0;
		public int 		global_gain = 0;
		public int 		scalefac_compress = 0;
		public int 		window_switching_flag = 0;
		public int 		block_type = 0;
		public int 		mixed_block_flag = 0;
		public int[]	table_select;
		public int[]	subblock_gain;
		public int 		region0_count = 0;
		public int 		region1_count = 0;
		public int 		preflag = 0;
		public int 		scalefac_scale = 0;
		public int 		count1table_select = 0;

		/**
		 * Dummy Constructor
		 */
		public gr_info_s()
		{
			table_select = new int[3];
			subblock_gain = new int[3];
		}
	}

	static class temporaire
	{
		public int[]			scfsi;
		public gr_info_s[] 		gr;

		/**
		 * Dummy Constructor
		 */
		public temporaire()
		{
			scfsi = new int[4];
			gr = new gr_info_s[2];
			gr[0] = new gr_info_s();
			gr[1] = new gr_info_s();
		}
	}

	static class III_side_info_t
	{

		public int 				main_data_begin = 0;
		public int 				private_bits = 0;
		public temporaire[]		ch;
	   	/**
	   	 * Dummy Constructor
	   	 */
	   	public III_side_info_t()
	   	{
	   			ch = new temporaire[2];
	   			ch[0] = new temporaire();
	   			ch[1] = new temporaire();
		}
	}

	static class temporaire2
	{
		public int[]		 l;         /* [cb] */
        public int[][]		 s;         /* [window][cb] */

	   	/**
	   	 * Dummy Constructor
	   	 */
	   	public temporaire2()
	   	{
	   		l = new int[23];
			s = new int[3][13];
		}
	}
	//class III_scalefac_t
	//{
	//    public temporaire2[]    tab;
	//   	/**
	//   	 * Dummy Constructor
	//   	 */
	//   	public III_scalefac_t()
	//   	{
	//   		tab = new temporaire2[2];
	//	}
	//}

	private static final int slen[][] =
	{
	 {0, 0, 0, 0, 3, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4},
	 {0, 1, 2, 3, 0, 1, 2, 3, 1, 2, 3, 1, 2, 3, 2, 3}
	};

	public static final int pretab[] =
	{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 3, 2, 0};

    private SBI[]			sfBandIndex; // Init in the constructor.

	public static final float two_to_negative_half_pow[] =
	{ 1.0000000000E+00f, 7.0710678119E-01f, 5.0000000000E-01f, 3.5355339059E-01f,
	  2.5000000000E-01f, 1.7677669530E-01f, 1.2500000000E-01f, 8.8388347648E-02f,
	  6.2500000000E-02f, 4.4194173824E-02f, 3.1250000000E-02f, 2.2097086912E-02f,
	  1.5625000000E-02f, 1.1048543456E-02f, 7.8125000000E-03f, 5.5242717280E-03f,
	  3.9062500000E-03f, 2.7621358640E-03f, 1.9531250000E-03f, 1.3810679320E-03f,
	  9.7656250000E-04f, 6.9053396600E-04f, 4.8828125000E-04f, 3.4526698300E-04f,
	  2.4414062500E-04f, 1.7263349150E-04f, 1.2207031250E-04f, 8.6316745750E-05f,
	  6.1035156250E-05f, 4.3158372875E-05f, 3.0517578125E-05f, 2.1579186438E-05f,
	  1.5258789062E-05f, 1.0789593219E-05f, 7.6293945312E-06f, 5.3947966094E-06f,
	  3.8146972656E-06f, 2.6973983047E-06f, 1.9073486328E-06f, 1.3486991523E-06f,
	  9.5367431641E-07f, 6.7434957617E-07f, 4.7683715820E-07f, 3.3717478809E-07f,
	  2.3841857910E-07f, 1.6858739404E-07f, 1.1920928955E-07f, 8.4293697022E-08f,
	  5.9604644775E-08f, 4.2146848511E-08f, 2.9802322388E-08f, 2.1073424255E-08f,
	  1.4901161194E-08f, 1.0536712128E-08f, 7.4505805969E-09f, 5.2683560639E-09f,
	  3.7252902985E-09f, 2.6341780319E-09f, 1.8626451492E-09f, 1.3170890160E-09f,
	  9.3132257462E-10f, 6.5854450798E-10f, 4.6566128731E-10f, 3.2927225399E-10f
	};


	public static final float t_43[] = create_t_43();

	static private float[] create_t_43()
	{
		float[] t43 = new float[8192];
		final double d43 = (4.0/3.0);

		for (int i=0; i<8192; i++)
		{
			t43[i] = (float)Math.pow(i, d43);
		}
		return t43;
	}

	public static final float io[][] =
	{
	 { 1.0000000000E+00f, 8.4089641526E-01f, 7.0710678119E-01f, 5.9460355751E-01f,
	   5.0000000001E-01f, 4.2044820763E-01f, 3.5355339060E-01f, 2.9730177876E-01f,
	   2.5000000001E-01f, 2.1022410382E-01f, 1.7677669530E-01f, 1.4865088938E-01f,
	   1.2500000000E-01f, 1.0511205191E-01f, 8.8388347652E-02f, 7.4325444691E-02f,
	   6.2500000003E-02f, 5.2556025956E-02f, 4.4194173826E-02f, 3.7162722346E-02f,
	   3.1250000002E-02f, 2.6278012978E-02f, 2.2097086913E-02f, 1.8581361173E-02f,
	   1.5625000001E-02f, 1.3139006489E-02f, 1.1048543457E-02f, 9.2906805866E-03f,
	   7.8125000006E-03f, 6.5695032447E-03f, 5.5242717285E-03f, 4.6453402934E-03f },
	 { 1.0000000000E+00f, 7.0710678119E-01f, 5.0000000000E-01f, 3.5355339060E-01f,
	   2.5000000000E-01f, 1.7677669530E-01f, 1.2500000000E-01f, 8.8388347650E-02f,
	   6.2500000001E-02f, 4.4194173825E-02f, 3.1250000001E-02f, 2.2097086913E-02f,
	   1.5625000000E-02f, 1.1048543456E-02f, 7.8125000002E-03f, 5.5242717282E-03f,
	   3.9062500001E-03f, 2.7621358641E-03f, 1.9531250001E-03f, 1.3810679321E-03f,
	   9.7656250004E-04f, 6.9053396603E-04f, 4.8828125002E-04f, 3.4526698302E-04f,
	   2.4414062501E-04f, 1.7263349151E-04f, 1.2207031251E-04f, 8.6316745755E-05f,
	   6.1035156254E-05f, 4.3158372878E-05f, 3.0517578127E-05f, 2.1579186439E-05f }
	};



	public static final float TAN12[] =
	{
	 0.0f, 0.26794919f, 0.57735027f, 1.0f,
	 1.73205081f, 3.73205081f, 9.9999999e10f, -3.73205081f,
	 -1.73205081f, -1.0f, -0.57735027f, -0.26794919f,
	 0.0f, 0.26794919f, 0.57735027f, 1.0f
	};

	// REVIEW: in java, the array lookup may well be slower than
	// the actual calculation
	// 576 / 18
/*
	private static final int ss_div[] =
	{
		 0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,
		 1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,  1,
		 2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,  2,
		 3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,  3,
		 4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,  4,
		 5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,  5,
		 6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,  6,
		 7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,  7,
		 8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,  8,
		 9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,  9,
		10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
		11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
		12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
		13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
		14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
		15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
		16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
		17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17,
		18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18, 18,
		19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19,
		20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
		21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
		22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
		23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
		24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
		25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
		26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
		27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
		28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
		29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
		30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30,
		31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31
	};

	// 576 % 18
	private static final int ss_mod[] =
	{
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17,
		 0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16, 17
	};
*/
	private static /*final*/ int reorder_table[][]/* = loadReorderTable()*/;	// SZD: will be generated on demand

	/**
	 * Loads the data for the reorder
	 */
	/*private static int[][] loadReorderTable()	// SZD: table will be generated
	{
		try
		{
			Class elemType = int[][].class.getComponentType();
			Object o = JavaLayerUtils.deserializeArrayResource("l3reorder.ser", elemType, 6);
			return (int[][])o;
		}
		catch (IOException ex)
		{
			throw new ExceptionInInitializerError(ex);
		}
	}*/

	static int[] reorder(int scalefac_band[]) {	// SZD: converted from LAME
		int j = 0;
		int ix[] = new int[576];
		for(int sfb = 0; sfb < 13; sfb++) {
			int start = scalefac_band[sfb];
			int end   = scalefac_band[sfb + 1];
			for(int window = 0; window < 3; window++)
				for(int i = start; i < end; i++)
					ix[3 * i + window] = j++;
		}
		return ix;
	}

	/*static final int reorder_table_data[][]; =
	{
	 {  0,  4,  8,  1,  5,  9,  2,  6, 10,  3,  7, 11, 12, 16, 20, 13,
	   17, 21, 14, 18, 22, 15, 19, 23, 24, 28, 32, 25, 29, 33, 26, 30,
	   34, 27, 31, 35, 36, 42, 48, 37, 43, 49, 38, 44, 50, 39, 45, 51,
	   40, 46, 52, 41, 47, 53, 54, 60, 66, 55, 61, 67, 56, 62, 68, 57,
	   63, 69, 58, 64, 70, 59, 65, 71, 72, 80, 88, 73, 81, 89, 74, 82,
	   90, 75, 83, 91, 76, 84, 92, 77, 85, 93, 78, 86, 94, 79, 87, 95,
	   96,106,116, 97,107,117, 98,108,118, 99,109,119,100,110,120,101,
	  111,121,102,112,122,103,113,123,104,114,124,105,115,125,126,140,
	  154,127,141,155,128,142,156,129,143,157,130,144,158,131,145,159,
	  132,146,160,133,147,161,134,148,162,135,149,163,136,150,164,137,
	  151,165,138,152,166,139,153,167,168,186,204,169,187,205,170,188,
	  206,171,189,207,172,190,208,173,191,209,174,192,210,175,193,211,
	  176,194,212,177,195,213,178,196,214,179,197,215,180,198,216,181,
	  199,217,182,200,218,183,201,219,184,202,220,185,203,221,222,248,
	  274,223,249,275,224,250,276,225,251,277,226,252,278,227,253,279,
	  228,254,280,229,255,281,230,256,282,231,257,283,232,258,284,233,
	  259,285,234,260,286,235,261,287,236,262,288,237,263,289,238,264,
	  290,239,265,291,240,266,292,241,267,293,242,268,294,243,269,295,
	  244,270,296,245,271,297,246,272,298,247,273,299,300,332,364,301,
	  333,365,302,334,366,303,335,367,304,336,368,305,337,369,306,338,
	  370,307,339,371,308,340,372,309,341,373,310,342,374,311,343,375,
	  312,344,376,313,345,377,314,346,378,315,347,379,316,348,380,317,
	  349,381,318,350,382,319,351,383,320,352,384,321,353,385,322,354,
	  386,323,355,387,324,356,388,325,357,389,326,358,390,327,359,391,
	  328,360,392,329,361,393,330,362,394,331,363,395,396,438,480,397,
	  439,481,398,440,482,399,441,483,400,442,484,401,443,485,402,444,
	  486,403,445,487,404,446,488,405,447,489,406,448,490,407,449,491,
	  408,450,492,409,451,493,410,452,494,411,453,495,412,454,496,413,
	  455,497,414,456,498,415,457,499,416,458,500,417,459,501,418,460,
	  502,419,461,503,420,462,504,421,463,505,422,464,506,423,465,507,
	  424,466,508,425,467,509,426,468,510,427,469,511,428,470,512,429,
	  471,513,430,472,514,431,473,515,432,474,516,433,475,517,434,476,
	  518,435,477,519,436,478,520,437,479,521,522,540,558,523,541,559,
	  524,542,560,525,543,561,526,544,562,527,545,563,528,546,564,529,
	  547,565,530,548,566,531,549,567,532,550,568,533,551,569,534,552,
	  570,535,553,571,536,554,572,537,555,573,538,556,574,539,557,575},
	 {  0,  4,  8,  1,  5,  9,  2,  6, 10,  3,  7, 11, 12, 16, 20, 13,
	   17, 21, 14, 18, 22, 15, 19, 23, 24, 28, 32, 25, 29, 33, 26, 30,
	   34, 27, 31, 35, 36, 42, 48, 37, 43, 49, 38, 44, 50, 39, 45, 51,
	   40, 46, 52, 41, 47, 53, 54, 62, 70, 55, 63, 71, 56, 64, 72, 57,
	   65, 73, 58, 66, 74, 59, 67, 75, 60, 68, 76, 61, 69, 77, 78, 88,
	   98, 79, 89, 99, 80, 90,100, 81, 91,101, 82, 92,102, 83, 93,103,
	   84, 94,104, 85, 95,105, 86, 96,106, 87, 97,107,108,120,132,109,
	  121,133,110,122,134,111,123,135,112,124,136,113,125,137,114,126,
	  138,115,127,139,116,128,140,117,129,141,118,130,142,119,131,143,
	  144,158,172,145,159,173,146,160,174,147,161,175,148,162,176,149,
	  163,177,150,164,178,151,165,179,152,166,180,153,167,181,154,168,
	  182,155,169,183,156,170,184,157,171,185,186,204,222,187,205,223,
	  188,206,224,189,207,225,190,208,226,191,209,227,192,210,228,193,
	  211,229,194,212,230,195,213,231,196,214,232,197,215,233,198,216,
	  234,199,217,235,200,218,236,201,219,237,202,220,238,203,221,239,
	  240,264,288,241,265,289,242,266,290,243,267,291,244,268,292,245,
	  269,293,246,270,294,247,271,295,248,272,296,249,273,297,250,274,
	  298,251,275,299,252,276,300,253,277,301,254,278,302,255,279,303,
	  256,280,304,257,281,305,258,282,306,259,283,307,260,284,308,261,
	  285,309,262,286,310,263,287,311,312,344,376,313,345,377,314,346,
	  378,315,347,379,316,348,380,317,349,381,318,350,382,319,351,383,
	  320,352,384,321,353,385,322,354,386,323,355,387,324,356,388,325,
	  357,389,326,358,390,327,359,391,328,360,392,329,361,393,330,362,
	  394,331,363,395,332,364,396,333,365,397,334,366,398,335,367,399,
	  336,368,400,337,369,401,338,370,402,339,371,403,340,372,404,341,
	  373,405,342,374,406,343,375,407,408,452,496,409,453,497,410,454,
	  498,411,455,499,412,456,500,413,457,501,414,458,502,415,459,503,
	  416,460,504,417,461,505,418,462,506,419,463,507,420,464,508,421,
	  465,509,422,466,510,423,467,511,424,468,512,425,469,513,426,470,
	  514,427,471,515,428,472,516,429,473,517,430,474,518,431,475,519,
	  432,476,520,433,477,521,434,478,522,435,479,523,436,480,524,437,
	  481,525,438,482,526,439,483,527,440,484,528,441,485,529,442,486,
	  530,443,487,531,444,488,532,445,489,533,446,490,534,447,491,535,
	  448,492,536,449,493,537,450,494,538,451,495,539,540,552,564,541,
	  553,565,542,554,566,543,555,567,544,556,568,545,557,569,546,558,
	  570,547,559,571,548,560,572,549,561,573,550,562,574,551,563,575},
	 {  0,  4,  8,  1,  5,  9,  2,  6, 10,  3,  7, 11, 12, 16, 20, 13,
	   17, 21, 14, 18, 22, 15, 19, 23, 24, 28, 32, 25, 29, 33, 26, 30,
	   34, 27, 31, 35, 36, 42, 48, 37, 43, 49, 38, 44, 50, 39, 45, 51,
	   40, 46, 52, 41, 47, 53, 54, 62, 70, 55, 63, 71, 56, 64, 72, 57,
	   65, 73, 58, 66, 74, 59, 67, 75, 60, 68, 76, 61, 69, 77, 78, 88,
	   98, 79, 89, 99, 80, 90,100, 81, 91,101, 82, 92,102, 83, 93,103,
	   84, 94,104, 85, 95,105, 86, 96,106, 87, 97,107,108,120,132,109,
	  121,133,110,122,134,111,123,135,112,124,136,113,125,137,114,126,
	  138,115,127,139,116,128,140,117,129,141,118,130,142,119,131,143,
	  144,158,172,145,159,173,146,160,174,147,161,175,148,162,176,149,
	  163,177,150,164,178,151,165,179,152,166,180,153,167,181,154,168,
	  182,155,169,183,156,170,184,157,171,185,186,204,222,187,205,223,
	  188,206,224,189,207,225,190,208,226,191,209,227,192,210,228,193,
	  211,229,194,212,230,195,213,231,196,214,232,197,215,233,198,216,
	  234,199,217,235,200,218,236,201,219,237,202,220,238,203,221,239,
	  240,264,288,241,265,289,242,266,290,243,267,291,244,268,292,245,
	  269,293,246,270,294,247,271,295,248,272,296,249,273,297,250,274,
	  298,251,275,299,252,276,300,253,277,301,254,278,302,255,279,303,
	  256,280,304,257,281,305,258,282,306,259,283,307,260,284,308,261,
	  285,309,262,286,310,263,287,311,312,342,372,313,343,373,314,344,
	  374,315,345,375,316,346,376,317,347,377,318,348,378,319,349,379,
	  320,350,380,321,351,381,322,352,382,323,353,383,324,354,384,325,
	  355,385,326,356,386,327,357,387,328,358,388,329,359,389,330,360,
	  390,331,361,391,332,362,392,333,363,393,334,364,394,335,365,395,
	  336,366,396,337,367,397,338,368,398,339,369,399,340,370,400,341,
	  371,401,402,442,482,403,443,483,404,444,484,405,445,485,406,446,
	  486,407,447,487,408,448,488,409,449,489,410,450,490,411,451,491,
	  412,452,492,413,453,493,414,454,494,415,455,495,416,456,496,417,
	  457,497,418,458,498,419,459,499,420,460,500,421,461,501,422,462,
	  502,423,463,503,424,464,504,425,465,505,426,466,506,427,467,507,
	  428,468,508,429,469,509,430,470,510,431,471,511,432,472,512,433,
	  473,513,434,474,514,435,475,515,436,476,516,437,477,517,438,478,
	  518,439,479,519,440,480,520,441,481,521,522,540,558,523,541,559,
	  524,542,560,525,543,561,526,544,562,527,545,563,528,546,564,529,
	  547,565,530,548,566,531,549,567,532,550,568,533,551,569,534,552,
	  570,535,553,571,536,554,572,537,555,573,538,556,574,539,557,575},
	 {  0,  4,  8,  1,  5,  9,  2,  6, 10,  3,  7, 11, 12, 16, 20, 13,
	   17, 21, 14, 18, 22, 15, 19, 23, 24, 28, 32, 25, 29, 33, 26, 30,
	   34, 27, 31, 35, 36, 40, 44, 37, 41, 45, 38, 42, 46, 39, 43, 47,
	   48, 54, 60, 49, 55, 61, 50, 56, 62, 51, 57, 63, 52, 58, 64, 53,
	   59, 65, 66, 74, 82, 67, 75, 83, 68, 76, 84, 69, 77, 85, 70, 78,
	   86, 71, 79, 87, 72, 80, 88, 73, 81, 89, 90,100,110, 91,101,111,
	   92,102,112, 93,103,113, 94,104,114, 95,105,115, 96,106,116, 97,
	  107,117, 98,108,118, 99,109,119,120,132,144,121,133,145,122,134,
	  146,123,135,147,124,136,148,125,137,149,126,138,150,127,139,151,
	  128,140,152,129,141,153,130,142,154,131,143,155,156,170,184,157,
	  171,185,158,172,186,159,173,187,160,174,188,161,175,189,162,176,
	  190,163,177,191,164,178,192,165,179,193,166,180,194,167,181,195,
	  168,182,196,169,183,197,198,216,234,199,217,235,200,218,236,201,
	  219,237,202,220,238,203,221,239,204,222,240,205,223,241,206,224,
	  242,207,225,243,208,226,244,209,227,245,210,228,246,211,229,247,
	  212,230,248,213,231,249,214,232,250,215,233,251,252,274,296,253,
	  275,297,254,276,298,255,277,299,256,278,300,257,279,301,258,280,
	  302,259,281,303,260,282,304,261,283,305,262,284,306,263,285,307,
	  264,286,308,265,287,309,266,288,310,267,289,311,268,290,312,269,
	  291,313,270,292,314,271,293,315,272,294,316,273,295,317,318,348,
	  378,319,349,379,320,350,380,321,351,381,322,352,382,323,353,383,
	  324,354,384,325,355,385,326,356,386,327,357,387,328,358,388,329,
	  359,389,330,360,390,331,361,391,332,362,392,333,363,393,334,364,
	  394,335,365,395,336,366,396,337,367,397,338,368,398,339,369,399,
	  340,370,400,341,371,401,342,372,402,343,373,403,344,374,404,345,
	  375,405,346,376,406,347,377,407,408,464,520,409,465,521,410,466,
	  522,411,467,523,412,468,524,413,469,525,414,470,526,415,471,527,
	  416,472,528,417,473,529,418,474,530,419,475,531,420,476,532,421,
	  477,533,422,478,534,423,479,535,424,480,536,425,481,537,426,482,
	  538,427,483,539,428,484,540,429,485,541,430,486,542,431,487,543,
	  432,488,544,433,489,545,434,490,546,435,491,547,436,492,548,437,
	  493,549,438,494,550,439,495,551,440,496,552,441,497,553,442,498,
	  554,443,499,555,444,500,556,445,501,557,446,502,558,447,503,559,
	  448,504,560,449,505,561,450,506,562,451,507,563,452,508,564,453,
	  509,565,454,510,566,455,511,567,456,512,568,457,513,569,458,514,
	  570,459,515,571,460,516,572,461,517,573,462,518,574,463,519,575},
	 {  0,  4,  8,  1,  5,  9,  2,  6, 10,  3,  7, 11, 12, 16, 20, 13,
	   17, 21, 14, 18, 22, 15, 19, 23, 24, 28, 32, 25, 29, 33, 26, 30,
	   34, 27, 31, 35, 36, 40, 44, 37, 41, 45, 38, 42, 46, 39, 43, 47,
	   48, 54, 60, 49, 55, 61, 50, 56, 62, 51, 57, 63, 52, 58, 64, 53,
	   59, 65, 66, 72, 78, 67, 73, 79, 68, 74, 80, 69, 75, 81, 70, 76,
	   82, 71, 77, 83, 84, 94,104, 85, 95,105, 86, 96,106, 87, 97,107,
	   88, 98,108, 89, 99,109, 90,100,110, 91,101,111, 92,102,112, 93,
	  103,113,114,126,138,115,127,139,116,128,140,117,129,141,118,130,
	  142,119,131,143,120,132,144,121,133,145,122,134,146,123,135,147,
	  124,136,148,125,137,149,150,164,178,151,165,179,152,166,180,153,
	  167,181,154,168,182,155,169,183,156,170,184,157,171,185,158,172,
	  186,159,173,187,160,174,188,161,175,189,162,176,190,163,177,191,
	  192,208,224,193,209,225,194,210,226,195,211,227,196,212,228,197,
	  213,229,198,214,230,199,215,231,200,216,232,201,217,233,202,218,
	  234,203,219,235,204,220,236,205,221,237,206,222,238,207,223,239,
	  240,260,280,241,261,281,242,262,282,243,263,283,244,264,284,245,
	  265,285,246,266,286,247,267,287,248,268,288,249,269,289,250,270,
	  290,251,271,291,252,272,292,253,273,293,254,274,294,255,275,295,
	  256,276,296,257,277,297,258,278,298,259,279,299,300,326,352,301,
	  327,353,302,328,354,303,329,355,304,330,356,305,331,357,306,332,
	  358,307,333,359,308,334,360,309,335,361,310,336,362,311,337,363,
	  312,338,364,313,339,365,314,340,366,315,341,367,316,342,368,317,
	  343,369,318,344,370,319,345,371,320,346,372,321,347,373,322,348,
	  374,323,349,375,324,350,376,325,351,377,378,444,510,379,445,511,
	  380,446,512,381,447,513,382,448,514,383,449,515,384,450,516,385,
	  451,517,386,452,518,387,453,519,388,454,520,389,455,521,390,456,
	  522,391,457,523,392,458,524,393,459,525,394,460,526,395,461,527,
	  396,462,528,397,463,529,398,464,530,399,465,531,400,466,532,401,
	  467,533,402,468,534,403,469,535,404,470,536,405,471,537,406,472,
	  538,407,473,539,408,474,540,409,475,541,410,476,542,411,477,543,
	  412,478,544,413,479,545,414,480,546,415,481,547,416,482,548,417,
	  483,549,418,484,550,419,485,551,420,486,552,421,487,553,422,488,
	  554,423,489,555,424,490,556,425,491,557,426,492,558,427,493,559,
	  428,494,560,429,495,561,430,496,562,431,497,563,432,498,564,433,
	  499,565,434,500,566,435,501,567,436,502,568,437,503,569,438,504,
	  570,439,505,571,440,506,572,441,507,573,442,508,574,443,509,575},
	 {  0,  4,  8,  1,  5,  9,  2,  6, 10,  3,  7, 11, 12, 16, 20, 13,
	   17, 21, 14, 18, 22, 15, 19, 23, 24, 28, 32, 25, 29, 33, 26, 30,
	   34, 27, 31, 35, 36, 40, 44, 37, 41, 45, 38, 42, 46, 39, 43, 47,
	   48, 54, 60, 49, 55, 61, 50, 56, 62, 51, 57, 63, 52, 58, 64, 53,
	   59, 65, 66, 74, 82, 67, 75, 83, 68, 76, 84, 69, 77, 85, 70, 78,
	   86, 71, 79, 87, 72, 80, 88, 73, 81, 89, 90,102,114, 91,103,115,
	   92,104,116, 93,105,117, 94,106,118, 95,107,119, 96,108,120, 97,
	  109,121, 98,110,122, 99,111,123,100,112,124,101,113,125,126,142,
	  158,127,143,159,128,144,160,129,145,161,130,146,162,131,147,163,
	  132,148,164,133,149,165,134,150,166,135,151,167,136,152,168,137,
	  153,169,138,154,170,139,155,171,140,156,172,141,157,173,174,194,
	  214,175,195,215,176,196,216,177,197,217,178,198,218,179,199,219,
	  180,200,220,181,201,221,182,202,222,183,203,223,184,204,224,185,
	  205,225,186,206,226,187,207,227,188,208,228,189,209,229,190,210,
	  230,191,211,231,192,212,232,193,213,233,234,260,286,235,261,287,
	  236,262,288,237,263,289,238,264,290,239,265,291,240,266,292,241,
	  267,293,242,268,294,243,269,295,244,270,296,245,271,297,246,272,
	  298,247,273,299,248,274,300,249,275,301,250,276,302,251,277,303,
	  252,278,304,253,279,305,254,280,306,255,281,307,256,282,308,257,
	  283,309,258,284,310,259,285,311,312,346,380,313,347,381,314,348,
	  382,315,349,383,316,350,384,317,351,385,318,352,386,319,353,387,
	  320,354,388,321,355,389,322,356,390,323,357,391,324,358,392,325,
	  359,393,326,360,394,327,361,395,328,362,396,329,363,397,330,364,
	  398,331,365,399,332,366,400,333,367,401,334,368,402,335,369,403,
	  336,370,404,337,371,405,338,372,406,339,373,407,340,374,408,341,
	  375,409,342,376,410,343,377,411,344,378,412,345,379,413,414,456,
	  498,415,457,499,416,458,500,417,459,501,418,460,502,419,461,503,
	  420,462,504,421,463,505,422,464,506,423,465,507,424,466,508,425,
	  467,509,426,468,510,427,469,511,428,470,512,429,471,513,430,472,
	  514,431,473,515,432,474,516,433,475,517,434,476,518,435,477,519,
	  436,478,520,437,479,521,438,480,522,439,481,523,440,482,524,441,
	  483,525,442,484,526,443,485,527,444,486,528,445,487,529,446,488,
	  530,447,489,531,448,490,532,449,491,533,450,492,534,451,493,535,
	  452,494,536,453,495,537,454,496,538,455,497,539,540,552,564,541,
	  553,565,542,554,566,543,555,567,544,556,568,545,557,569,546,558,
	  570,547,559,571,548,560,572,549,561,573,550,562,574,551,563,575}
	};
*/

	private static final float cs[] =
	{
	 0.857492925712f, 0.881741997318f, 0.949628649103f, 0.983314592492f,
	 0.995517816065f, 0.999160558175f, 0.999899195243f, 0.999993155067f
	};

	private static final float ca[] =
	{
	 -0.5144957554270f, -0.4717319685650f, -0.3133774542040f, -0.1819131996110f,
	 -0.0945741925262f, -0.0409655828852f, -0.0141985685725f, -0.00369997467375f
	};

    /************************************************************/
 	/*                       END OF L3TABLE                     */
	/************************************************************/

    /************************************************************/
	/*                            L3TYPE                        */
	/************************************************************/


	/***************************************************************/
	/*                          END OF L3TYPE                      */
	/***************************************************************/

	/***************************************************************/
	/*                             INV_MDCT                        */
	/***************************************************************/
	public static final float win[][] =
	{
	 { -1.6141214951E-02f, -5.3603178919E-02f, -1.0070713296E-01f, -1.6280817573E-01f,
	   -4.9999999679E-01f, -3.8388735032E-01f, -6.2061144372E-01f, -1.1659756083E+00f,
	   -3.8720752656E+00f, -4.2256286556E+00f, -1.5195289984E+00f, -9.7416483388E-01f,
	   -7.3744074053E-01f, -1.2071067773E+00f, -5.1636156596E-01f, -4.5426052317E-01f,
	   -4.0715656898E-01f, -3.6969460527E-01f, -3.3876269197E-01f, -3.1242222492E-01f,
	   -2.8939587111E-01f, -2.6880081906E-01f, -5.0000000266E-01f, -2.3251417468E-01f,
	   -2.1596714708E-01f, -2.0004979098E-01f, -1.8449493497E-01f, -1.6905846094E-01f,
	   -1.5350360518E-01f, -1.3758624925E-01f, -1.2103922149E-01f, -2.0710679058E-01f,
	   -8.4752577594E-02f, -6.4157525656E-02f, -4.1131172614E-02f, -1.4790705759E-02f },

	 { -1.6141214951E-02f, -5.3603178919E-02f, -1.0070713296E-01f, -1.6280817573E-01f,
	   -4.9999999679E-01f, -3.8388735032E-01f, -6.2061144372E-01f, -1.1659756083E+00f,
	   -3.8720752656E+00f, -4.2256286556E+00f, -1.5195289984E+00f, -9.7416483388E-01f,
	   -7.3744074053E-01f, -1.2071067773E+00f, -5.1636156596E-01f, -4.5426052317E-01f,
	   -4.0715656898E-01f, -3.6969460527E-01f, -3.3908542600E-01f, -3.1511810350E-01f,
	   -2.9642226150E-01f, -2.8184548650E-01f, -5.4119610000E-01f, -2.6213228100E-01f,
	   -2.5387916537E-01f, -2.3296291359E-01f, -1.9852728987E-01f, -1.5233534808E-01f,
	   -9.6496400054E-02f, -3.3423828516E-02f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f },

	 { -4.8300800645E-02f, -1.5715656932E-01f, -2.8325045177E-01f, -4.2953747763E-01f,
	   -1.2071067795E+00f, -8.2426483178E-01f, -1.1451749106E+00f, -1.7695290101E+00f,
	   -4.5470225061E+00f, -3.4890531002E+00f, -7.3296292804E-01f, -1.5076514758E-01f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f },

	 { 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f, 0.0000000000E+00f,
	   0.0000000000E+00f, 0.0000000000E+00f, -1.5076513660E-01f, -7.3296291107E-01f,
	   -3.4890530566E+00f, -4.5470224727E+00f, -1.7695290031E+00f, -1.1451749092E+00f,
	   -8.3137738100E-01f, -1.3065629650E+00f, -5.4142014250E-01f, -4.6528974900E-01f,
	   -4.1066990750E-01f, -3.7004680800E-01f, -3.3876269197E-01f, -3.1242222492E-01f,
	   -2.8939587111E-01f, -2.6880081906E-01f, -5.0000000266E-01f, -2.3251417468E-01f,
	   -2.1596714708E-01f, -2.0004979098E-01f, -1.8449493497E-01f, -1.6905846094E-01f,
	   -1.5350360518E-01f, -1.3758624925E-01f, -1.2103922149E-01f, -2.0710679058E-01f,
	   -8.4752577594E-02f, -6.4157525656E-02f, -4.1131172614E-02f, -1.4790705759E-02f }
	};
	/***************************************************************/
	/*                         END OF INV_MDCT                     */
	/***************************************************************/

	class Sftable
	{
		public int[]	 l;
		public int[]	 s;

		public Sftable()
		{
			l = new int[5];
			s = new int[3];
		}

		public Sftable(int[] thel, int[] thes)
		{
			l = thel;
			s = thes;
		}
	}

	public Sftable				sftable;

	public static final int 	nr_of_sfb_block[][][] =
	{{{ 6, 5, 5, 5} , { 9, 9, 9, 9} , { 6, 9, 9, 9}},
    {{ 6, 5, 7, 3} , { 9, 9,12, 6} , { 6, 9,12, 6}},
    {{11,10, 0, 0} , {18,18, 0, 0} , {15,18, 0, 0}},
    {{ 7, 7, 7, 0} , {12,12,12, 0} , { 6,15,12, 0}},
    {{ 6, 6, 6, 3} , {12, 9, 9, 6} , { 6,12, 9, 6}},
    {{ 8, 8, 5, 0} , {15,12, 9, 0} , { 6,18, 9, 0}}};


}
