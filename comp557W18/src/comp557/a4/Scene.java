package comp557.a4;

/**
 *	COMP 557 - Winter 2018
 *	Assignment 4
 *	Parker King-Fournier
 *	260556983
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Simple scene loader based on XML file format.
 */
public class Scene {
	
	public double k_a = 0.8;
	public double k_d = 1.0;
	public double k_s = 1.0;
    
    /** List of surfaces in the scene */
    public List<Intersectable> surfaceList = new ArrayList<Intersectable>();
	
	/** All scene lights */
	public Map<String,Light> lights = new HashMap<String,Light>();

    /** Contains information about how to render the scene */
    public Render render;
    
    /** The ambient light colour */
    public Color3f ambient = new Color3f();

    /**  Default constructor.*/
    public Scene() {
    	this.render = new Render();
    }
    
    /**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~RENDER METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  
     *  
     *  The following methods render various scenes from the a4data directory
     *  		
     *  		render():			A general render method that will render any scene 
     *  
     *  @author parkerkingfournier
     */
    
    // Render the scene using Ray Tracing
    public void render(boolean showPanel) {
    	
    		// Variable declarations
    		Camera 			cam 		= render.camera; 
        int 				w 		= cam.imageSize.width;
        int 				h 		= cam.imageSize.height;
        int 				argb		= 255;
        int 				alpha 	= 0;
        double			r, g, b;
        double[] 		color 		= new double[3];
        double[] 		old_color 	= new double[3];
        double[] 		offset 		= new double[2];
        Ray ray = new Ray();
        
        render.init(w, h, showPanel);
        
        // Loop through each pixel p_i = (i,j)
        for ( int i = 0; i < h && !render.isDone(); i++ ) {
            for ( int j = 0; j < w && !render.isDone(); j++ ) {
          //for ( int i = 4; i < 5; i++ ) {
                //for ( int j = 0; j < 1; j++ ) {
            	
            		r = g = b = 0;
            		for( int k = 0; k < render.samples; k++) {
            			
            			if(render.samples == 1) {
            				offset = new double[] {0.0, 0.0};
            			}
            			else {
            				offset = fuzzyGridDistribution(k, render.samples);
            			}
            			
            			// Cast Ray
                		generateRay(i, j, offset, cam, ray);
                		color = cast(ray, old_color, 0);
                		r += color[0];
                		g += color[1];
                		b += color[2];
            		}//k-loop
                	
            		// Cap at white to prevent RGB overflow
            		r = Math.min(255, r/render.samples);
            		g = Math.min(255, g/render.samples);
            		b = Math.min(255, b/render.samples);
                		            		
            		// Update the render image
                	alpha = (argb<<24 | (int)r<<16 | (int)g<<8 | (int)b); 
            		render.setPixel(j, i, alpha);
            }//j-loop
        } //i-loop
        render.save();
        render.waitDone();
    }//render
    
    // A recursive cast method
    public double[] cast(Ray ray, double[] old_color, int depth) {
    			
		// Variable declarations
    		int max_depth 							= 5;
    		int shadow 								= 1;
    		double[] color 							= new double[] {0.0,0.0,0.0};
    		double[] reflected_color  				= new double[] {0.0,0.0,0.0};
    		double[] nonreflected_color 				= new double[] {0.0,0.0,0.0};
    		double 	diffuse, specular;
    				diffuse = specular  				= 0.0;
    		Light light;
    		Ray shadow_ray							= new Ray();
    		IntersectResult shadow_result			= new IntersectResult();
    	
    		// Reflection depth limit
    		if(depth < max_depth) {
	    		
	    		// Find closest intersection
	    		IntersectResult closest_result = findClosestIntersection(ray);
	    		
	    		if(closest_result.t != Double.POSITIVE_INFINITY) {
	    			
				// Cast a reflection_ray;
				Ray reflection_ray = new Ray();
				generateReflectionRay(reflection_ray, closest_result);
				reflected_color=cast(reflection_ray, color, depth+1);
	    			
		    		// For all lights
		    		for(int i = 0; i < this.lights.size(); i++) {
		    			light = this.lights.get("light" + i);
		    			
		    			// Shadows
		    			generateShadowRay(shadow_ray, closest_result.p, light); 
					shadow = inShadow(light, shadow_result, shadow_ray);
		    		
					// Calculate diffuse and specular lighting components
					diffuse	 = calculateDiffuse	(light, closest_result, shadow);
					specular	 = calculateSpecular	(light, closest_result, shadow);
					
					// Set the color in regards to the closest objects
					nonreflected_color = setColor(closest_result, light, shadow, diffuse, specular);
					
					// Take the maximum of each RGB component between the non-reflected color, and the scaled reflected color
					color[0] += Math.max(nonreflected_color[0], closest_result.material.reflectiveness*reflected_color[0]);
					color[1] += Math.max(nonreflected_color[1], closest_result.material.reflectiveness*reflected_color[1]);
					color[2] += Math.max(nonreflected_color[2], closest_result.material.reflectiveness*reflected_color[2]);
		    		}
		    		color[0] /= this.lights.size();
		    		color[1] /= this.lights.size();
		    		color[2] /= this.lights.size();
	    		}
    		}
    		else {
    			color = new double[] {this.ambient.x, this.ambient.y, this.ambient.z };
    		}
    		return color;
    }//end-cast
    
    
    /**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~RAY GENERATION METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    
    // Generate a ray through pixel (i,j).
    public static void generateRay(final int i, final int j, final double[] offset, final Camera cam, Ray ray) {
    			
    		// Construct an ortho-normal basis using the camera parameters.
    		Vector3d u = new Vector3d();
    		Vector3d v = new Vector3d();
    		Vector3d w = new Vector3d(	cam.from.x - cam.to.x,
    									cam.from.y - cam.to.y,
    									cam.from.z - cam.to.z	);
    		// create u vector by crossing up and w, making u orthogonal to both w and up
    		u.cross(cam.up, w);
    		v.cross(w, u);
    			
    		// Normalize for safety
    		u.normalize();
    		v.normalize();
    		w.normalize();
    		
    		double v_scale = (-1)*(i + 0.5 + offset[0]) + cam.imageSize.getHeight()/2;
    		double u_scale = (j+ 0.5 + offset[1]) - cam.imageSize.getWidth()/2;
    		double w_scale = (cam.imageSize.getHeight()/2) / Math.tan( ((Math.PI)/180) *(cam.fovy/2));
    		
    		u.scale(u_scale);
    		v.scale(v_scale);
    		w.scale(w_scale);
    		
    		Vector3d point = new Vector3d(cam.from);
    		point.add(u);
    		point.add(v);
    		point.sub(w);
    		
    		Vector3d view_direction = new Vector3d(	point.x - cam.from.x,
    												point.y - cam.from.y,
    												point.z - cam.from.z);
    		view_direction.normalize();
    		
    		ray.eyePoint = new Point3d(cam.from);
    		ray.viewDirection = new Vector3d(view_direction);
    }
	
	// Generate a shadow ray from a point p to a light.
	public static void generateShadowRay(Ray shadow_ray, Point3d p, Light light) {
		Vector3d p_2 = new Vector3d(p.x, p.y, p.z);
		Vector3d d = new Vector3d(	light.from.x - p.x,
									light.from.y - p.y,
									light.from.z - p.z	);
		d.normalize();
		// Add a little big of d to p
		d.scale(.01);
		p_2.add(d);
		d.normalize();
		
		shadow_ray.eyePoint = new Point3d(p_2.x, p_2.y, p_2.z);
		shadow_ray.viewDirection = d;
	}
	
	// Generate a reflection ray
	public void generateReflectionRay(Ray reflection_ray, IntersectResult closest_result) {
		//if(light != null) {
			Vector3d from_light = new Vector3d(	render.camera.from.x - closest_result.p.x,
												render.camera.from.y - closest_result.p.y,
												render.camera.from.z - closest_result.p.z	);
				from_light.scale(-1);
				from_light.normalize();
			
			Vector3d to_camera = new Vector3d(render.camera.from);
				to_camera.sub(closest_result.p);
				to_camera.normalize();
				
			reflection_ray.viewDirection = new Vector3d(closest_result.n);
				reflection_ray.viewDirection.scale(-2.0*from_light.dot(closest_result.n));
				reflection_ray.viewDirection.add(from_light);
				reflection_ray.viewDirection.normalize();
				
			reflection_ray.eyePoint = new Point3d(closest_result.p);
		}
	//}
	
		
	/**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~SHADING METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  
     */
	
	// Diffuse shading
	public double calculateDiffuse(Light light, IntersectResult closest_result, int shadow) {
		double diffuse 	= 0;
		if(light != null) {
			Vector3d to_light = new Vector3d(	light.from.x - closest_result.p.x,
												light.from.y - closest_result.p.y,
												light.from.z - closest_result.p.z	);
			to_light.normalize();
			diffuse 	= (this.k_d *light.power*Math.max(0, closest_result.n.dot(to_light)));
		}
		return diffuse;
	}
	
	// Specular shading
	public double calculateSpecular(Light light, IntersectResult closest_result, int shadow) {
		double specular 	= 0.0;
		
		if(light != null) {
			Vector3d from_light = new Vector3d(	light.from.x - closest_result.p.x,
												light.from.y - closest_result.p.y,
												light.from.z - closest_result.p.z	);
				from_light.scale(-1);
				from_light.normalize();
			
			Vector3d to_camera = new Vector3d(render.camera.from);
				to_camera.sub(closest_result.p);
				to_camera.normalize();
				
			Vector3d reflection_vector = new Vector3d(closest_result.n);
				reflection_vector.scale(-2.0*from_light.dot(closest_result.n));
				reflection_vector.add(from_light);
				reflection_vector.normalize();
			
			if(closest_result.material != null)
				{specular = (this.k_s * light.power * Math.pow(Math.max(0, to_camera.dot(reflection_vector)), closest_result.material.shinyness));}
			else 
				{specular = 0;}
		}
		return specular;
	}
	
	// Calculate the color for a given intersection
	public double[] setColor(IntersectResult closest_result, Light light, int shadow, double diffuse, double specular) {
		double[] color = new double[3];
		
		if(closest_result.t == Double.POSITIVE_INFINITY) {
			color[0] = 255*this.k_a*ambient.x/this.lights.size();
			color[1] = 255*this.k_a*ambient.y/this.lights.size();
			color[2] = 255*this.k_a*ambient.z/this.lights.size();
		}
		else {			
			color[0] = 	255 * (	this.k_a *ambient.x	* light.color.x * closest_result.material.diffuse.x 	
								+ shadow *(diffuse	* light.color.x * closest_result.material.diffuse.x
										+ specular	* light.color.x * closest_result.material.specular.x)); 
	
			color[1] = 	255 * (	this.k_a *ambient.y* light.color.y * closest_result.material.diffuse.y 	
								+ shadow *(diffuse	* light.color.y * closest_result.material.diffuse.y
										+ specular	* light.color.y * closest_result.material.specular.y));
	
			color[2] = 	255 * ( this.k_a *ambient.z	* light.color.z * closest_result.material.diffuse.z 	
								+ shadow *(diffuse	* light.color.z * closest_result.material.diffuse.z
										+ specular	* light.color.z * closest_result.material.specular.z));
		}
		return color;
	}
	

	/**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~SAMPLING DISTRIBUTIONS~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */
	
	// A distribution approximating a circle around each point
	public static double[] circleDistribution(int sample_number, int samples) {
		double[] coordinates = new double[2];
		
		coordinates[0] = (0.4)* Math.cos( ((double)sample_number/(double)samples) * 2*Math.PI);
		coordinates[1] = (0.4)* Math.sin( ((double)sample_number/(double)samples) * 2*Math.PI);

		return coordinates;
	}
	
	// A distribution of random noise
	public static double[] randomDistribution(){
		double[] coordinates = new double[2];
		
		coordinates[0] = 0.001*(2*Math.random()-1);
		coordinates[1] = 0.001*(2*Math.random()-1);
		
		return coordinates;
		
	}
	
	// A grid distribution with random noise introduced
	public double[] fuzzyGridDistribution(int sample_number, int samples) {
		double[] coordinates = new double[2];
		
		if(Math.floor(Math.sqrt(samples)) != Math.sqrt(samples)) {
			System.out.print("\nWhen using the grid distribution the number of samples must be a perfect square!\n");
			coordinates[0] = 0;
			coordinates[1] = 0;
		}
		else {
			double side_length = Math.sqrt(samples);
			coordinates[0] = ((Math.floor((double)(sample_number)/side_length))/(side_length-1) - 0.5) + (Math.random()-0.5)/10;
			coordinates[1] = (((sample_number%side_length)/(side_length-1)) - 0.5) + (Math.random()-0.5)/10;
		}
		
		if(this.render.samples == 1) {
			System.out.println("\nCome on, dont super sample with one sample you pussy.\n");
		}
		
		return coordinates;
	}
	
	
    /**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~MISC METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

	// Find if something is in a shadow
	public int inShadow(final Light light, IntersectResult shadowResult, Ray shadowRay) {
		int in_shadow = 1;
		Intersectable object;
		
		for(int i = 0; i < this.surfaceList.size(); i++){
			object = this.surfaceList.get(i);
			
			if(object.getType().equals("scene_node")) {
				SceneNode node = (SceneNode)object;
				
				for(int j = 0; j < node.children.size(); j++) {
					node.children.get(j).intersect(shadowRay, shadowResult);
					if(shadowResult.t != Double.POSITIVE_INFINITY) {
						in_shadow = 0; 
						return in_shadow;
					}
				}	
			}
			else {
				object.intersect(shadowRay, shadowResult);
				if(shadowResult.t != Double.POSITIVE_INFINITY) {
					in_shadow = 0; 
					return in_shadow;
				}
			}
		}
		return in_shadow;
	}  
	
	public IntersectResult findClosestIntersection(Ray ray) {
		double 					min_t 		= Double.POSITIVE_INFINITY;	
		Intersectable			child		= this.surfaceList.get(0);
		Intersectable			object		= this.surfaceList.get(0);
		IntersectResult 			result 		= new IntersectResult();
		IntersectResult 	closest_result  		= new IntersectResult();
						closest_result.t 	= min_t;

		for(int i = 0; i < this.surfaceList.size(); i++) {
			
			//Get the i'th object
			object = this.surfaceList.get(i);
			
			//find the intersection
			object.intersect(ray, result);
			if(result.t < min_t && !object.getType().equals("scene_node")) {
				object 					= child;
				min_t 					= result.t;
				closest_result.t	 		= result.t;
				closest_result.p 		= new Point3d(result.p);
				closest_result.n 		= new Vector3d(result.n);
				closest_result.material 	= result.material;
			}
			
			// if its a scene_node
			while(object.getType().equals("scene_node")) {
				SceneNode node = (SceneNode)object;
				
				double temp = min_t;
				
				// find its closest child
				for(int j = 0; j < node.children.size(); j++) {
					
					
					//System.out.println(i + "," + j);
					//System.out.println(min_t);
					//System.out.println();
					
					child = node.children.get(j);
					child.intersect(ray, result);
					
					if(result.t < min_t) {
						object 					= child;
						min_t 					= result.t;
						closest_result.t	 		= result.t;
						closest_result.p 		= new Point3d(result.p);
						closest_result.n 		= new Vector3d(result.n);
						closest_result.material 	= result.material;
					}
				}
				if(min_t == temp) {
					//closest_result.t = Double.POSITIVE_INFINITY;
					break;
				}
			}
		}
		
		return closest_result;
	}
}
