package comp557.a4;

/**
 *	@author parkerkingfournier
 */

import java.util.ArrayList;
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
	
	public boolean shading 		= true;
	public boolean shadows 		= false;
	public boolean reflection 	= false;
	public boolean refraction	= false;
	public boolean fresnel 		= false;
	
	public double k_a = 0.8;
	public double k_d = 1.0;
	public double k_s = 1.0;
	
	public int noise_height 	= 128;
	public int noise_width 	= 128;
	public double[][] noise = new double[noise_height][noise_width];
    
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
        
        //Generate a noise array
        generateNoise();
        
	    // Loop through each pixel p_i = (i,j)
	    for ( int i = 0; i < h && !render.isDone(); i++ ) {
	        for ( int j = 0; j < w && !render.isDone(); j++ ) {	
	            r = g = b = 0;
	            	for( int k = 0; k < render.samples; k++) {
	            		// Super sampling
	            		offset = fuzzyGridDistribution(k, render.samples, i, j, k);
	       
	            		// Cast Ray
	                generateRay(i, j, offset, cam, ray);
	                
	                	color = cast(ray, old_color, 0, i, j);
	                	r += color[0];
	                	g += color[1];
	                	b += color[2];
	            	}//k-loop
	                	
	            	// Cap at white to prevent RGB overflow
	            	r = Math.min(255, r/render.samples);
	            	g = Math.min(255, g/render.samples);
	            	b = Math.min(255, b/render.samples);
	                		            		
	            	// Update the image pixel
	            alpha = (argb<<24 | (int)r<<16 | (int)g<<8 | (int)b); 
	            	render.setPixel(j, i, alpha);
	            }//j-loop
	        } //i-loop
        render.save();
        render.waitDone();
    }//render
    
    // A recursive cast method
    public double[] cast(Ray ray, double[] old_color, int depth, int x, int y) {
    			
		// Variable declarations
    		int max_depth 							= 5;
    		int shadow 								= 1;
    		double[] color 							= new double[] {0.0,0.0,0.0};
    		double[] reflected_color  				= new double[] {0.0,0.0,0.0};
    		double[] refracted_color  				= new double[] {0.0,0.0,0.0};
    		double[] object_color 					= new double[] {0.0,0.0,0.0};
    		double 	diffuse, specular;
    				diffuse = specular  				= 0.0;
    		double 	fresnel_reflected				= 1.0;
    		double 	fresnel_refracted				= 0.0;
    		Light light;
    		IntersectResult shadow_result			= new IntersectResult();
    	
    		// Reflection depth limit
    		if(depth < max_depth) {
	    		
	    		// Find closest intersection
	    		IntersectResult closest_result = findClosestIntersection(ray);
	    		
	    		if(closest_result.t != Double.POSITIVE_INFINITY) {	  
		    		// For all lights
		    		for(int i = 0; i < this.lights.size(); i++) {
		    			light = this.lights.get("light" + i);
		    			
		    			// Calculate diffuse and specular lighting components
		    			if(this.shading == true) {
						diffuse	 = calculateDiffuse	(light, closest_result);
						specular	 = calculateSpecular	(light, closest_result, shadow);
		    			}
		    			else {
		    				diffuse = specular = 0;
		    			}
						
			    		// Cast a shadow_ray;
		    			if(this.shadows == true) {
			    			Ray shadow_ray = new Ray();
			    			generateShadowRay(shadow_ray, closest_result.p, light); 
						shadow = inShadow(light, shadow_result, shadow_ray);
		    			}
		    			else {
		    				shadow = 1;
		    			}
		    			
		    			// Cast a reflection_ray;
		    			if(this.reflection == true) {
		    				Ray reflection_ray = new Ray();
		    				generateReflectionRay(reflection_ray, closest_result);
		    				reflected_color=cast(reflection_ray, color, depth+1, x, y);
		    			}
					
					// Cast a refraction_ray;
		    			if(this.refraction ==  true) {
						Ray refraction_ray = new Ray();
						generateRefractionRay(ray, refraction_ray, closest_result);
						refracted_color=cast(refraction_ray, color, depth+1, x, y);
		    			}
		    			
					// Calculate the fresnel coefficient for the intersection
		    			if(this.fresnel == true) {
						fresnel_reflected = calculateFresnel(ray, closest_result);
						fresnel_refracted = 1 - fresnel_refracted;
		    			}
		    			else {
		    				fresnel_reflected = fresnel_refracted = 1;
		    			}
					
					// Find the color of the closest object
					object_color = setColor(closest_result, light, shadow, diffuse, specular, x, y);
					
					// Combine the object color, reflected color, and the scaled refracted color
					color[0] += (fresnel_refracted)	*	(closest_result.material.refractiveness)*refracted_color[0] + 
								(fresnel_reflected)	*	(closest_result.material.reflectiveness)*reflected_color[0] + 
								(fresnel_reflected) * 	(1-closest_result.material.reflectiveness)*object_color[0];
					
					color[1] += (fresnel_refracted)	*	(closest_result.material.refractiveness)*refracted_color[1] + 
								(fresnel_reflected)	*	(closest_result.material.reflectiveness)*reflected_color[1] + 
								(fresnel_reflected) * 	(1-closest_result.material.reflectiveness)*object_color[1];
					
					color[2] += (fresnel_refracted)	*	(closest_result.material.refractiveness)*refracted_color[2] + 
								(fresnel_reflected)	*	(closest_result.material.reflectiveness)*reflected_color[2] + 
								(fresnel_reflected) * 	(1-closest_result.material.reflectiveness)*object_color[2];

					
		    		}
		    		// Average over all lights, iteratively
		    		color[0] /= this.lights.size();
		    		color[1] /= this.lights.size();
		    		color[2] /= this.lights.size();
	    		}
	    		else {
	    			color = new double[] {this.ambient.x, this.ambient.y, this.ambient.z };
	    		}
    		}
    		else {
    			color = new double[] {this.ambient.x, this.ambient.y, this.ambient.z };
    		}
    		return color;
    }
    
    		
	/**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~COLOR AND SHADING METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~  
     */
	// Calculate the color for a given intersection
	public double[] setColor(IntersectResult closest_result, Light light, int shadow, double diffuse, double specular, int i, int j) {
		float[] 	 rgb 	= new float[3];
		double[] color	 = new double[3];
		
		if(closest_result.t == Double.POSITIVE_INFINITY) {
			color = new double[] {255*this.k_a*ambient.x/this.lights.size(), 255*this.k_a*ambient.x/this.lights.size(), 255*this.k_a*ambient.x/this.lights.size()};
		}
		else	 if(closest_result.material.pattern.equals("granite")) {
			rgb = makeGranite(closest_result, 0.08, i, j);
		}
		else	 if(closest_result.material.pattern.equals("sine")) {
			rgb = makeSine(closest_result, 0.08);
		}
		else	 if(closest_result.material.pattern.equals("checker")) {
			rgb = makeChecker(closest_result, i, j);
		}
		else	 if(closest_result.material.pattern.equals("pattern")) {
			rgb = makePattern(closest_result, i, j);
		}
		else{
			rgb = new float[] {closest_result.material.diffuse.x, closest_result.material.diffuse.y, closest_result.material.diffuse.z};
		}
			
			color[0] = 	255 * (	this.k_a *this.ambient.x	* light.color.x * rgb[0] 	
							+ shadow *(diffuse	* light.color.x * rgb[0] 
							+ specular	* light.color.x * closest_result.material.specular.x)); 
		
			color[1] = 	255 * (	this.k_a *this.ambient.y* light.color.y * rgb[1] 	
							+ shadow *(diffuse	* light.color.y * rgb[1] 
							+ specular	* light.color.y * closest_result.material.specular.y));
		
			color[2] = 	255 * ( this.k_a *this.ambient.z	* light.color.z * rgb[2]  	
							+ shadow *(diffuse	* light.color.z * rgb[2] 
							+ specular	* light.color.z * closest_result.material.specular.z));
			
		return color;
	}
	
	public double calculateDiffuse(Light light, IntersectResult closest_result) {
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
	
	public double calculateFresnel(Ray ray, IntersectResult closest_result){
		double k_r 		= 0.0;
		
		float etai 		= 1;
		float etat 		= closest_result.material.ior; 
		double cos_i 	= ray.viewDirection.dot(closest_result.n);
		double sin_t = (etai/etat) * Math.sqrt(Math.max(0.0, 1-cos_i*cos_i)); 
		
		if(cos_i > 0) {
			etai 	= closest_result.material.ior;
			etat 	= 1; 
		}
		
		if(sin_t >= 1) {
			k_r = 1;
		}
		else { 
	        double cost = Math.sqrt(Math.max(0.0, 1-sin_t*sin_t)); 
	        cos_i = Math.abs(cos_i); 
	        double Rs = ((etat * cos_i) - (etai * cost)) / ((etat * cos_i) + (etai * cost)); 
	        double Rp = ((etai * cos_i) - (etat * cost)) / ((etai * cos_i) + (etat * cost)); 
	        k_r = (Rs * Rs + Rp * Rp) / 2; 
	    }
		
		
		return k_r;
	}
	
	
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
		
		// Add a little bit of the view direction to the view point
		reflection_ray.viewDirection.scale(0.01);
		reflection_ray.eyePoint.add(reflection_ray.viewDirection);
		reflection_ray.viewDirection.normalize();
		}
	
	public void generateRefractionRay(Ray ray, Ray refraction_ray, IntersectResult closest_result) {
			
			Vector3d temp;
		
			refraction_ray.viewDirection 	= new Vector3d(closest_result.n);
			refraction_ray.eyePoint 			= new Point3d(closest_result.p);
			
			double cos_i = closest_result.n.dot(ray.viewDirection);
			double eta = 1/closest_result.material.ior;
			
			if(cos_i < 0) {
				cos_i *= -1;
			}
			else	{
				eta = 1/eta;
				refraction_ray.viewDirection.scale(-1);
			}
			
			double k = 1 - eta*eta*(1 - cos_i*cos_i);
			if(k < 0) {
				refraction_ray.viewDirection 	= new Vector3d(0, 0, 0);
				refraction_ray.eyePoint 			= new Point3d(0, 0, 0);
			}
			else {
				refraction_ray.viewDirection.scale(eta*cos_i - Math.sqrt(k));
				temp = new Vector3d(ray.viewDirection);
				temp.scale(eta);
				refraction_ray.viewDirection.add(temp);
			}
			
			temp = new Vector3d(refraction_ray.viewDirection);
			temp.scale(0.01);
			refraction_ray.eyePoint.add(temp);
		}	
	
		
    /**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~INTERSECTING METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
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
										
					child = node.children.get(j);
					
					if(!child.getType().equals("mesh")){
						child.intersect(ray, result);
					}

					if(result.t < min_t && !child.getType().equals("mesh")) {
						object 					= child;
						min_t 					= result.t;
						closest_result.t	 		= result.t;
						closest_result.p 		= new Point3d(result.p);
						closest_result.n 		= new Vector3d(result.n);
						closest_result.material 	= result.material;
					}
				}
				if(min_t == temp) {
					break;
				}				
			}
		}
		
		return closest_result;
	}
	
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
				if(shadowResult.t != Double.POSITIVE_INFINITY && shadowResult.t > 0) {
					in_shadow = 0; 
					return in_shadow;
				}
			}
		}
		return in_shadow;
	}  
	
	
	
    /**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~PATTERN METHODS~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
	public float[] makeGranite(IntersectResult closest_result, double num_stripes, int i, int j) {		
		float temp = (float) (num_stripes*(i+j) + closest_result.material.turbulence*turbulence(i,j,32)/255);
		float[] granite_color = new float[]{	(float) (closest_result.material.diffuse.x*Math.abs(Math.sin(temp))),
											(float) (closest_result.material.diffuse.y*Math.abs(Math.sin(temp))),
											(float) (closest_result.material.diffuse.z*Math.abs(Math.sin(temp)))	};		
		return granite_color;
	}
	
	public float[] makeSine(IntersectResult closest_result, double num_stripes) {		
		float[] sine_color = new float[]{	(float) (closest_result.material.diffuse.x*Math.abs(Math.sin(8*(closest_result.p.x+closest_result.p.y)))),
											(float) (closest_result.material.diffuse.y*Math.abs(Math.sin(8*(closest_result.p.x+closest_result.p.y)))),
											(float) (closest_result.material.diffuse.z*Math.abs(Math.sin(8*(closest_result.p.x+closest_result.p.y))))	};		
		return sine_color;
	}
	
	public float[] makeChecker(IntersectResult closest_result, int i, int j) {	
			
		float temp = (float) ( closest_result.material.turbulence*turbulence(i,j,32)/255);
		if(closest_result.material.turbulence == 0){
			temp = 1;
		}
		
		double pattern = (Math.abs(Math.floor(temp*closest_result.p.x)) % 2) * (Math.abs(Math.floor(temp*closest_result.p.z)) % 2);
		float[] sine_color = new float[]{	(float) (closest_result.material.diffuse.x * pattern),
											(float) (closest_result.material.diffuse.y * pattern),
											(float) (closest_result.material.diffuse.z * pattern)	};		
		return sine_color;
	}
	
	// Temporary color pattern - calculates color as a function of position
	public float[] makePattern(IntersectResult closest_result, int i, int j) {	
		
		double x = Math.abs(closest_result.p.x);
		double z = Math.abs(closest_result.p.z);

		float temp = (float) ( closest_result.material.turbulence*turbulence(i,j,32)/255);
		if(closest_result.material.turbulence == 0){
			temp = 1;
		}
		
		float[] sine_color = new float[]{	(float) (Math.min(1, x*temp)),
											(float) (Math.min(1, temp/x)),
											(float) (1)};		
		return sine_color;
	}
	
	/**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~NOISE AND TURBULENCE METHODS~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
	public double smoothNoise(double x, double y){
	   //get fractional part of x and y
	   double fractX = x - (int)x;
	   double fractY = y - (int)y;

	   //wrap around
	   int x1 = ( (int)x + this.noise_width) % this.noise_width;
	   int y1 = ( (int)y + this.noise_height) % this.noise_height;

	   //neighbor values
	   int x2 = (x1 + this.noise_width - 1) % this.noise_width;
	   int y2 = (y1 + this.noise_height - 1) % this.noise_height;

	   //smooth the noise with bilinear interpolation
	   double value = 0.0;
	   value += fractX     * fractY     * noise[y1][x1];
	   value += (1 - fractX) * fractY     * noise[y1][x2];
	   value += fractX     * (1 - fractY) * noise[y2][x1];
	   value += (1 - fractX) * (1 - fractY) * noise[y2][x2];

	   return value;
	}
	
	public void generateNoise(){
		for (int y = 0; y < this.noise_height; y++) {
			  for (int x = 0; x < this.noise_width; x++) {
			    noise[y][x] = Math.random();
			  }
		}
	}
	
	double turbulence(double x, double y, double size) {
		double value = 0.0;
		double initialSize = size;

		while(size >= 1){
			value += smoothNoise(x/size, y/size)*size;
		    size /= 2.0;
		}
	
		return(128.0 * value / initialSize);
	}
	
	/**
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~SAMPLING DISTRIBUTIONS~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	 */
	public static double[] circleDistribution(int sample_number, int samples, int i, int j) {
		double[] coordinates = new double[] {(0.4)* Math.cos( ((double)sample_number/(double)samples) * 2*Math.PI), (0.4)* Math.sin( ((double)sample_number/(double)samples) * 2*Math.PI)};
		return coordinates;
	}
	
	public static double[] randomDistribution(){
		double[] coordinates = new double[] {0.001*(2*Math.random()-1), 0.001*(2*Math.random()-1)};
		return coordinates;	
	}
	
	public double[] fuzzyGridDistribution(int sample_number, int samples, int i, int j, int k) {
		double[] coordinates = new double[2];
		
		if(Math.floor(Math.sqrt(samples)) != Math.sqrt(samples) ) {
			if(i == 0 && j == 0 && k == 0) {
				System.out.print("\nWhen using the grid distribution the number of samples must be a perfect square!\n");
				System.out.print("Sampling the center of each pixel as default.");
			}
			coordinates[0] = 0;
			coordinates[1] = 0;
		}
		else {
			double side_length = Math.sqrt(samples);
			coordinates[0] = ((Math.floor((double)(sample_number)/side_length))/(side_length-1) - 0.5) + (Math.random()-0.5)/10;
			coordinates[1] = (((sample_number%side_length)/(side_length-1)) - 0.5) + (Math.random()-0.5)/10;
		}
		
		if(this.render.samples == 1) {
			coordinates[0] = 0;
			coordinates[1] = 0;
		}
		
		return coordinates;
	}
}
