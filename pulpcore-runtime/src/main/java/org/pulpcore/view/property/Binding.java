package org.pulpcore.view.property;

/**
 Internal class used to implement property binding
 */
class Binding implements PropertyListener {

    final Property source;
    final Property dest;

    Binding(Property source, Property dest) {
        this.source = source;
        this.dest = dest;
    }

    @Override
    public void onPropertyChange(Property property) {
        // Gross hack. 
        // Instanceof Hell.
        // This class is internal magic, and I'd rather not have a different
        // class for each combination of source and dest.
        if (source instanceof IntProperty && dest instanceof IntProperty) {
            ((IntProperty)dest).set(((IntProperty)source).get());
        }
        else if (source instanceof IntProperty && dest instanceof FloatProperty) {
            ((FloatProperty)dest).set(((IntProperty)source).get());
        }
        else if (source instanceof FloatProperty && dest instanceof IntProperty) {
            ((IntProperty)dest).set((int)((FloatProperty)source).get());
        }
        else if (source instanceof FloatProperty && dest instanceof FloatProperty) {
            ((FloatProperty)dest).set(((FloatProperty)source).get());
        }
        else if (source instanceof ColorProperty && dest instanceof ColorProperty) {
            ((ColorProperty)dest).set(((ColorProperty)source).get());
        }
    }
}
