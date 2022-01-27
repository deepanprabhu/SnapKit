package snap.gfx3d;
import java.util.Arrays;

/**
 * Used to create new renderers.
 */
public abstract class RendererFactory {

    // Default RendererFactory
    private static RendererFactory  _defaultFactory;

    // All RendererFactories
    private static RendererFactory[]  _factories = new RendererFactory[0];

    /**
     * Returns the renderer name.
     */
    public abstract String getRendererName();

    /**
     * Returns a new default renderer.
     */
    public abstract Renderer newRenderer(Camera3D aCamera);

    /**
     * Returns a new default renderer.
     */
    public static Renderer newDefaultRenderer(Camera3D aCamera)
    {
        if (_defaultFactory != null)
            return _defaultFactory.newRenderer(aCamera);

        return new Renderer2D(aCamera);
    }

    /**
     * Returns the default renderer.
     */
    public static RendererFactory getDefaultFactory()
    {
        return _defaultFactory;
    }

    /**
     * Sets a default renderer.
     */
    public static void setDefaultFactory(RendererFactory aRenderer)
    {
        _defaultFactory = aRenderer;
    }

    /**
     * Returns all factories.
     */
    public static RendererFactory[] getFactories()
    {
        return _factories;
    }

    /**
     * Adds a factory.
     */
    public static void addFactory(RendererFactory aFactory)
    {
        int end = _factories.length;
        _factories = Arrays.copyOf(_factories, end + 1);
        _factories[end] = aFactory;
    }

    /**
     * Static initializer.
     */
    static
    {
        Renderer2D.registerFactory();
    }
}
