import java.util.concurrent.atomic.AtomicInteger;

import ij.ImageStack;

/**
 **
 ** ImageDtt - Process images with DTT-based methods
 **
 ** Copyright (C) 2016 Elphel, Inc.
 **
 ** -----------------------------------------------------------------------------**
 **  
 **  ImageDtt.java is free software: you can redistribute it and/or modify
 **  it under the terms of the GNU General Public License as published by
 **  the Free Software Foundation, either version 3 of the License, or
 **  (at your option) any later version.
 **
 **  This program is distributed in the hope that it will be useful,
 **  but WITHOUT ANY WARRANTY; without even the implied warranty of
 **  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **  GNU General Public License for more details.
 **
 **  You should have received a copy of the GNU General Public License
 **  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ** -----------------------------------------------------------------------------**
 **
 */

public class ImageDtt {
	public ImageDtt(){

	}

	public double [][] mdctStack(
			final ImageStack                                 imageStack,
			final EyesisCorrectionParameters.DCTParameters   dctParameters, //
			final int                                        threadsMax, // maximal step in pixels on the maxRadius for 1 angular step (i.e. 0.5)
			final int                                        debugLevel,
			final boolean                                    updateStatus) // update status info

	{
	  	  if (imageStack==null) return null;
		  final int imgWidth=imageStack.getWidth();
		  final int nChn=imageStack.getSize();
		  double [][] dct_data = new double [nChn][];
		  float [] fpixels;
		  int i,chn; //tileX,tileY;
		  /* find number of the green channel - should be called "green", if none - use last */
		  // Extract float pixels from inage stack, convert each to double

		  for (chn=0;chn<nChn;chn++) {
			  fpixels= (float[]) imageStack.getPixels(chn+1);
			  double[] dpixels = new double[fpixels.length];
			  for (i = 0; i <fpixels.length;i++) dpixels[i] = fpixels[i];
			  // convert each to DCT tiles
			  dct_data[chn] =lapped_dct(
						dpixels,
						imgWidth,
						dctParameters.dct_size,
						0, //     dct_mode,    // 0: dct/dct, 1: dct/dst, 2: dst/dct, 3: dst/dst
						dctParameters.dct_window, // final int       window_type,
						threadsMax,  // maximal number of threads to launch                         
						debugLevel);
		  }
		return dct_data;
	}

	public double [] lapped_dct(
			final double [] dpixels,
			final int       width,
			final int       dct_size,
			final int       dct_mode,    // 0: dct/dct, 1: dct/dst, 2: dst/dct, 3: dst/dst
			final int       window_type,
			final int       threadsMax,  // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
		final int height=dpixels.length/width;
		final int tilesX=width/dct_size-1;
		final int tilesY=height/dct_size-1;
		final int nTiles=tilesX*tilesY; 
		final double [] dct_data = new double[tilesY*tilesX*dct_size*dct_size];
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);
		for (int i=0; i<dct_data.length;i++) dct_data[i]= 0;

		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				public void run() {
					DttRad2 dtt = new DttRad2(dct_size);
					dtt.set_window(window_type);
					double [] tile_in = new double[4*dct_size * dct_size];
					double [] tile_folded;
					double [] tile_out; // = new double[dct_size * dct_size];
					int tileY,tileX;
					int n2 = dct_size * 2;
					for (int nTile = ai.getAndIncrement(); nTile < nTiles; nTile = ai.getAndIncrement()) {
						tileY = nTile/tilesX;
						tileX = nTile - tileY * tilesX;
						for (int i = 0; i < n2;i++){
							System.arraycopy(dpixels, (tileY*width+tileX)*dct_size + i*width, tile_in, i*n2, n2);
						}
//						tile_out=dtt.mdct_2d(tile_in, dct_mode, dct_size);
						tile_folded=dtt.fold_tile(tile_in, dct_size);
						tile_out=dtt.dttt_iv  (tile_folded, dct_mode, dct_size);
						for (int i = 0; i < dct_size;i++){
							System.arraycopy(tile_out, dct_size* i, dct_data, ((tileY*dct_size + i) *tilesX + tileX)*dct_size , dct_size);
						}
					}
				}
			};
		}		      
		startAndJoin(threads);
		return dct_data;
	}
	
	
	
	
	
	public double [] lapped_idct(
			final double [] dct_data,  // scanline representation of dcd data, organized as dct_size x dct_size tiles  
			final int       dct_width,
			final int       dct_size,
			final int       window_type,
			final int       threadsMax,  // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
		final int tilesX=dct_width/dct_size;
		final int tilesY=dct_data.length/(dct_width*dct_size);
		final int width=  (tilesX+1)*dct_size;
		final int height= (tilesY+1)*dct_size;
		if (globalDebugLevel > 0) {
			System.out.println("lapped_idct():dct_width="+dct_width);
			System.out.println("lapped_idct():tilesX=   "+tilesX);
			System.out.println("lapped_idct():tilesY=   "+tilesY);
			System.out.println("lapped_idct():width=    "+width);
			System.out.println("lapped_idct():height=   "+height);
			double debug0 = 0.0;
			for (int i=0;i<dct_data.length;i++){
				debug0 +=dct_data[i]*dct_data[i];
			}
			debug0 = Math.sqrt(debug0)/dct_data.length;
			System.out.println("lapped_idct():debug0=   "+debug0+" (dct_data.length= "+dct_data.length+")");
		}
		
		final double [] dpixels = new double[width*height];
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger nser = new AtomicInteger(0);
		final int [][][] tiles_list = new int[4][][];
		for (int n=0; n<4; n++){
			int nx = (tilesX + 1 - (n &1)) / 2;
			int ny = (tilesY + 1 - ((n>>1) & 1)) / 2;
			tiles_list[n] = new int [nx*ny][2];
			int indx = 0;
			for (int i = 0;i < ny; i++) for (int j = 0; j < nx; j++){
				tiles_list[n][indx][0]=2*j+(n &1);
				tiles_list[n][indx++][1]=2*i+((n>>1) & 1);
			}
		}

		for (int i=0; i<dpixels.length;i++) dpixels[i]= 0;
		for (int n=0; n<4; n++){
			nser.set(n);
			ai.set(0);
			for (int ithread = 0; ithread < threads.length; ithread++) {
				threads[ithread] = new Thread() {
					public void run() {
						DttRad2 dtt = new DttRad2(dct_size);
						dtt.set_window(window_type);
						double [] tile_in = new double[dct_size * dct_size];
						double [] tile_dct; // = new double[dct_size * dct_size];
						double [] tile_out; //  = new double[4*dct_size * dct_size];
						int tileY,tileX;
						int n2 = dct_size * 2;
						for (int nTile = ai.getAndIncrement(); nTile < tiles_list[nser.get()].length; nTile = ai.getAndIncrement()) {
							tileX = tiles_list[nser.get()][nTile][0];
							tileY = tiles_list[nser.get()][nTile][1];
							for (int i = 0; i < dct_size;i++){
								System.arraycopy(dct_data, (tileY*dct_width+tileX)*dct_size + i*dct_width, tile_in, dct_size*i, dct_size);
							}
							tile_dct=dtt.dttt_iv  (tile_in, 0, dct_size);
							tile_out=dtt.unfold_tile(tile_dct, dct_size);
							for (int i = 0; i < n2;i++){
								int start_line = ((tileY*dct_size + i) *(tilesX+1) + tileX)*dct_size; 
								for (int j = 0; j<n2;j++) {
									dpixels[start_line + j] += tile_out[n2 * i + j]; //  +1.0; 

								}
							}

						}
					}
				};
			}		      
			startAndJoin(threads);
		}
		return dpixels;
	}
	
	public double [][][][] mdctDcStack(
			final ImageStack                                 imageStack,
			final EyesisCorrectionParameters.DCTParameters   dctParameters, //
			final EyesisDCT                                  eyesisDCT,
			final int                                        threadsMax, // maximal step in pixels on the maxRadius for 1 angular step (i.e. 0.5)
			final int                                        debugLevel,
			final boolean                                    updateStatus) // update status info

	{
	  	  if (imageStack==null) return null;
		  final int imgWidth=imageStack.getWidth();
		  final int nChn=imageStack.getSize();
		  double [][][][] dctdc_data = new double [nChn][][][];
		  float [] fpixels;
		  int i,chn; //tileX,tileY;
		  /* find number of the green channel - should be called "green", if none - use last */
		  // Extract float pixels from inage stack, convert each to double

		  EyesisDCT.DCTKernels dct_kernels = null;
		  if (dctParameters.kernel_chn >=0 ){
			  dct_kernels = eyesisDCT.kernels[dctParameters.kernel_chn];
		  }
		  
		  for (chn=0;chn<nChn;chn++) {
			  fpixels= (float[]) imageStack.getPixels(chn+1);
			  double[] dpixels = new double[fpixels.length];
			  for (i = 0; i <fpixels.length;i++) dpixels[i] = fpixels[i];
			  // convert each to DCT tiles
			  dctdc_data[chn] =lapped_dctdc(
						dpixels,
						imgWidth,
						dctParameters.dct_size,
						dctParameters.subtract_dc,
						0, //     dct_mode,    // 0: dct/dct, 1: dct/dst, 2: dst/dct, 3: dst/dst
						dctParameters.dct_window, // final int       window_type,
						chn,
						dct_kernels,
						dctParameters.skip_sym,
						dctParameters.convolve_direct,
						dctParameters.tileX,
						dctParameters.tileY,
						dctParameters.dbg_mode,
						threadsMax,  // maximal number of threads to launch                         
						debugLevel);
		  }
		return dctdc_data;
	}
	
	// extract DC, result is an array [tilesY][tilesX][dct_size*dct_size+1] - last element is DC value
	public double [][][] lapped_dctdc(
			final double [] dpixels,
			final int       width,
			final int       dct_size,
			final boolean   subtract_dc,
			final int       dct_mode,    // 0: dct/dct, 1: dct/dst, 2: dst/dct, 3: dst/dst
			final int       window_type,
			final int       color,
			final EyesisDCT.DCTKernels dct_kernels,
			final boolean   skip_sym,
			final boolean   convolve_direct, // test feature - convolve directly with the symmetrical kernel
			final int       debug_tileX,
			final int       debug_tileY,
			final int       debug_mode,
			final int       threadsMax,  // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
		final int kernel_margin = 1; //move to parameters?
		final int height=dpixels.length/width;
		final int tilesX=width/dct_size-1;
		final int tilesY=height/dct_size-1;
		final int nTiles=tilesX*tilesY; 
		final double [][][] dctdc_data = new double[tilesY][tilesX][dct_size*dct_size+1];
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);
		for (int tileY = 0; tileY < tilesY; tileY++){
			for (int tileX = 0; tileX < tilesX; tileX++){
				for (int i=0; i<dctdc_data[tileY][tileX].length;i++) dctdc_data[tileY][tileX][i]= 0.0; // actually not needed, Java initializes arrays
			}
		}
		double [] dc = new double [dct_size*dct_size];
		for (int i = 0; i<dc.length; i++) dc[i] = 1.0;
		DttRad2 dtt0 = new DttRad2(dct_size);
		dtt0.set_window(window_type);
		final double [] dciii = dtt0.dttt_iii  (dc, dct_size);
		final double [] dciiie = dtt0.dttt_iiie  (dc, dct_size);
		if ((globalDebugLevel > 0) && (color ==2)) {
			double [][]dcx = {dc,dciii,dciiie, dtt0.dttt_ii(dc, dct_size),dtt0.dttt_iie(dc, dct_size)}; 
			showDoubleFloatArrays sdfa_instance0 = new showDoubleFloatArrays(); // just for debugging?
			sdfa_instance0.showArrays(dcx,  dct_size, dct_size, true, "dcx");
		}

		
		if (globalDebugLevel > 0) {
			System.out.println("lapped_dctdc(): width="+width+" height="+height);
		}

		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				public void run() {
					DttRad2 dtt = new DttRad2(dct_size);
					dtt.set_window(window_type);
					double [] tile_in = new double[4*dct_size * dct_size];
					double [] sym_conv= null;
					if ((dct_kernels != null) && convolve_direct){ // debug feature - directly convolve with symmetrical kernel
						sym_conv = new double[4*dct_size * dct_size];
					}
					double [] tile_folded;
					double [] tile_out; // = new double[dct_size * dct_size];
					int tileY,tileX;
					int n2 = dct_size * 2;
					double dc;
					double [] tile_out_copy = null;
					showDoubleFloatArrays sdfa_instance = new showDoubleFloatArrays(); // just for debugging?
					
					for (int nTile = ai.getAndIncrement(); nTile < nTiles; nTile = ai.getAndIncrement()) {
						tileY = nTile/tilesX;
						tileX = nTile - tileY * tilesX;
						int kernelTileY=0;
						int kernelTileX=0;
						//readDCTKernels() debugLevel = 1 kernels[0].size = 8 kernels[0].img_step = 16 kernels[0].asym_nonzero = 4 nColors = 3 numVert = 123 numHor =  164
						if (dct_kernels != null){ // convolve directly with asym_kernel
							int asym_center = dct_kernels.asym_size/2; // 7 for 15
							kernelTileY = kernel_margin + (tileY * dct_size) / dct_kernels.img_step;
							kernelTileX = kernel_margin + (tileX * dct_size) / dct_kernels.img_step;
							if ((globalDebugLevel > 0) && (tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2)) {
								System.out.println("kernelTileY="+kernelTileY+" kernelTileX="+kernelTileX+" width="+width);
							}
							for (int i = 0; i < n2; i++){
								for (int j = 0; j < n2; j++){
									tile_in[i*n2 + j] = 0.0;
									// convolve list
									int [] asym_indx =   dct_kernels.asym_indx[color][kernelTileY][kernelTileX];
									double [] asym_val = dct_kernels.asym_val[color][kernelTileY][kernelTileX];
									for (int indx = 0; indx < asym_indx.length; indx++){
										int xy = asym_indx[indx];
										if ((globalDebugLevel > 0) && (tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2)) {
											System.out.println("i="+i+" j="+j+" indx="+indx+" xy="+xy);
										}
										if (xy >= 0) {
											int dy = (xy / dct_kernels.asym_size) - asym_center;
											int dx = (xy % dct_kernels.asym_size) - asym_center;
											int y = tileY*dct_size - dy + i;
											int x = tileX*dct_size - dx + j;
											if (y < 0) y &= 1; 
											if (x < 0) x &= 1;
											if (y >= height) y = (height - 2) + (y & 1);
											if (x >= width)  x = (width - 2) +  (x & 1);
											tile_in[i*n2 + j] += asym_val[indx] * dpixels[ y * width + x]; 
											if ((globalDebugLevel > 0) && (tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2)) {
												System.out.println("dy= "+dy+" dx="+dx+" x = "+x+" y="+y+" y*width + x="+(y*width + x));
												System.out.println("asym_val["+indx+"]="+asym_val[indx]+
														"  dpixels["+(y * width + x)+"]="+ dpixels[ y * width + x]+
														"tile_in["+(i*n2 + j)+"]="+tile_in[i*n2 + j]);
											}
										}
									}
								}
							}
							// directly convolve with symmetrical kernel (debug feature
							if ((dct_kernels != null) && convolve_direct){
								double [] dir_sym = dct_kernels.st_direct[color][kernelTileY][kernelTileX];
								double s0 = 0;
								for (int i = 0; i < n2; i++){
									for (int j = 0; j < n2; j++){
										int indx = i*n2+j;
										sym_conv[indx] = 0.0; // dir_sym[0]* tile_in[indx];
										for (int dy = -dct_size +1; dy < dct_size; dy++){
											int ady = (dy>=0)?dy:-dy;
											int sgny = 1;
											int y = i - dy;
											if (y < 0){
												y = -1 -y;
												sgny = -sgny;
											}
											if (y >= n2){
												y = 2*n2 - y -1;
												sgny = -sgny;
											}
											for (int dx = -dct_size +1; dx < dct_size; dx++){
												int adx = (dx >= 0)? dx:-dx;
												int sgn = sgny;
												int x = j - dx;
												if (x < 0){
													x = -1 -x;
													sgn = -sgn;
												}
												if (x >= n2){
													x = 2*n2 - x -1;
													sgn = -sgn;
												}
												sym_conv[indx] += sgn*dir_sym[ady * dct_size + adx] * tile_in[y * n2 + x];
												s0+=dir_sym[ady * dct_size + adx];
												if ((globalDebugLevel > 0) && (tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2) &&
														(i == dct_size) && (j== dct_size)) {
													System.out.println("i="+i+" j="+j+" dy="+dy+" dx="+dx+" ady="+ady+" adx="+adx+
															" y="+y+" x="+x+" sgny="+sgny+" sgn="+sgn+
															"sym_conv["+indx+"] += "+sgn+"* dir_sym["+(ady * dct_size + adx)+"] * tile_in["+(y * n2 + x)+"] +="+
															sgn+"* "+ dir_sym[ady * dct_size + adx]+" * "+tile_in[y * n2 + x]+" +="+
															(sgn*dir_sym[ady * dct_size + adx] * tile_in[y * n2 + x])+" ="+sym_conv[indx]);
												}

											}
										}
									}
								}


								if ((globalDebugLevel > 0) && (tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2)) {
									//								if ((tileY == debug_tileY) && (tileX == debug_tileX)) {
									double [][] pair = {tile_in, sym_conv};
									sdfa_instance.showArrays(pair,  n2, n2, true, "dconv-X"+tileX+"Y"+tileY+"C"+color);
									sdfa_instance.showArrays(dir_sym,  dct_size, dct_size, "dk-X"+tileX+"Y"+tileY+"C"+color);
									double s1=0,s2=0;
									for (int i = 0; i<tile_in.length; i++){
										s1 +=tile_in[i];
										s2 +=sym_conv[i];
									}
									double s3 = 0.0;
									for (int i=0; i<dct_size;i++){
										for (int j=0; j<dct_size;j++){
											double d = dir_sym[i*dct_size+j];
											if (i > 0) d*=2;
											if (j > 0) d*=2;
											s3+=d;
										}
									}
									System.out.println("s1="+s1+" s2="+s2+" s1/s2="+(s1/s2)+" s0="+s0+" s3="+s3);
								}								
//								tile_in = sym_conv.clone(); 
								System.arraycopy(sym_conv, 0, tile_in, 0, n2*n2);
							}
						} else { // no aberration correction, just copy data
							for (int i = 0; i < n2;i++){
								System.arraycopy(dpixels, (tileY*width+tileX)*dct_size + i*width, tile_in, i*n2, n2);
							}
						}
						tile_folded=dtt.fold_tile(tile_in, dct_size);
						dc = 0.0;
						if (subtract_dc) {
							for (int i = 0; i < tile_folded.length; i++) dc+=tile_folded[i];
							dc /= tile_folded.length;
							for (int i = 0; i < tile_folded.length; i++) tile_folded[i] -= dc;
						}
						tile_out=dtt.dttt_iv  (tile_folded, dct_mode, dct_size);
						 
						
						if ((dct_kernels != null) && !skip_sym){ // convolve in frequency domain with sym_kernel
							double s0 =0;

							if (debug_mode == 2){
								for (int i=0;i<dct_kernels.st_kernels[color][kernelTileY][kernelTileX].length; i++){
									s0+=dct_kernels.st_kernels[color][kernelTileY][kernelTileX][i];
								}
								s0 = dct_size*dct_size/s0;
							} else if (debug_mode == 3){
								for (int i=0;i<dct_size;i++){
									double scale0 = (i>0)?2.0:1.0; 
									for (int j=0;j<dct_size;j++){
										double scale = scale0*((j>0)?2.0:1.0);
										int indx = i*dct_size+j;
										s0+=scale*dct_kernels.st_kernels[color][kernelTileY][kernelTileX][indx];
									}
								}
								s0 = (2*dct_size-1)*(2*dct_size-1)/s0;
							}else if (debug_mode == 4){
								//dciii								
								for (int i=0;i<dct_kernels.st_kernels[color][kernelTileY][kernelTileX].length; i++){
									s0+=dciii[i]* dct_kernels.st_kernels[color][kernelTileY][kernelTileX][i];
								}
								s0 = dct_size*dct_size/s0;
							} else s0 = 1.0;
							
							for (int i = 0; i < tile_out.length; i++){
								tile_out[i] *= s0;
							}
						}
						
						if ((tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2)) {
							tile_out_copy = tile_out.clone();
						}
						
						
						if ((dct_kernels != null) && !skip_sym){ // convolve in frequency domain with sym_kernel
							for (int i = 0; i < tile_out.length; i++){
								tile_out[i] *=dct_kernels.st_kernels[color][kernelTileY][kernelTileX][i];
							}
						}						

						
						if ((dct_kernels!=null) && (tileY == debug_tileY) && (tileX == debug_tileX) && (color == 2)) {
							double [][] dbg_tile = {
									dct_kernels.st_direct[color][kernelTileY][kernelTileX],
									dct_kernels.st_kernels[color][kernelTileY][kernelTileX],
									tile_out_copy,
									tile_out};
							if (globalDebugLevel > 0){
								sdfa_instance.showArrays(tile_in,  n2, n2, "tile_in-X"+tileX+"Y"+tileY+"C"+color);
								sdfa_instance.showArrays(dbg_tile,  dct_size, dct_size, true, "dbg-X"+tileX+"Y"+tileY+"C"+color);
								System.out.println("tileY="+tileY+" tileX="+tileX+" kernelTileY="+kernelTileY+" kernelTileX="+kernelTileX);
								double s0=0.0, s1=0.0, s2=0.0, s3=0.0;
								for (int i=0;i<dct_size;i++){
									double scale0 = (i>0)?2.0:1.0; 
									for (int j=0;j<dct_size;j++){
										double scale = scale0*((j>0)?2.0:1.0);
										int indx = i*dct_size+j;
										s0+=scale*dct_kernels.st_direct[color][kernelTileY][kernelTileX][indx];
										s1+=scale*dct_kernels.st_kernels[color][kernelTileY][kernelTileX][indx];
										s2+=      dct_kernels.st_kernels[color][kernelTileY][kernelTileX][indx];
										s3+=dciii[indx]*dct_kernels.st_kernels[color][kernelTileY][kernelTileX][indx];
									}
								}
								System.out.println("s0="+s0+" s1="+s1+" s2="+s2+" s3="+s3);
							}
						}
						System.arraycopy(tile_out, 0, dctdc_data[tileY][tileX], 0, tile_out.length);
						dctdc_data[tileY][tileX][tile_out.length] = dc;
					}
				}
			};
		}		      
		startAndJoin(threads);
		return dctdc_data;
	}
	
	// extract DC or AC components in linescan order (for visualization)
	public double [] lapped_dct_dcac(
			final boolean       out_ac, // false - output DC, true - output AC
			final double [][][] dctdc_data,
			final int       threadsMax,     // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
		final int tilesY=dctdc_data.length;
		final int tilesX=dctdc_data[0].length;
		final int nTiles=tilesX*tilesY;
		final int dct_size = (int) Math.round(Math.sqrt(dctdc_data[0][0].length-1));
		final int dct_len = dct_size*dct_size;
		final double [] dct_data = new double[tilesY*tilesX*(out_ac?dct_len:1)];
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);
		for (int i=0; i<dct_data.length;i++) dct_data[i]= 0;

		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				public void run() {
					int tileY,tileX;
					for (int nTile = ai.getAndIncrement(); nTile < nTiles; nTile = ai.getAndIncrement()) {
						tileY = nTile/tilesX;
						tileX = nTile - tileY * tilesX;
						if (out_ac) {
							for (int i = 0; i < dct_size;i++){
								System.arraycopy(dctdc_data[tileY][tileX], dct_size* i, dct_data, ((tileY*dct_size + i) *tilesX + tileX)*dct_size , dct_size);
							}
						} else {
							dct_data[tileY *tilesX + tileX] =  dctdc_data[tileY][tileX][dct_len];
						}
					}
				}
			};
		}		      
		startAndJoin(threads);
		return dct_data;
	}
	
	public void dct_lpf(
			final double sigma,
			final double [][][] dctdc_data,
			final int       threadsMax,     // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
		final int tilesY=dctdc_data.length;
		final int tilesX=dctdc_data[0].length;
		final int nTiles=tilesX*tilesY;
		final int dct_size = (int) Math.round(Math.sqrt(dctdc_data[0][0].length-1));
		final int dct_len = dct_size*dct_size;
		final double [] filter_direct= new double[dct_len];
		if (sigma == 0) {
			filter_direct[0] = 1.0; 
			for (int i= 1; i<filter_direct.length;i++) filter_direct[i] =0; 
		} else {
			for (int i = 0; i < dct_size; i++){
				for (int j = 0; j < dct_size; j++){
					filter_direct[i*dct_size+j] = Math.exp(-(i*i+j*j)/(2*sigma));
				}
			}
		}
		// normalize
		double sum = 0;
		for (int i = 0; i < dct_size; i++){
			for (int j = 0; j < dct_size; j++){
				double d = 	filter_direct[i*dct_size+j];
				d*=Math.cos(Math.PI*i/(2*dct_size))*Math.cos(Math.PI*j/(2*dct_size));
				if (i > 0) d*= 2.0;
				if (j > 0) d*= 2.0;
				sum +=d;
			}
		}
		for (int i = 0; i<filter_direct.length; i++){
			filter_direct[i] /= sum;
		}
		
		if (globalDebugLevel > -1) {
			for (int i=0; i<filter_direct.length;i++){
				System.out.println("dct_lpf_psf() "+i+": "+filter_direct[i]); 
			}
		}
		DttRad2 dtt = new DttRad2(dct_size);
//		final double [] filter= dtt.dttt_iii(filter_direct);
		final double [] filter= dtt.dttt_iiie(filter_direct);
		final double [] dbg_filter= dtt.dttt_ii(filter);
		
		for (int i=0; i < filter.length;i++) filter[i] *= dct_size;  
		
		if (globalDebugLevel > -1) {
			for (int i=0; i<filter.length;i++){
				System.out.println("dct_lpf_psf() "+i+": "+filter[i]); 
			}
			showDoubleFloatArrays sdfa_instance = new showDoubleFloatArrays(); // just for debugging?
			double [][] ff = {filter_direct,filter,dbg_filter};
			sdfa_instance.showArrays(ff,  dct_size,dct_size, true, "filter_lpf");
		}
		
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);

		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				public void run() {
					int tileY,tileX;
					for (int nTile = ai.getAndIncrement(); nTile < nTiles; nTile = ai.getAndIncrement()) {
						tileY = nTile/tilesX;
						tileX = nTile - tileY * tilesX;
						for (int i = 0; i < filter.length; i++){
							dctdc_data[tileY][tileX][i] *= filter[i];
						}
					}
				}
			};
		}		      
		startAndJoin(threads);
	}
	
	public double [][][][] dct_color_convert(
			final double [][][][] dctdc_data,
			final double kr,
			final double kb,
			final double sigma_rb,        // blur of channels 0,1 (r,b) in addition to 2 (g)
			final double sigma_y,         // blur of Y from G
			final double sigma_color,     // blur of Pr, Pb in addition to Y
			final int       threadsMax,     // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
		final int tilesY=dctdc_data[0].length;
		final int tilesX=dctdc_data[0][0].length;
		final int nTiles=tilesX*tilesY;
		final int dct_size = (int) Math.round(Math.sqrt(dctdc_data[0][0][0].length-1));
		final int dct_len = dct_size*dct_size;
		final double [][][][] yPrPb = new double [3][tilesY][tilesX][dct_len];
		final double [][][] filters = new double [3][3][dct_len];
		final double kg = 1.0 - kr - kb;
		final double [][] filters_proto_direct = new double[3][dct_len];
		final double [][] filters_proto = new double[3][];
		System.out.println("dct_color_convert(): kr="+kr+" kg="+kg+" kb="+kb);
		final double [] sigmas = {sigma_rb,sigma_y,sigma_color};
		double [] norm_sym_weights = new double [dct_size*dct_size];
		for (int i = 0; i < dct_size; i++){
			for (int j = 0; j < dct_size; j++){
				double d = 	Math.cos(Math.PI*i/(2*dct_size))*Math.cos(Math.PI*j/(2*dct_size));
				if (i > 0) d*= 2.0;
				if (j > 0) d*= 2.0;
				norm_sym_weights[i*dct_size+j] = d;
			}
		}
		
		for (int n = 0; n<3; n++) {
			
			double s = 0.0;
			for (int i = 0; i < dct_size; i++){
				for (int j = 0; j < dct_size; j++){
					double d;
					if (sigmas[n] == 0.0)   d = ((i == 0) && (j==0))? 1.0:0.0;
					else                    d = Math.exp(-(i*i+j*j)/(2*sigmas[n]));
					filters_proto_direct[n][i*dct_size+j] = d;
//					if (i > 0) d*=2;
//					if (j > 0) d*=2;
//					s += d;
				}
				
			}
			for (int i = 0; i< dct_len; i++){
				s += norm_sym_weights[i]*filters_proto_direct[n][i];
			}
			
			System.out.println("dct_color_convert(): sigmas["+n+"]="+sigmas[n]+", sum="+s);
			for (int i = 0; i < dct_len; i++){
				filters_proto_direct[n][i] /=s;
			}
		}
		
		DttRad2 dtt = new DttRad2(dct_size);
		for (int i = 0; i < filters_proto.length; i++){
			filters_proto[i] = dtt.dttt_iiie(filters_proto_direct[i]);
			System.out.println("filters_proto.length="+filters_proto.length+" filters_proto["+i+"].length="+filters_proto[i].length+" dct_len="+dct_len+" dct_size="+dct_size);
			for (int j=0; j < dct_len; j++) filters_proto[i][j] *= 2*dct_size;  

		}
		if (globalDebugLevel > 0) {
			showDoubleFloatArrays sdfa_instance = new showDoubleFloatArrays(); // just for debugging?
			double [][] ff = {filters_proto_direct[0],filters_proto_direct[1],filters_proto_direct[2],filters_proto[0],filters_proto[1],filters_proto[2]};
			sdfa_instance.showArrays(ff,  dct_size,dct_size, true, "filters_proto");
		}

		double [][] coeff_arr ={
				{ kr,              kb,               kg             },  // Y = R*Kr+G*Hg+B*Kb
				{ 0.5,            -kb/(2.0*(1-kr)), -kg/(2.0*(1-kr))},  // Pr =  R* 0.5  - G* Kg/(2.0*(1-Kr)) - B *Kb/(2.0*(1-Kr))
				{-kr/(2.0*(1-kb)), 0.5,             -kg/(2.0*(1-kb))}}; // Pb =  B* 0.5  - G* Kg/(2.0*(1-Kb)) - R *Kr/(2.0*(1-Kb))
		for (int k = 0; k < dct_len; k++){
			for (int i = 0; i < coeff_arr.length; i++){
				for (int j = 0; j < coeff_arr.length; j++){
					filters[i][j][k] = coeff_arr[i][j]* filters_proto[1][k];      // minimal blur - for all sigma_y
					if (i > 0){
						filters[i][j][k] *= filters_proto[2][k]; // for Pr, Pb sigma_color
					}
					if (j <2){ // all but green 
						filters[i][j][k] *= filters_proto[0][k]; // for R,B sigma_rb
					}
					
				}
			}
		}
		if (globalDebugLevel > 0) {
			showDoubleFloatArrays sdfa_instance = new showDoubleFloatArrays(); // just for debugging?
			double [][] ff = {
					filters[0][0], filters[0][1], filters[0][2],
					filters[1][0], filters[1][1], filters[1][2],
					filters[2][0], filters[2][1], filters[2][2]};
			sdfa_instance.showArrays(ff,  dct_size,dct_size, true, "filters");
		}
		
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);

		for (int ithread = 0; ithread < threads.length; ithread++) {
			threads[ithread] = new Thread() {
				public void run() {
					int tileY,tileX;
					for (int nTile = ai.getAndIncrement(); nTile < nTiles; nTile = ai.getAndIncrement()) {
						tileY = nTile/tilesX;
						tileX = nTile - tileY * tilesX;
						for (int i = 0; i < filters.length; i++){
							for (int k = 0; k <dct_len; k++){
								yPrPb[i][tileY][tileX][k]=0.0;
								for (int j = 0; j < filters[i].length; j++){
									yPrPb[i][tileY][tileX][k] += filters[i][j][k] * dctdc_data[j][tileY][tileX][k];
//									yPrPb[i][tileY][tileX][k] += filters[i][j][k] * dctdc_data[2][tileY][tileX][k]; // make it grey level (r=g=b)
								}
							}
							
						}
					}
				}
			};
		}		      
		startAndJoin(threads);
		return yPrPb;
	}
	

	
	
	
	
	// Restore DC
	public double [] lapped_idctdc(
			final double [][][] dctdc_data,  // array [tilesY][tilesX][dct_size*dct_size+1] - last element is DC value  
			final int       dct_size,
			final int       window_type,
			final int       threadsMax,  // maximal number of threads to launch                         
			final int       globalDebugLevel)
	{
//		final int tilesX=dct_width/dct_size;
//		final int tilesY=dct_data.length/(dct_width*dct_size);
		final int tilesY=dctdc_data.length;
		final int tilesX=dctdc_data[0].length;

		final int width=  (tilesX+1)*dct_size;
		final int height= (tilesY+1)*dct_size;
		if (globalDebugLevel > 0) {
			System.out.println("lapped_idct():tilesX=   "+tilesX);
			System.out.println("lapped_idct():tilesY=   "+tilesY);
			System.out.println("lapped_idct():width=    "+width);
			System.out.println("lapped_idct():height=   "+height);
		}
		final double [] dpixels = new double[width*height];
		final Thread[] threads = newThreadArray(threadsMax);
		final AtomicInteger ai = new AtomicInteger(0);
		final AtomicInteger nser = new AtomicInteger(0);
		final int [][][] tiles_list = new int[4][][];
		for (int n=0; n<4; n++){
			int nx = (tilesX + 1 - (n &1)) / 2;
			int ny = (tilesY + 1 - ((n>>1) & 1)) / 2;
			tiles_list[n] = new int [nx*ny][2];
			int indx = 0;
			for (int i = 0;i < ny; i++) for (int j = 0; j < nx; j++){
				tiles_list[n][indx][0]=2*j+(n &1);
				tiles_list[n][indx++][1]=2*i+((n>>1) & 1);
			}
		}

		for (int i=0; i<dpixels.length;i++) dpixels[i]= 0;
		for (int n=0; n<4; n++){
			nser.set(n);
			ai.set(0);
			for (int ithread = 0; ithread < threads.length; ithread++) {
				threads[ithread] = new Thread() {
					public void run() {
						DttRad2 dtt = new DttRad2(dct_size);
						dtt.set_window(window_type);
						double [] tile_in = new double[dct_size * dct_size];
						double [] tile_dct; // = new double[dct_size * dct_size];
						double [] tile_out; //  = new double[4*dct_size * dct_size];
						int tileY,tileX;
						int n2 = dct_size * 2;
						for (int nTile = ai.getAndIncrement(); nTile < tiles_list[nser.get()].length; nTile = ai.getAndIncrement()) {
							tileX = tiles_list[nser.get()][nTile][0];
							tileY = tiles_list[nser.get()][nTile][1];
							System.arraycopy(dctdc_data[tileY][tileX], 0, tile_in, 0, tile_in.length);
							// will get rid of the DC eventually
							double dc = (dctdc_data[tileY][tileX].length > tile_in.length)?dctdc_data[tileY][tileX][tile_in.length]:0.0;
							tile_dct=dtt.dttt_iv  (tile_in, 0, dct_size);
							for (int i = 0; i<tile_dct.length; i++) tile_dct[i] += dc;
							tile_out=dtt.unfold_tile(tile_dct, dct_size);
							for (int i = 0; i < n2;i++){
								int start_line = ((tileY*dct_size + i) *(tilesX+1) + tileX)*dct_size; 
								for (int j = 0; j<n2;j++) {
									dpixels[start_line + j] += tile_out[n2 * i + j]; //  +1.0; 
								}
							}
						}
					}
				};
			}		      
			startAndJoin(threads);
		}
		return dpixels;
	}

	
	
	
	
	
	/* Create a Thread[] array as large as the number of processors available.
	 * From Stephan Preibisch's Multithreading.java class. See:
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
	 */
	static Thread[] newThreadArray(int maxCPUs) {
		int n_cpus = Runtime.getRuntime().availableProcessors();
		if (n_cpus>maxCPUs)n_cpus=maxCPUs;
		return new Thread[n_cpus];
	}
/* Start all given threads and wait on each of them until all are done.
	 * From Stephan Preibisch's Multithreading.java class. See:
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD
	 */
	public static void startAndJoin(Thread[] threads)
	{
		for (int ithread = 0; ithread < threads.length; ++ithread)
		{
			threads[ithread].setPriority(Thread.NORM_PRIORITY);
			threads[ithread].start();
		}

		try
		{   
			for (int ithread = 0; ithread < threads.length; ++ithread)
				threads[ithread].join();
		} catch (InterruptedException ie)
		{
			throw new RuntimeException(ie);
		}
	}
	
}