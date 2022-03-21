# SnapKit

SnapKit is a new Java UI kit for creating rich Write-Once-Run-Anywhere UI applications that run
pixel-perfect on the desktop and in the browser.

Why do we need another UI kit? Because Swing is out of date, and JavaFX missed the boat.
And neither run natively in the browser.

Check out [demos of SnapKit running in the browser](http://www.reportmill.com/snaptea/).

## Much to love about Swing

    - Solid view hierarchy and set of controls/components
    
    - Relatively easy to create and update UI and respond to user input and UI changes
    
    - Full set of geometric shape primitives: Line, Rect, Ellipse, Path, Polygon, etc.
	
    - Easily set border, background, font on any component with simple API
	
    - The whole convenient painting model - just override paint() to customize
	
    - It handles property changes in conventional Java property change manner
	
    - It binds easily with POJOs
	

## Much to love about JavaFX

    - Easily mix graphics and app controls

    - Easily add gradients, textures, effects
	
    - Set arbitrary transforms (rotate, scale, skew) on any node
	
    - It has built-in binding support to easily wire values across objects
	
    - It has a full set of nodes for easy layout: Box, BorderView, StackPane, etc.
	
    - It has support for easily defining UI in a separate text file (FXML)
	

## What's to love about SnapKit?

    - It provides all these features
	
    - It runs on top of Swing, JavaFX and HTML DOM
	
    - It is portable to any future UI kit
	
    - The base class is called View. Now that puts the V in MVC!
	
    - The ViewOwner class provides control functionally (whoops, there goes the C)
	
    - The ViewEvent class unifies all input events for more consistent handling
	
## The Graphics Package

One of the great aspects of Swing is the separation and synergy between the core graphics layer (Java2D) and
the UI layer. SnapKit provides this same separation with the snap.gfx package that contains:

    - Full set of geometric primitives: Rect, Point, Size, Insets, Pos (for alignment)
	
    - Transform for arbitrary transforms and coordinate conversions: rotate, scale, skew
	
    - Full set of Shape primitives: Rect, RoundRect, Arc, Ellipse, Line, Path, Polygon
	
    - Paint define fill styles with common subclasses: Color, GradientPaint, ImagePaint
	
    - Stroke for defining outline style, and Border for a stroke in a specific Paint
	
    - Effects for rich rendering: Shadow, Reflect, Emboss, Blur
	
    - Font and FontFile objects (wrap around platform fonts)
	
    - Painter capable of rendering shapes, images and text with transform, fill, stroke, effect
	
    - Image object (wraps around platform image)
	
    - RichText object for managing large text content with attributes
	
    - TextStyle object to manage a set of attributes: font, color, underline, hyper links, format, etc.
	
    - TextBox object for managing RichText in a geometric region (with spelling and hyphenation)
	
    - SoundClip for playing sounds

## The View Package

And the essentail part of a good UI kit is the set of classes that model the scene graph and
standard UI controls.

    - View for managing hierarchy of coordinate systems, drawing and input events
	
    - Full set of classes for graphics primitives: RectView, ShapeView, ImageView, StringView
	
    - Label: Convenient View+StringView+View layout to easily label UI
	
    - ButtonBase: Embeds Label for simple, flexible and customizable buttons
	
    - Button subclasses: Button, CheckBox, ToggleButton RadioButton, MenuButton, MenuItem
	
    - TextField for editing simple text values (with flexible background label for prompts, icons, etc.)
	
    - TextView: Comprehensive rich text editing with style setting, spellcheck, etc.
	
    - ComboBox, Slider, Spinner, ThumbWheel for modifying values with more advanced UI
	
    - ListView, TableView, TreeView, BrowserView for displaying large sets of objects
	
    - ParentView for Views that manage children (and ChildView for views that allow others to add them)
	
    - Box, VBox, HBox, BorderView, StackView, SpringView to facilitate layout
	
    - ScrollView, SplitView, TabView, TitleView
	
    - DocView, PageView: represent a real world document and page
	
    - ViewOwner: integrated controller class to manage UI creation, initialization, updates, bindings and events
	
    - RootPane: Manages view event dispatch and hierarchy updates, layout and painting
	
    - WindowView: Maps to a platform window
	
    - MenuItem, Menu, MenuBar
	
    - ProgressBar, Separator

    - ViewArchiver for reading/writing views from simple XML files
	
    - ViewEvent for encapsulating all input events in unified object

    - DialogBox, FormBuilder: For quickly generating UI for common user input
