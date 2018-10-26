package comp557.a4;

/**
 *	COMP 557 - Winter 2018
 *	Assignment 4
 *	Parker King-Fournier
 *	260556983
 */

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Mesh extends Intersectable {
	
	/** Static map storing all meshes by name */
	public static Map<String,Mesh> meshMap = new HashMap<String,Mesh>();
	
	/**  Name for this mesh, to allow re-use of a polygon soup across Mesh objects */
	public String name = "";
	
	/**
	 * The polygon soup.
	 */
	public PolygonSoup soup;

	public Mesh() {
		super();
		this.soup = null;
	}			
	
	@Override
    public String getType() {
    		return "mesh";
    }
	
	@Override
	public void intersect(Ray ray, IntersectResult result) {
		
		Double min_t = Double.POSITIVE_INFINITY;
		Double t = min_t;
		Vector3d p = new Vector3d(ray.eyePoint);
		Vector3d d = new Vector3d(ray.viewDirection);
		Vector3d point = new Vector3d(d);
		
		// TODO: Objective 7: ray triangle intersection for meshes
		for(int i = 0; i < soup.faceList.size(); i++) {
			
			Boolean intersects = true;
			
			Vector3d v_1, v_2, v_3,
					 e_1, e_2, 
					 h, s, q;
			
			final double EPSILON = 0.0000001; 
			
		    double 	 a, f, u, v;
			
			int[] face = soup.faceList.get(i);

			// Get points on a triangle
			v_1 = new Vector3d(	soup.vertexList.get(face[0]).p );							
			v_2 = new Vector3d(	soup.vertexList.get(face[1]).p );
			v_3 = new Vector3d(	soup.vertexList.get(face[2]).p );
			
			e_1 = new Vector3d(v_2);
				e_1.sub(v_1);
			e_2 = new Vector3d(v_3);
				e_2.sub(v_1);
				
			h = new Vector3d();
			h.cross(e_2, ray.viewDirection);
			a = e_1.dot(h);
			
			if (a > -EPSILON && a < EPSILON)
				{intersects = false;} 
			 
			 f = 1/a;
			 s = new Vector3d(ray.eyePoint);
			 	s.sub(v_1);
			 u = f * (s.dot(h));
			 
			 if(u < 0.0 || u > 1.0)
			 	{intersects = false;}
			 
			 q = new Vector3d();
			 q.cross(e_1,s);
			 v = f * ray.viewDirection.dot(q);
			 if (v < 0.0 || u + v > 1.0)
			 	{intersects = false;}
			 
			 if(intersects) {
				 t = f * e_2.dot(q);
				 if (t <= EPSILON || t >= min_t)
				 	{intersects = false;}
			 }
			 
			 if(intersects && t < min_t && t > 0) {
				 min_t = t;
				 point.scale(t);
				 point.add(p);
				 
				 result.t = min_t;
				 result.p = new Point3d(point);
				 result.material = this.material;
				 result.n.cross(e_1, e_2);
				 result.n.normalize();
			 }
		}
		
	}

}
