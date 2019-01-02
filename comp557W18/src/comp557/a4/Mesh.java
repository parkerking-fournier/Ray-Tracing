package comp557.a4;

import java.util.ArrayList;

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
	
	/** A bounding box to reduce run time  */
	public Box bounding_box;

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


		
		//Inside the bounding box
		if(		ray.eyePoint.x > this.bounding_box.min.x && ray.eyePoint.x < this.bounding_box.max.x && 
				ray.eyePoint.y > this.bounding_box.min.x && ray.eyePoint.y < this.bounding_box.max.y &&	
				ray.eyePoint.z > this.bounding_box.min.x && ray.eyePoint.z < this.bounding_box.max.z ) {
			closest_result.t = 1;			
		}
		//Intersect with the bounding box
		else {
			this.bounding_box.intersect(ray, closest_result);
		}
		
		// ray face intersection
		if(closest_result.t < Double.POSITIVE_INFINITY) {
			closest_result.t = Double.POSITIVE_INFINITY;
			for(int i = 0; i < soup.faceList.size(); i++) {
				int[] face = soup.faceList.get(i);
				IntersectResult result = new IntersectResult();
				
				if(face.length == 3) {
					intersectTriangle(ray, result, face);
				}
				else if(face.length == 4){
					int[] face_1 = new int[] {soup.faceList.get(i)[0], soup.faceList.get(i)[1], soup.faceList.get(i)[2]};
					int[] face_2 = new int[] {soup.faceList.get(i)[2], soup.faceList.get(i)[3], soup.faceList.get(i)[0]};
					
					IntersectResult result_1 = new IntersectResult();
					IntersectResult result_2 = new IntersectResult();
					
					intersectTriangle(ray, result_1, face_1);
					intersectTriangle(ray, result_2, face_2);
					
					result = result_1;
					if(result_1.t == Double.POSITIVE_INFINITY){
						result = result_2;
					}
				}
				
				if(result.t < closest_result.t) {
					closest_result.t = result.t;
					closest_result.p = new Point3d(result.p);
					closest_result.n = new Vector3d(result.n);
					closest_result.material = result.material;
				}
			}
		}
	}
	
	public void intersectTriangle(Ray ray, IntersectResult result, int[] face) {
				
		// Points of the triangle
		Point3d v0 = soup.vertexList.get(face[0]).p;
		Point3d v1 = soup.vertexList.get(face[1]).p;
		Point3d v2 = soup.vertexList.get(face[2]).p;
		
		// v0 to v1
		Vector3d e1 = new Vector3d(v1);
			e1.sub(v0);
		// v0 to v2
		Vector3d e2 = new Vector3d(v2);
			e2.sub(v0);
		// Normal
		Vector3d normal = new Vector3d();
			normal.cross(e1, e2);
			normal.normalize();
			
		double e = 0.0000001;
		double t = Double.POSITIVE_INFINITY;
        Vector3d h = new Vector3d();
        Vector3d s = new Vector3d();
        Vector3d q = new Vector3d();
        double a, f, u, v;
        		a = f = u = v = 0;
        	boolean flag = true;
        
        	h.cross(ray.viewDirection, e2);
        a = e1.dot(h);
        if (a > -e && a < e) {
            flag = false;    // This ray is parallel to this triangle.
        }
        f = 1.0 / a;
        s.sub(ray.eyePoint, v0);
        u = f * (s.dot(h));
        if (u < 0.0 || u > 1.0) {
        		flag = false;
        }
        q.cross(s, e1);
        v = f * ray.viewDirection.dot(q);
        if (v < 0.0 || u + v > 1.0) {
            flag = false;
        }
        // At this stage we can compute t to find out where the intersection point is on the line.
        t = f * e2.dot(q);
        if (t > e && flag){
        		result.t = t;
        		result.n = new Vector3d(normal);
        		result.p = new Point3d(ray.viewDirection);
          		result.p.scale(t);
        			result.p.add(ray.eyePoint);
        		result.material = this.material;
          } 
        else{
        		result.t = Double.POSITIVE_INFINITY;
        }        
	}
}

