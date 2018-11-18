package comp557.a4;

/**
 *	COMP 557 - Winter 2018
 *	Assignment 4
 *	Parker King-Fournier
 *	260556983
 */

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import java.util.*;

/**
 * A simple box class. A box is defined by it's lower (@see min) and upper (@see max) corner. 
 */
public class Box extends Intersectable {

	public Point3d max;
	public Point3d min;
	
    /**
     * Default constructor. Creates a 2x2x2 box centered at (0,0,0)
     */
    public Box() {
    		super();
    		this.max = new Point3d( 1, 1, 1 );
    		this.min = new Point3d( -1, -1, -1 );
    }	
    
    @Override
    public String getType() {
    		return "box";
    }

	@Override
	public void intersect(Ray ray, IntersectResult result) {
		
		
		double	x_near, x_far,
				y_near, y_far,
				z_near, z_far;
		Vector3d p = new Vector3d(ray.eyePoint);
		Vector3d d = new Vector3d(ray.viewDirection);
		ArrayList<Double> intersect_list = new ArrayList<Double>(); 
		ArrayList<Double> entering_list = new ArrayList<Double>(); 
		
		if(ray.viewDirection.x == 0) {
			x_near = Double.NEGATIVE_INFINITY;
			x_far = Double.POSITIVE_INFINITY;
		}
		else {
			x_near = (this.min.x - ray.eyePoint.x) / ray.viewDirection.x;
			x_far = (this.max.x - ray.eyePoint.x) / ray.viewDirection.x;
		}
		if(ray.viewDirection.y == 0) {
			y_near = Double.NEGATIVE_INFINITY;
			y_far = Double.POSITIVE_INFINITY;
		}
		else {
			y_near = (this.min.y - ray.eyePoint.y) / ray.viewDirection.y;
			y_far = (this.max.y - ray.eyePoint.y) / ray.viewDirection.y;
		}
		if(ray.viewDirection.z == 0) {
			z_near = Double.NEGATIVE_INFINITY;
			z_far = Double.POSITIVE_INFINITY;
		}
		else {
			z_near = (this.min.z - ray.eyePoint.z) / ray.viewDirection.z;
			z_far = (this.max.z - ray.eyePoint.z) / ray.viewDirection.z;
		}
		
		intersect_list.add(x_near);
		intersect_list.add(x_far);
		intersect_list.add(y_near);
		intersect_list.add(y_far);
		intersect_list.add(z_near);
		intersect_list.add(z_far);
		
		Collections.sort(intersect_list);
		
		entering_list.add(intersect_list.get(0));
		entering_list.add(intersect_list.get(1));
		entering_list.add(intersect_list.get(2));
		
		if(		(entering_list.contains(x_near) || entering_list.contains(x_far))
			&&  (entering_list.contains(y_near) || entering_list.contains(y_far))
			&&  (entering_list.contains(z_near) || entering_list.contains(z_far))
			&& entering_list.get(2) != Double.POSITIVE_INFINITY 
			&& entering_list.get(2) > 0) {
			
			result.t = entering_list.get(2);
			d.scale(result.t);
			d.add(p);
			result.p = new Point3d(d);
			result.material = this.material;
			
			if(result.t == x_near)
				{result.n = new Vector3d(-1,0,0);}
			else if(result.t == x_far)
				{result.n = new Vector3d(1,0,0);}
			else if(result.t == y_near)
				{result.n = new Vector3d(0,-1,0);}
			else if(result.t == y_far)
				{result.n = new Vector3d(0,1,0);}
			else if(result.t == z_near)
				{result.n = new Vector3d(0,0,-1);}
			else if(result.t == z_far)
				{result.n = new Vector3d(0,0,1);}
		}
		else {
			result.t = Double.POSITIVE_INFINITY;
			result.p = new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
			result.n = new Vector3d(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
			result.material = null;
		}
	}
		
}
