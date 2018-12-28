package comp557.a4;

import javax.vecmath.Point3d;

/**
 *	COMP 557 - Winter 2018
 *	Assignment 4
 *	Parker King-Fournier
 *	260556983
 */

import javax.vecmath.Vector3d;

/**
 * Class for a plane at y=0.
 * 
 * This surface can have two materials.  If both are defined, a 1x1 tile checker 
 * board pattern should be generated on the plane using the two materials.
 */
public class Plane extends Intersectable{
    
	/** The second material, if non-null is used to produce a checker board pattern. */
	Material material2;
	
	public String object_type = "plane";
	
	
	/** The plane normal is the y direction */
	public static final Vector3d n = new Vector3d( 0, 1, 0 );
    
    /**
     * Default constructor
     */
    public Plane(){
    		super();
    }
     
    @Override
    public String getType() {
    		return "plane";
    }
    
    @Override
    public void intersect( Ray ray, IntersectResult result ) {
    	
        Vector3d p = new Vector3d(ray.eyePoint.x, ray.eyePoint.y, ray.eyePoint.z);
        Vector3d d = new Vector3d(ray.viewDirection.x, ray.viewDirection.y, ray.viewDirection.z);
        Vector3d position = new Vector3d();
        
        
        double r;
        double m = ray.viewDirection.y;
        double b = ray.eyePoint.y;
        r = (-1)*b/m;
        
        position.x = ray.eyePoint.x + r*ray.viewDirection.x;
        position.y = 0;
        position.z = ray.eyePoint.z + r*ray.viewDirection.z;
        
        Vector3d to_point = new Vector3d(	position.x - ray.eyePoint.x,
        										position.y - ray.eyePoint.y,
        										position.z - ray.eyePoint.z );     
        
       double t = to_point.length();
		
        if(r <= 0 || r == Double.POSITIVE_INFINITY) {
        		result.t = Double.POSITIVE_INFINITY;
			result.p = new Point3d	(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
			result.n = new Vector3d	(0, 1, 0);
			result.n.normalize();
        }
        else {
        		position = new Vector3d(d);
			position.scale(t);
			position.add(p);
			
			result.t = t;
			result.p = new Point3d	(position.x, position.y, position.z);
			result.n = new Vector3d	(0, 1, 0);
			result.n.normalize();
			
			if(evenQuadrant(result.p))
				{result.material = this.material;}
			else
				{result.material = this.material2;}
        }
    }
    
    /** An even quadrant is any quadrant that would be colored in if one
	 *  was making a checkered pattern of colored 1x1 squares, starting with a
	 *	square occupying the quadrant (+x, +z). These quadrants will have the property
	 *
	 *			[Ceiling(x_in_even_quadrant) + Ceiling(z_in_even_quadrant)] is even.
	 *
	 *	Odd quadrants have the property
	 *
	 *			[Ceiling(x_in_odd_quadrant) + Ceiling(z_in_odd_quadrant)] is odd.
	 *
	 *	But are dealt with knowing that 
	 *
	 *			odd_quadrants U even_quadrants = all_quadrants
	 *
	 *	@author parkerkingfournier
	 *	@param a Point3d specifying where a ray intersects an object
	 *	@return True if a point is in an even quadrant. False if else. 
	 */ 
	public static boolean evenQuadrant(Point3d p) {
		boolean b = false;
		if( (Math.ceil(p.x) + Math.ceil(p.z))%2 == 0)	{b = true;}
		return b;
	}
}
