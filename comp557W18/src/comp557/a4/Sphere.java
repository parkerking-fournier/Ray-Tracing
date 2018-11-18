package comp557.a4;

import javax.vecmath.Matrix4d;

/**
 *	COMP 557 - Winter 2018
 *	Assignment 4
 *	Parker King-Fournier
 *	260556983
 */

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple sphere class.
 */
public class Sphere extends Intersectable {

	
	/** Radius of the sphere. */
	public double radius = 1;
    
	/** Location of the sphere center. */
	public Point3d center = new Point3d( 0, 0, 0 );
    
    /**
     * Default constructor
     */
    public Sphere() {
    	super();
    }
    
    /**
     * Creates a sphere with the request radius and center. 
     * 
     * @param radius
     * @param center
     * @param material
     */
    public Sphere( double radius, Point3d center, Material material, Matrix4d M, Matrix4d Minv ) {
    		super();
    		this.radius = radius;
    		this.center = center;
    		this.material = material;
    }
    
    @Override
    public String getType() {
    		return "sphere";
    }
    
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    	
    		double d_squared, t_ca, t_hc, t;
    		Vector3d point;
    	
    		Vector3d L = new Vector3d(this.center);
    		L.sub(ray.eyePoint);
    		
    		t_ca = L.dot(ray.viewDirection);
    		if(t_ca <= 0) {
    			result.t = Double.POSITIVE_INFINITY;
    			result.p = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    			result.n = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    			result.n.normalize();
    			result.material = null;
    		}
    		else {
    			d_squared = L.dot(L) - t_ca*t_ca;
    			
    			if(d_squared > this.radius*this.radius) {
    				result.t = Double.POSITIVE_INFINITY;
        			result.p = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        			result.n = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        			result.n.normalize();
        			result.material = null;
    			}
    			else {
    				t_hc = Math.sqrt(this.radius*this.radius - d_squared);
    				t = Math.min(t_ca + t_hc, t_ca-t_hc);
    				
    				
    				point = new Vector3d(ray.viewDirection);
    				point.scale(t);
    				point.add(ray.eyePoint);
    				
    				result.t = t;
    				result.p = new Point3d(point.x, point.y, point.z);
    				result.n = new Vector3d(point.x - this.center.x,
    										point.y - this.center.y,
    										point.z - this.center.z);
    				result.n.normalize();
    				result.material = this.material;
    			}
    		}

    		// Final check to make sure t > 0
    		if(result.t <= 0) {
    			result.t = Double.POSITIVE_INFINITY;
    			result.p = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    			result.n = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    			result.n.normalize();
    			result.material = null;
    		}
    }  
}
