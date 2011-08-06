/*
 * 11/19/04		1.0 moved to LGPL. 
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

import java.io.IOException;

/**
 * Work in progress.
 * 
 * Class to describe a seekable data source. 
 *  
 */
public interface Source
{
	
	public static final long	LENGTH_UNKNOWN = -1;
	
	public int read(byte[] b, int offs, int len)
		throws IOException;
	
	
	public boolean	willReadBlock();
			
	public boolean	isSeekable();
		
	public long		length();
	
	public long		tell();
	
	public long		seek(long pos);
	
}
