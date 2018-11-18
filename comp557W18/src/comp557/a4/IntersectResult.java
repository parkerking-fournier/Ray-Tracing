package comp557.a4;

/**
 *	COMP 557 - Winter 2018
 *	Assignment 4
 *	Parker King-Fournier
 *	260556983
 */

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Use this class to store the result of an intersection, or modify it to suit your needs!
 */
public class IntersectResult {
	
	/** The normal at the intersection */ 
	public Vector3d n = new Vector3d();
	
	/** Intersection position */
	public Point3d p = new Point3d();
	
	/** The material of the intersection */
	public Material material = null;
		
	/** Parameter on the ray giving the position of the intersection */
	public double t = Double.POSITIVE_INFINITY; 
	
	/**
	 * Default constructor.
	 */
	IntersectResult() {
		// do nothing
	}
	
	/**
	 * Copy constructor.
	 */
	IntersectResult( IntersectResult other ) {
		n.set( other.n );
		p.set( other.p );
		t = other.t;
		material = other.material;
	}
	
}
