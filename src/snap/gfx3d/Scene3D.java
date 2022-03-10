/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snap.gfx3d;

/**
 * This class is the root ParentShape of all scene shapes. It also manages cameras and lights.
 */
public class Scene3D extends ParentShape3D {
    
    // Camera that renders the scene
    private Camera3D _camera;
    
    // Light that illuminates the scene
    private Light3D  _light = new Light3D();
    
    /**
     * Constructor.
     */
    public Scene3D()
    {
        _camera = new Camera3D();
        _camera.setScene(this);
    }

    /**
     * Returns the camera that renders this scene.
     */
    public Camera3D getCamera()  { return _camera; }

    /**
     * Returns the light that illumiates this scene.
     */
    public Light3D getLight()  { return _light; }

    /**
     * Override to notify camera.
     */
    @Override
    public void setChildren(Shape3D[] theChildren)
    {
        super.setChildren(theChildren);
        _camera.sceneDidChange();
    }

    /**
     * Adds a child.
     */
    public void addChild(Shape3D aShape, int anIndex)
    {
        super.addChild(aShape, anIndex);
        _camera.sceneDidChange();
    }

    /**
     * Removes the child at given index.
     */
    public Shape3D removeChild(int anIndex)
    {
        Shape3D child = super.removeChild(anIndex);
        _camera.sceneDidChange();
        return child;
    }

    /**
     * Returns a path in camera coords for given path in local coords.
     */
    public Path3D localToCamera(Path3D aPath)
    {
        Transform3D localToCamera = _camera.getTransform();
        return aPath.copyForTransform(localToCamera);
    }

    /**
     * Returns the given vector in camera coords.
     */
    public Vector3D localToCameraForVector(double aX, double aY, double aZ)
    {
        Vector3D v2 = new Vector3D(aX, aY, aZ);
        Transform3D localToCamera = _camera.getTransform();
        localToCamera.transformVector(v2);
        return v2;
    }
}