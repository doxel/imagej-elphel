/*
 **
 ** EyesisSubCameraParameters.java
 **
 ** Copyright (C) 2011-2014 Elphel, Inc.
 **
 ** -----------------------------------------------------------------------------**
 **  
 **  EyesisSubCameraParameters.java is free software: you can redistribute it and/or modify
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
import java.util.Properties;

    public class EyesisSubCameraParameters{
    	// origin is on the rotation axis of the tube body closest to the goniometer horizontal axis
    	public double azimuth; // azimuth of the lens entrance pupil center, degrees, clockwise looking from top
    	public double radius;  // mm, distance from the rotation axis
    	public double height;       // mm, up - from the origin point
    	public double phi;     // degrees, optical axis from azimuth/r vector, clockwise
    	public double theta;   // degrees, optical axis from the eyesis horizon, positive - up
    	public double psi;     // degrees, rotation (of the sensor) around the optical axis. Positive if camera is rotated clockwise looking to the target
		public double focalLength=4.5;
		public double pixelSize=  2.2; //um
		public double distortionRadius=  2.8512; // mm - half width of the sensor
		public double distortionA8=0.0; //r^8 (normalized to focal length or to sensor half width?)
		public double distortionA7=0.0; //r^7 (normalized to focal length or to sensor half width?)
		public double distortionA6=0.0; //r^6 (normalized to focal length or to sensor half width?)
		public double distortionA5=0.0; //r^5 (normalized to focal length or to sensor half width?)
		public double distortionA=0.0; // r^4 (normalized to focal length or to sensor half width?)
		public double distortionB=0.0; // r^3
		public double distortionC=0.0; // r^2
		public double px0=1296.0;          // center of the lens on the sensor, pixels
		public double py0=968.0;           // center of the lens on the sensor, pixels
		public double channelWeightDefault=1.0;
		public double channelWeightCurrent=1.0;
		public int [][] defectsXY=null; // pixel defects coordinates list (starting with worst)
		public double [] defectsDiff=null; // pixel defects value (diff from average of neighbors), matching defectsXY
    	public EyesisSubCameraParameters(
    			double azimuth, // azimuth of the lens entrance pupil center, degrees, clockwise looking from top
    			double radius,  // mm, distance from the rotation axis
    			double height,  // mm, up from the origin point
    			double phi,     // degrees, optical axis from azimuth/r vector, clockwise
    			double theta,   // degrees, optical axis from the eyesis horizon, positive - up
    			double psi,      // degrees, rotation (of the sensor) around the optical axis. Positive if camera is rotated clockwise looking to the target
    			double focalLength,
    			double pixelSize,//um
    			double distortionRadius, //mm - half width of the sensor
    			double distortionA8, // r^8 
    			double distortionA7, // r^7 
    			double distortionA6, // r^5 
    			double distortionA5, // r^5 
    			double distortionA, // r^4 (normalized to focal length or to sensor half width?)
    			double distortionB, // r^3
    			double distortionC, // r^2
    			double px0,           // center of the lens on the sensor, pixels
    			double py0,           // center of the lens on the sensor, pixels
    			double channelWeightDefault
    	){
    		this.azimuth=azimuth;
    		this.radius=radius;
    		this.height=height;
    		this.phi=phi;
    		this.theta=theta;
    		this.psi=psi;
    		this.focalLength=focalLength;
    		this.pixelSize=  pixelSize; //um
    		this.distortionRadius=  distortionRadius; // mm - half width of the sensor
    		this.distortionA8=distortionA8;
    		this.distortionA7=distortionA7;
    		this.distortionA6=distortionA6;
    		this.distortionA5=distortionA5;
    		this.distortionA=distortionA; // r^4 (normalized to focal length or to sensor half width?)
    		this.distortionB=distortionB; // r^3
    		this.distortionC=distortionC; // r^2
    		this.px0=px0;
    		this.py0=py0;
    		this.channelWeightDefault=channelWeightDefault;
    		this.channelWeightCurrent=this.channelWeightDefault;
    		this.defectsXY=null; // pixel defects coordinates list (starting with worst)
    		this.defectsDiff=null; // pixel defects value (diff from average of neighbors), matching defectsXY

    	}
    	// defects are not cloned!
    	public EyesisSubCameraParameters clone() {
    		return new EyesisSubCameraParameters(
    				this.azimuth,
    				this.radius,
    				this.height,
    				this.phi,
    				this.theta,
    				this.psi,
    	    		this.focalLength,
    	    		this.pixelSize,
    	    		this.distortionRadius,
    	    		this.distortionA8,
    	    		this.distortionA7,
    	    		this.distortionA6,
    	    		this.distortionA5,
    	    		this.distortionA,
    	    		this.distortionB,
    	    		this.distortionC,
    	    		this.px0,
    	    		this.py0,
    	    		this.channelWeightDefault
    				);
    	}

    	public void setProperties(String prefix,Properties properties){
    		properties.setProperty(prefix+"azimuth",this.azimuth+"");
    		properties.setProperty(prefix+"radius",this.radius+"");
    		properties.setProperty(prefix+"height",this.height+"");
    		properties.setProperty(prefix+"phi",this.phi+"");
    		properties.setProperty(prefix+"theta",this.theta+"");
    		properties.setProperty(prefix+"psi",this.psi+"");
			properties.setProperty(prefix+"focalLength",this.focalLength+"");
			properties.setProperty(prefix+"pixelSize",this.pixelSize+"");
			properties.setProperty(prefix+"distortionRadius",this.distortionRadius+"");
			properties.setProperty(prefix+"distortionA8",this.distortionA8+"");
			properties.setProperty(prefix+"distortionA7",this.distortionA7+"");
			properties.setProperty(prefix+"distortionA6",this.distortionA6+"");
			properties.setProperty(prefix+"distortionA5",this.distortionA5+"");
			properties.setProperty(prefix+"distortionA",this.distortionA+"");
			properties.setProperty(prefix+"distortionB",this.distortionB+"");
			properties.setProperty(prefix+"distortionC",this.distortionC+"");
			properties.setProperty(prefix+"px0",this.px0+"");
			properties.setProperty(prefix+"py0",this.py0+"");
			properties.setProperty(prefix+"channelWeightDefault",this.channelWeightDefault+"");
    	}
    	public void getProperties(String prefix,Properties properties){
    		if (properties.getProperty(prefix+"azimuth")!=null)
    			this.azimuth=Double.parseDouble(properties.getProperty(prefix+"azimuth"));
    		if (properties.getProperty(prefix+"radius")!=null)
    			this.radius=Double.parseDouble(properties.getProperty(prefix+"radius"));
    		if (properties.getProperty(prefix+"height")!=null)
    			this.height=Double.parseDouble(properties.getProperty(prefix+"height"));
    		if (properties.getProperty(prefix+"phi")!=null)
    			this.phi=Double.parseDouble(properties.getProperty(prefix+"phi"));
    		if (properties.getProperty(prefix+"theta")!=null)
    			this.theta=Double.parseDouble(properties.getProperty(prefix+"theta"));
    		if (properties.getProperty(prefix+"psi")!=null)
    			this.psi=Double.parseDouble(properties.getProperty(prefix+"psi"));
			if (properties.getProperty(prefix+"focalLength")!=null)
				this.focalLength=Double.parseDouble(properties.getProperty(prefix+"focalLength"));
			if (properties.getProperty(prefix+"pixelSize")!=null)
				this.pixelSize=Double.parseDouble(properties.getProperty(prefix+"pixelSize"));
			if (properties.getProperty(prefix+"distortionRadius")!=null)
				this.distortionRadius=Double.parseDouble(properties.getProperty(prefix+"distortionRadius"));
			if (properties.getProperty(prefix+"distortionA8")!=null)
				this.distortionA8=Double.parseDouble(properties.getProperty(prefix+"distortionA8"));
			if (properties.getProperty(prefix+"distortionA7")!=null)
				this.distortionA7=Double.parseDouble(properties.getProperty(prefix+"distortionA7"));
			if (properties.getProperty(prefix+"distortionA6")!=null)
				this.distortionA6=Double.parseDouble(properties.getProperty(prefix+"distortionA6"));
			if (properties.getProperty(prefix+"distortionA5")!=null)
				this.distortionA5=Double.parseDouble(properties.getProperty(prefix+"distortionA5"));
			if (properties.getProperty(prefix+"distortionA")!=null)
				this.distortionA=Double.parseDouble(properties.getProperty(prefix+"distortionA"));
			if (properties.getProperty(prefix+"distortionB")!=null)
				this.distortionB=Double.parseDouble(properties.getProperty(prefix+"distortionB"));
			if (properties.getProperty(prefix+"distortionC")!=null)
				this.distortionC=Double.parseDouble(properties.getProperty(prefix+"distortionC"));
			if (properties.getProperty(prefix+"px0")!=null)
				this.px0=Double.parseDouble(properties.getProperty(prefix+"px0"));
			if (properties.getProperty(prefix+"py0")!=null)
				this.py0=Double.parseDouble(properties.getProperty(prefix+"py0"));
			if (properties.getProperty(prefix+"channelWeightDefault")!=null) {
				this.channelWeightDefault=Double.parseDouble(properties.getProperty(prefix+"channelWeightDefault"));
	    		this.channelWeightCurrent=this.channelWeightDefault;
			}
    	}
    	public void setChannelWeightCurrent(
    			double weight){
    		this.channelWeightCurrent=weight;
    	}
    	public double getChannelWeightCurrent(){
    		return this.channelWeightCurrent;
    	}
    	public double getChannelWeightDefault(){
    		return this.channelWeightDefault;
    	}

    }
