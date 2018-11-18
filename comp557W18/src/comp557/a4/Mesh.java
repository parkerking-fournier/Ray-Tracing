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
	public void intersect(Ray ray, IntersectResult closest_result) {
		
		closest_result.t = Double.POSITIVE_INFINITY;
		
		// TODO: Objective 7: ray triangle intersection for meshes
		for(int i = 0; i < soup.faceList.size(); i++) {
						
			int[] face = soup.faceList.get(i);
			IntersectResult result = new IntersectResult();
			intersectTriangle(ray, result, face);
			
			if(result.t < closest_result.t) {
				closest_result.t = result.t;
				closest_result.p = new Point3d(result.p);
				closest_result.n = new Vector3d(result.n);
				closest_result.material = result.material;
			}
		}
	}
	
	public void intersectTriangle(Ray ray, IntersectResult result, int[] face) {
		
		// Points of the triangle
		Point3d v0 = soup.vertexList.get(face[0]).p;
		Point3d v1 = soup.vertexList.get(face[1]).p;
		Point3d v2 = soup.vertexList.get(face[2]).p;
		
		// v0 to v1
		Vector3d e0 = new Vector3d(v1);
			e0.sub(v0);
		// v0 to v2
		Vector3d e1 = new Vector3d(v2);
			e1.sub(v0);
		// Normal
		Vector3d normal = new Vector3d();
			normal.cross(e0, e1);
			normal.normalize();
		
		// Check if the ray and triangle are parallel
		boolean flag = true;
		double e = 0.0001;
		double n_dot_ray = normal.dot(ray.viewDirection);
		if( Math.abs(n_dot_ray) < e) {
			flag = false;
		}
		
		// Check if the triangle is behind the ray
		Vector3d v_0 = new Vector3d(v0);
		double d = normal.dot(v_0);
		Vector3d orig = new Vector3d(ray.eyePoint);
		double t = (normal.dot(orig) + d)/n_dot_ray;
		if(t < 0) {
			flag = false;
		}
		
		// inside out test
		
		Point3d point = new Point3d();
		point.scale(t);
		point.add(orig);
		
		Vector3d C = new Vector3d();
		
		// edge 0
		Vector3d edge_0 = new Vector3d(v1);
		edge_0.sub(v0);
		Vector3d vp_0 = new Vector3d(point);
		vp_0.sub(v0);
		C.cross(edge_0, vp_0);
		if(normal.dot(C) < 0) {
			flag = false;
		}
		
		// edge 1
		Vector3d edge_1 = new Vector3d(v2);
		edge_1.sub(v1);
		Vector3d vp_1 = new Vector3d(point);
		vp_1.sub(v1);
		C.cross(edge_1, vp_1);
		if(normal.dot(C) < 0) {
			flag = false;
		}
		
		// edge 1
		Vector3d edge_2 = new Vector3d(v0);
		edge_1.sub(v2);
		Vector3d vp_2 = new Vector3d(point);
		vp_2.sub(v2);
		C.cross(edge_2, vp_2);
		if(normal.dot(C) < 0) {
			flag = false;
		}
		
		if(flag = true) {
			result.t = t;
			result.p = new Point3d(point);
			result.n = new Vector3d(normal);
			result.material = this.material;
		}
		
	}
}

